package io.mellen.manalytics.data;

import io.mellen.manalytics.data.connection.Attribute;
import io.mellen.manalytics.data.connection.MysqlConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class EAVObject {

    private static final String LOAD_EAV_QUERY = "select\n" +
            "       ea.attribute_name as attribute_name,\n" +
            "       coalesce(eav.`value`, eai.`value`, eadt.`value`, eadc.`value`) as attribute_value\n" +
            "    from entity_attribute ea\n" +
            "    left join entity_attribute_varchar eav\n" +
            "        on eav.attribute_id = ea.attribute_id\n" +
            "            and eav.entity_type_id = ea.entity_type_id\n" +
            "            and eav.entity_id = ?\n" +
            "    left join entity_attribute_int eai\n" +
            "        on eai.attribute_id = ea.attribute_id\n" +
            "            and eai.entity_type_id = ea.entity_type_id\n"+
            "            and eai.entity_id = ?\n" +
            "    left join entity_attribute_datetime eadt\n" +
            "        on eadt.attribute_id = ea.attribute_id\n" +
            "            and eadt.entity_type_id = ea.entity_type_id\n" +
            "            and eadt.entity_id = ?\n" +
            "    left join entity_attribute_decimal eadc\n" +
            "        on eadc.attribute_id = ea.attribute_id\n" +
            "            and eadc.entity_type_id = ea.entity_type_id\n" +
            "            and eadc.entity_id = ?\n" +
            "    where ea.entity_type_id = ?";

    protected transient String pkName = "entity_id";

    private transient String entityName;
    private int id;
    protected Map<String, Object> eavData = new HashMap<>();
    private transient Map<String, Object> originalEavData = new HashMap<>();

    private transient boolean isEAVLoaded = false;

    public EAVObject(String entityName) {
        this.entityName = entityName;
    }

    public EAVObject(String entityName, int id) {
        this.entityName = entityName;
        this.id = id;
    }

    public void load(MysqlConnection connection, int id) {
        loadByField(connection, getPrimaryKeyName(), id);
    }

    public String getPrimaryKeyName() {
        return "entity_id";
    }

    protected void loadByField(MysqlConnection connection, String column, Object value) {
        try (Connection sqlConnection = connection.getConnection()) {
            PreparedStatement mainDataQuery = sqlConnection.prepareStatement(
                    "select * from `%table%` where `%column%` = ?"
                            .replace("%table%", entityName)
                            .replace("%column%", column));

            mainDataQuery.setObject(1, value);
            ResultSet mainData = mainDataQuery.executeQuery();

            //Set main entity data
            ResultSetMetaData md = mainData.getMetaData();
            int columns = md.getColumnCount();
            if (mainData.next()) {
                HashMap<String, Object> row = new HashMap<>(columns);
                for (int i = 1; i <= columns; i++) {
                    row.put(md.getColumnName(i), mainData.getObject(i));
                }
                id = (int)row.get(getPrimaryKeyName());
                connection.debug("Loaded entity type " + entityName + " with id " + id);
                setEntityData(row);
            }
            else {
                connection.debug("Creating entity type " + entityName);
                try {
                    create(connection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Load EAV data
            PreparedStatement stmt = sqlConnection.prepareStatement(LOAD_EAV_QUERY);
            stmt.setInt(1, id);
            stmt.setInt(2, id);
            stmt.setInt(3, id);
            stmt.setInt(4, id);
            stmt.setInt(5, connection.getEntityTypeId(entityName));

            ResultSet results = stmt.executeQuery();
            HashMap<String, Object> eavData = new HashMap<>();
            while (true) {
                try {
                    if (!results.next()) break;
                    eavData.put(results.getString("attribute_name"), results.getObject("attribute_value"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            setEavData(eavData);
        }
        catch (SQLException ex) {
            connection.debug("CONNERROR An SQL Exception occurred trying to load a " + entityName + " entity with " + column + " = " + value.toString() + "!");
            ex.printStackTrace();
        }
    }

    protected void setEavData(HashMap<String, Object> data) {
        this.eavData = data;
        this.originalEavData = (HashMap<String, Object>)data.clone();

        isEAVLoaded = true;
    }

    protected abstract void setEntityData(Map<String, Object> data);

    public int getId() {
        return id;
    }

    public void setData(String key, Object value) {
        this.eavData.put(key, value);
    }

    public Object getData(String key) {
        return getData(key, null);
    }

    public Object getData(String key, Object def) {
        return this.eavData.getOrDefault(key, def);
    }

    protected abstract void update(MysqlConnection connection) throws SQLException;
    protected abstract void create(MysqlConnection connection) throws Exception;

    public void save(MysqlConnection connection) {
        //Save entity data first
        try {
            update(connection);
        }
        catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (isEAVLoaded) {
            //Save EAV data
            Map<Integer, AttributeData<Object>> varcharData = new HashMap<>();
            Map<Integer, AttributeData<Object>> intData = new HashMap<>();
            Map<Integer, AttributeData<Object>> datetimeData = new HashMap<>();
            Map<Integer, AttributeData<Object>> decimalData = new HashMap<>();

            //Load all attributes, and sort in to maps for query generation
            Map<String, Attribute> attributes = connection.getEavAttributesForType(entityName);
            for (Map.Entry<String, Attribute> attribute : attributes.entrySet()) {
                String attributeName = attribute.getKey();
                boolean isNewlySet = !originalEavData.containsKey(attributeName);

                connection.debug("Checking attribute " + attributeName);
                //Calculate whether or not this EAV attribute should be updated
                boolean shouldAppend = isNewlySet;
                if (!shouldAppend) {
                    //Old and new data contain key; let's compare - if they are not equal, we want to update
                    if (eavData.containsKey(attributeName) && null != eavData.get(attributeName)) {
                        connection.debug("Should we update " + attributeName + " to " + eavData.get(attributeName) + "?");
                        if (null == originalEavData.get(attributeName)) {
                            connection.debug("Original EAV data not found for " + attributeName);
                            shouldAppend = true;
                        }
                        else {
                            connection.debug("Checking if " + eavData.get(attributeName) + " equals " + originalEavData.get(attributeName));
                            shouldAppend = !eavData.get(attributeName).equals(originalEavData.get(attributeName));
                        }
                    }
                }


                if (shouldAppend) {
                    switch (attribute.getValue().type) {
                        case "varchar":
                            varcharData.put(attribute.getValue().id, new AttributeData<>((String)eavData.get(attributeName), attributeName, attribute.getValue().id, "varchar"));
                            break;

                        case "int":
                            intData.put(attribute.getValue().id, new AttributeData<>((int)eavData.get(attributeName), attributeName, attribute.getValue().id, "int"));
                            break;

                        case "datetime":
                            datetimeData.put(attribute.getValue().id, new AttributeData<>((Timestamp)eavData.get(attributeName), attributeName, attribute.getValue().id, "datetime"));
                            break;

                        case "decimal":
                            decimalData.put(attribute.getValue().id, new AttributeData<>((BigDecimal)eavData.get(attributeName), attributeName, attribute.getValue().id, "decimal"));
                            break;
                    }
                }
            }

            try (Connection conn = connection.getConnection()) {
                BiFunction<Map<Integer, AttributeData<Object>>, String, PreparedStatement> generateEavInsertQuery = (dataMap, tableName) -> {
                    StringBuilder query = new StringBuilder("insert into ")
                            .append(tableName)
                            .append(" (attribute_id, entity_id, entity_type_id, `value`) values ");

                    Object[] params = new Object[dataMap.size() * 4];

                    int rowIndex = 0;
                    for (Map.Entry<Integer, AttributeData<Object>> attribute : dataMap.entrySet()) {
                        if (rowIndex > 0) {
                            query.append(", ");
                        }
                        query.append("(?, ?, ?, ?)");
                        params[rowIndex] = attribute.getKey();
                        params[rowIndex + 1] = getId();
                        params[rowIndex + 2] = connection.getEntityTypeId(entityName);
                        params[rowIndex + 3] = attribute.getValue().value;

                        rowIndex++;
                    }

                    query.append(" on duplicate key update `value` = VALUES(`value`)");

                    try {
                        PreparedStatement stmt = conn.prepareStatement(query.toString());

                        for (int i = 0; i < params.length; i++) {
                            stmt.setObject(i + 1, params[i]);
                        }

                        return stmt;
                    } catch (SQLException e) {
                        connection.debug("CONNERROR an SQL error occurred when trying to generate an EAV Insert Query");
                        e.printStackTrace();
                    }
                    return null;
                };

                int total = varcharData.size() + intData.size() + decimalData.size() + datetimeData.size();

                if (varcharData.size() > 0) {
                    PreparedStatement stmt = generateEavInsertQuery.apply(varcharData, "entity_attribute_varchar");
                    stmt.execute();
                }

                if (intData.size() > 0) {
                    PreparedStatement stmt = generateEavInsertQuery.apply(intData, "entity_attribute_int");
                    stmt.execute();
                }

                if (decimalData.size() > 0) {
                    PreparedStatement stmt = generateEavInsertQuery.apply(decimalData, "entity_attribute_decimal");
                    stmt.execute();
                }

                if (datetimeData.size() > 0) {
                    PreparedStatement stmt = generateEavInsertQuery.apply(datetimeData, "entity_attribute_datetime");
                    stmt.execute();
                }

                connection.debug("Updated " + total + " eav attributes for entity_id = " + getId() + ", entity_type = " + entityName);
            }
            catch (SQLException ex) {
                connection.debug("CONNERRROR an SQL error occurred when saving EAV data");
                ex.printStackTrace();
            }
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    private class AttributeData<T> {
        public T value;
        public String attributeName;
        public int attributeId;
        public String attributeType;

        public AttributeData(T value, String attributeName, int attributeId, String attributeType) {
            this.value = value;
            this.attributeName = attributeName;
            this.attributeId = attributeId;
            this.attributeType = attributeType;
        }
    }
}
