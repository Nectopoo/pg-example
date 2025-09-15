package test.dev.smartreplication.example.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@UtilityClass
public class Utils {
    public boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        var metaData = rs.getMetaData();
        int columns = metaData.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            if (columnName.equals(metaData.getColumnName(i))) {
                return true;
            }
        }
        log.error("Column {} not found in ResultSet", columnName);
        return false;
    }
}
