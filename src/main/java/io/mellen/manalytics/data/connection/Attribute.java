package io.mellen.manalytics.data.connection;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Attribute {
    public int id;
    public String type;
    public String name;

    public Object getValue(ResultSet results, String columnName) throws SQLException {
        switch (type) {
            case "varchar":
                return results.getString(columnName);

            case "int":
                return results.getInt(columnName);

            case "datetime":
                return results.getTimestamp(columnName);

            case "decimal":
                return results.getBigDecimal(columnName);
        }

        return null;
    }
}
