package io.mellen.manalytics.data.connection;

import com.zaxxer.hikari.HikariDataSource;
import io.mellen.manalytics.AnalyticsPlugin;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MysqlConnection {

    private static final String ENTITY_TYPE_QUERY = "select entity_type_id, entity_type_name from entity";
    private static final String ENTITY_ATTRIBUTE_QUERY = "select attribute_id, attribute_name, attribute_type from entity_attribute where entity_type_id = ?";

    private HikariDataSource source;

    private AnalyticsPlugin plugin;

    private Map<String, Integer> entityTypeMap = new HashMap<String, Integer>();
    private Map<Integer, Map<String, Attribute>> entityAttributeMap = new HashMap<>();

    public MysqlConnection(AnalyticsPlugin plugin, String host, String port, String username, String password, String database) {
        this.plugin = plugin;

        source = new HikariDataSource();
        source.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        source.addDataSourceProperty("serverName", host);
        source.addDataSourceProperty("port", port);
        source.addDataSourceProperty("databaseName", database);
        source.addDataSourceProperty("user", username);
        source.addDataSourceProperty("password", password);
    }

    public void close() {
        if (source != null) {
            source.close();
        }
    }

    public void loadEntityTypes() {
        entityTypeMap = new HashMap<String, Integer>();
        try (Connection connection = source.getConnection()) {
            ResultSet rs = stmt(connection, ENTITY_TYPE_QUERY).executeQuery();
            while (rs.next()) {
                entityTypeMap.put(rs.getString("entity_type_name"), rs.getInt("entity_type_id"));
                debug("Added entity type " + rs.getString("entity_type_name"));
            }
            rs.close();
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            //TODO catch
        }
    }

    public void async(Function<Connection, PreparedStatement> setup, Consumer<ResultSet> after) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = source.getConnection()) {
                PreparedStatement stmt = setup.apply(connection);
                if (stmt != null) {
                    ResultSet rs = stmt.executeQuery();
                    after.accept(rs);
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    public void asyncUpdate(Function<Connection, PreparedStatement> setup, Consumer<ResultSet> after) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = source.getConnection()) {
                PreparedStatement stmt = setup.apply(connection);
                if (stmt != null) {
                    stmt.executeUpdate();
                    after.accept(stmt.getGeneratedKeys());
                }
            }catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private PreparedStatement stmt(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public Map<String, Attribute> getEavAttributesForType(String typeId) {
        int entityTypeId = getEntityTypeId(typeId);
        if (entityAttributeMap.containsKey(entityTypeId)) {
            return entityAttributeMap.get(entityTypeId);
        }
        Map<String, Attribute> attributes = loadEAVAttributes(entityTypeId);
        entityAttributeMap.put(entityTypeId, attributes);
        return attributes;
    }

    //Synchronous method!!!
    private Map<String, Attribute> loadEAVAttributes(int id) {
        try (Connection connection = source.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(ENTITY_ATTRIBUTE_QUERY);
            stmt.setInt(1, id);
            ResultSet results = stmt.executeQuery();

            Map<String, Attribute> attributeMap = new HashMap<>();
            while (results.next()) {
                Attribute attr = new Attribute();
                attr.id = results.getInt("attribute_id");
                attr.type = results.getString("attribute_type");
                attr.name = results.getString("attribute_name");

                attributeMap.put(attr.name, attr);
            }

            return attributeMap;
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public int getEntityTypeId(String typeId) {
        return entityTypeMap.get(typeId);
    }

    public void debug(String message) {
        //TODO: feature flag
        if (true) {
            Bukkit.getLogger().info(message);
        }

    }
}
