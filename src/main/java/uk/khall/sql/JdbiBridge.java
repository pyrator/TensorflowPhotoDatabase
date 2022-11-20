package uk.khall.sql;

import org.jdbi.v3.core.Jdbi;



public class JdbiBridge {

    public static Jdbi createSqliteJdbiConnection(String dbName) {
        // Create a connection to the database
        String url = "jdbc:sqlite:" + dbName ;
        Jdbi jdbi = Jdbi.create(url);
        return jdbi;
    }
}
