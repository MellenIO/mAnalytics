package io.mellen.manalytics.data;

import io.mellen.manalytics.data.analytics.AnalyticsEngine;
import io.mellen.manalytics.data.connection.MysqlConnection;
import io.mellen.manalytics.data.events.EventRenderer;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PlayerEvent extends EAVObject {
    private transient Player player;
    private String eventName;
    private Timestamp createdAt;

    protected String customOne;
    protected String customTwo;
    protected String customThree;
    protected String customFour;

    private String html;

    public PlayerEvent() {
        super("player_event");
    }

    public PlayerEvent(Player player, String eventName, String customOne, String customTwo, String customThree, String customFour) {
        super("player_event");
        this.player = player;
        this.eventName = eventName;
        this.customOne = customOne;
        this.customTwo = customTwo;
        this.customThree = customThree;
        this.customFour = customFour;
        this.createdAt = Timestamp.from(Instant.now());

        //Set an empty EAV object to allow EAV to be saved
        setEavData(new HashMap<>());
    }

    public PlayerEvent(Player player, String eventName, String customOne, String customTwo, String customThree, String customFour, Timestamp createdAt) {
        super("player_event");
        this.player = player;
        this.eventName = eventName;
        this.customOne = customOne;
        this.customTwo = customTwo;
        this.customThree = customThree;
        this.customFour = customFour;
        this.createdAt = createdAt;
        this.html = EventRenderer.forEvent(eventName).renderHTML(this);
    }

    @Override
    protected void setEntityData(Map<String, Object> data) {
        player = AnalyticsEngine.getInstance().getPlayerByInternalId((int)data.get("entity_id"));
        eventName = (String)data.getOrDefault("event_name", "");
        customOne = (String)data.getOrDefault("custom_one", "");
        customTwo = (String)data.getOrDefault("custom_two", "");
        customThree = (String)data.getOrDefault("custom_three", "");
        customFour = (String)data.getOrDefault("custom_four", "");
        createdAt = (Timestamp) data.getOrDefault("created_at", "");
    }

    @Override
    protected void update(MysqlConnection connection) throws SQLException {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("update player_event set event_name = ?, entity_id = ?, custom_one = ?, custom_two = ?, custom_three = ?, custom_four = ? where event_id = ?");
            stmt.setString(1, eventName);
            stmt.setInt(2, player.getId());
            stmt.setString(3, customOne);
            stmt.setString(4, customTwo);
            stmt.setString(5, customThree);
            stmt.setString(6, customFour);
            stmt.setInt(7, getId());
            stmt.execute();
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR an SQL error occurred when trying to update the player object");
            ex.printStackTrace();
        }
    }

    @Override
    protected void create(MysqlConnection connection) throws Exception {
        try (Connection conn = connection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("insert into player_event (event_name, entity_id, custom_one, custom_two, custom_three, custom_four) values (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, eventName);
            stmt.setInt(2, player.getId());
            stmt.setString(3, customOne);
            stmt.setString(4, customTwo);
            stmt.setString(5, customThree);
            stmt.setString(6, customFour);
            stmt.executeUpdate();

            ResultSet results = stmt.getGeneratedKeys();
            if (results.next()) {
                setId(results.getInt(1));
            }
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR an SQL error occurred when trying to create the player event object");
            ex.printStackTrace();
        }
        //throw new Exception("This method is not implemented and should not be used.");
    }

    public void forceCreate(MysqlConnection connection) {
        try {
            create(connection);
            saveEAV(connection);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getPrimaryKeyName() {
        return "event_id";
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCustomOne() {
        return (null == customOne) ? "null" : customOne;
    }

    public String getCustomTwo() {
        return (null == customTwo) ? "null" : customTwo;
    }

    public String getCustomThree() {
        return (null == customThree) ? "null" : customThree;
    }

    public String getCustomFour() {
        return (null == customFour) ? "null" : customFour;
    }

    public String getEventName() {
        return eventName;
    }
}
