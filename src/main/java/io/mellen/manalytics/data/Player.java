package io.mellen.manalytics.data;

import io.mellen.manalytics.data.analytics.AnalyticsEngine;
import io.mellen.manalytics.data.connection.MysqlConnection;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

public class Player extends EAVObject {
    private UUID uuid;
    private String name;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private int sessionId;

    private OfflinePlayer referencePlayer;

    public Player() {
        super("player");
    }

    public Player(OfflinePlayer reference) {
        super("player");
        this.referencePlayer = reference;
        loadByField(AnalyticsEngine.getInstance().getConnection(), "uuid", reference.getUniqueId().toString());
    }

    @Override
    protected void setEntityData(Map<String, Object> data) {
        uuid = UUID.fromString((String)data.getOrDefault("uuid", referencePlayer.getUniqueId().toString()));
        name = (String)data.getOrDefault("name", referencePlayer.getName());
        createdAt = (Timestamp)data.getOrDefault("created_at", Timestamp.from(Instant.now()));

    }


    @Override
    protected void update(MysqlConnection connection) throws SQLException {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("update player set uuid = ?, name = ?, updated_at = CURRENT_TIMESTAMP where entity_id = ?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setInt(3, getId());
            stmt.execute();
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR an SQL error occurred when trying to update the player object");
            ex.printStackTrace();
        }
    }

    @Override
    protected void create(MysqlConnection connection) throws SQLException {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("insert into player (uuid, name) values (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, referencePlayer.getUniqueId().toString());
            stmt.setString(2, referencePlayer.getName());
            stmt.executeUpdate();

            ResultSet results = stmt.getGeneratedKeys();
            if (results.next()) {
                setId(results.getInt(1));
            }
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR An SQL error occurred when trying to create a player DB object");
            ex.printStackTrace();
        }
    }

    public void startSession(MysqlConnection connection) {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("insert into player_session_history (entity_id) values (?)", PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, getId());
            stmt.executeUpdate();
            ResultSet results = stmt.getGeneratedKeys();
            if (results.next()) {
                sessionId = results.getInt(1);
            }
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR An SQL error occurred when trying to start a player session");
            ex.printStackTrace();
        }
    }

    public void endSession(MysqlConnection connection) {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("update player_session_history set finished_at = CURRENT_TIMESTAMP where session_id = ?");
            stmt.setInt(1, sessionId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR An SQL error occurred when trying to end a player session");
            ex.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }
}
