package ivory.host.Database;


import ivory.host.util.CSend;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {

    @Setter
    private static Connection connection;

    private DataBaseConnection() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                synchronized (DataBaseConnection.class) {
                    if (connection == null || connection.isClosed()) {
                        initialize();
                    }
                }
            }
        } catch (SQLException e) {
            CSend.error("Failed to check or establish a database connection.");
            CSend.error(e);
        }
        return connection;
    }

    public static void initialize() {
        try {
            connectToSQLite();
        } catch (Exception e) {
            CSend.error("Database initialization failed.");
            CSend.error(e);
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                CSend.info("Database connection has been closed.");
            }
        } catch (SQLException e) {
            CSend.error("Error while closing the database connection.");
            CSend.error(e);
        }
    }

    private static void connectToSQLite() {
        try {
            CSend.info("Connecting to SQLite...");
            String path = getSQLitePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            CSend.info("SQLite connection established.");
        } catch (SQLException e) {
            CSend.error("Failed to connect to SQLite.");
            CSend.error(e);
        }
    }

    private static String getSQLitePath() {
        String fileName = "database/ivory.db";
        File dbFile = new File(fileName);

        if (!dbFile.exists() && !dbFile.mkdirs()) {
            CSend.error("Could not create database folder.");
        }

        if (!dbFile.exists()) {
            try {
                if (!dbFile.createNewFile()) {
                    CSend.error("Could not create SQLite database file.");
                }
            } catch (IOException e) {
                CSend.error("Error while creating SQLite database file.");
                CSend.error(e);
            }
        }

        return dbFile.getPath();
    }

}

