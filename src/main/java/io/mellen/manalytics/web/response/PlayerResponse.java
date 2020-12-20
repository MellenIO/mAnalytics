package io.mellen.manalytics.web.response;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.data.Player;
import io.mellen.manalytics.data.PlayerEvent;
import spark.Request;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerResponse {

    private static final String PLAYTIME_QUERY = "select abs(sum(time_to_sec(started_at) - time_to_sec(coalesce(finished_at, current_timestamp)))) as playtime from player_session_history where entity_id = ?";

    private static final String SESSION_QUERY = "select started_at, finished_at from player_session_history where entity_id = ? order by started_at desc limit 0, 20";

    private static final String NAME_CHANGE_QUERY = "select old_name, new_name, created_at from player_name_history where entity_id = ? order by created_at asc";

    private String name;
    private UUID uuid;
    private long playtimeInSeconds;
    private ArrayList<NameChange> nameChanges;
    private ArrayList<PlayerEvent> lastFewEvents;
    private ArrayList<Session> lastFewSessions;

    public PlayerResponse() {
        nameChanges = new ArrayList<>();
        lastFewSessions = new ArrayList<>();
        lastFewEvents = new ArrayList<>();
    }

    public PlayerResponse(Request request, AnalyticsPlugin plugin) {
        String idType = request.params(":idType");
        String id = request.params(":id");
        Player targetPlayer = ("uuid".equalsIgnoreCase(idType))
                ? plugin.getAnalyticsEngine().getPlayerByUUID(UUID.fromString(id))
                : plugin.getAnalyticsEngine().getPlayerByInternalId(Integer.valueOf(id));
        getData(targetPlayer, plugin);
    }

    private void getData(Player targetPlayer, AnalyticsPlugin plugin) {
        name = targetPlayer.getName();
        uuid = targetPlayer.getUuid();
        lastFewEvents = (ArrayList<PlayerEvent>)plugin.getAnalyticsEngine().getEventsForPlayer(targetPlayer, 1, 20);
        lastFewSessions = new ArrayList<>();
        nameChanges = new ArrayList<>();

        try (Connection connection = plugin.getAnalyticsEngine().getConnection().getConnection()) {
            //Get total playtime
            PreparedStatement stmt = connection.prepareStatement(PLAYTIME_QUERY);
            stmt.setInt(1, targetPlayer.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                playtimeInSeconds = rs.getLong("playtime");
            }
            rs.close();
            stmt.close();

            //Get sessions
            PreparedStatement sessionStatement = connection.prepareStatement(SESSION_QUERY);
            sessionStatement.setInt(1, targetPlayer.getId());
            ResultSet sessionResults = sessionStatement.executeQuery();
            while (sessionResults.next()) {
                lastFewSessions.add(new Session(sessionResults.getTimestamp("started_at"), sessionResults.getTimestamp("finished_at")));
            }
            sessionResults.close();
            sessionStatement.close();


            //Get name changes
            PreparedStatement nameStatement = connection.prepareStatement(NAME_CHANGE_QUERY);
            nameStatement.setInt(1, targetPlayer.getId());
            ResultSet nameResults = nameStatement.executeQuery();
            while (nameResults.next()) {
                nameChanges.add(new NameChange(nameResults.getString("old_name"), nameResults.getString("new_name"), nameResults.getTimestamp("created_at")));
            }
            nameResults.close();
            nameStatement.close();
        }
        catch (SQLException ex) {
            plugin.debug("CONNERROR an SQL error occurred when trying to generate a JSON object for a player!");
            ex.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getPlaytimeInSeconds() {
        return playtimeInSeconds;
    }

    public class NameChange {
        private String oldName;
        private String newName;
        private Timestamp createdAt;

        public NameChange() { }

        public NameChange(String oldName, String newName, Timestamp createdAt) {
            this.oldName = oldName;
            this.newName = newName;
            this.createdAt = createdAt;
        }

        public String getOldName() {
            return oldName;
        }

        public String getNewName() {
            return newName;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }
    }

    public class Session {
        private Timestamp startedAt;
        private Timestamp finishedAt;

        public Session() { }

        public Session(Timestamp startedAt, Timestamp finishedAt) {
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
        }

        public Timestamp getStartedAt() {
            return startedAt;
        }

        public Timestamp getFinishedAt() {
            return finishedAt;
        }
    }
}
