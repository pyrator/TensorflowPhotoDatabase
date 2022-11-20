package uk.khall.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlLiteBridge {
    public static Connection createSqliteConnection(String dbName) throws SQLException {
        String driverName = "org.sqlite.JDBC";
        // Create a connection to the database

        String url = "jdbc:sqlite:" + dbName ;
        Connection connection = DriverManager.getConnection(url);
        return connection;
    }
}
