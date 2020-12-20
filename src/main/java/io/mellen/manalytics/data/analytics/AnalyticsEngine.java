package io.mellen.manalytics.data.analytics;

import io.mellen.manalytics.AnalyticsPlugin;
import io.mellen.manalytics.data.Player;
import io.mellen.manalytics.data.PlayerEvent;
import io.mellen.manalytics.data.connection.MysqlConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AnalyticsEngine {

    protected MysqlConnection connection;

    protected AnalyticsPlugin plugin;

    private Map<UUID, Player> playerMap = new HashMap<>();
    private Map<Integer, UUID> internalUUIDMap = new HashMap<>();

    private static AnalyticsEngine INSTANCE;

    public AnalyticsEngine(AnalyticsPlugin plugin, MysqlConnection connection) {
        this.plugin = plugin;
        this.connection = connection;

        INSTANCE = this;
    }

    public Player getPlayer(OfflinePlayer referencePlayer) {
        return getPlayerByUUID(referencePlayer.getUniqueId());
    }

    public Player getPlayerByUUID(UUID uuid) {
        if (playerMap.containsKey(uuid)) {
            return playerMap.get(uuid);
        }
        else {
            OfflinePlayer plr = Bukkit.getOfflinePlayer(uuid);
            Player playerObject = new Player(plr);
            playerMap.put(uuid, playerObject);
            internalUUIDMap.put(playerObject.getId(), uuid);
            return playerObject;
        }
    }

    public Player getPlayerByInternalId(int id) {
        return getPlayerByUUID(getUUIDByInternalId(id));
    }

    public void pushEvent(Player player, String eventName, String customOne, String customTwo, String customThree, String customFour) {
        PlayerEvent event = new PlayerEvent(player, eventName, customOne, customTwo, customThree, customFour);
        event.forceCreate(connection);
    }

    public UUID getUUIDByInternalId(int id) {
        if (internalUUIDMap.containsKey(id)) {
            return internalUUIDMap.get(id);
        }
        else {
            try (Connection conn = connection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("select uuid from player where entity_id = ?");
                stmt.setInt(1, id);
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    internalUUIDMap.put(id, UUID.fromString(results.getString("uuid")));
                    return internalUUIDMap.get(id);
                }
            }
            catch (SQLException ex) {
                connection.debug("CONNERROR an SQL error occurred when trying to get the UUID for id " + id);
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static AnalyticsEngine getInstance() {
        return INSTANCE;
    }

    public MysqlConnection getConnection() {
        return connection;
    }

    public List<PlayerEvent> getEventsForPlayer(Player player, int page, int limit) {
        int start = (page - 1)* limit;
        int end = (page) * limit;

        List<PlayerEvent> events = new ArrayList<>();

        String query = "select * from player_event where entity_id = ? order by created_at desc limit " + start + ", " + end;
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, player.getId());
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                events.add(new PlayerEvent(
                        player,
                        results.getString("event_name"),
                        results.getString("custom_one"),
                        results.getString("custom_two"),
                        results.getString("custom_three"),
                        results.getString("custom_four"),
                        results.getTimestamp("created_at")
                ));
            }
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR an SQL error occurred when trying to view events for a player");
            ex.printStackTrace();
        }

        return events;
    }
}
