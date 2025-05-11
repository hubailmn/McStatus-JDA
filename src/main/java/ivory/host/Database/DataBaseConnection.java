package ivory.host.Database;
import ivory.host.util.CSend;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {
    private static volatile String dbPath;

    private DataBaseConnection() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Connection getConnection() {
        try {
            if (dbPath == null) {
                synchronized (DataBaseConnection.class) {
                    if (dbPath == null) {
                        dbPath = getSQLitePath();
                    }
                }
            }
            return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (SQLException e) {
            CSend.error("Failed to establish a database connection.");
            CSend.error(e);
            throw new RuntimeException("Failed to establish database connection", e);
        }
    }

    private static String getSQLitePath() {
        String fileName = "database/ivory.db";
        File dbFile = new File(fileName);
        File parentDir = dbFile.getParentFile();

        if (!parentDir.exists() && !parentDir.mkdirs()) {
            CSend.error("Could not create database folder.");
            throw new RuntimeException("Could not create database folder");
        }

        if (!dbFile.exists()) {
            try {
                if (!dbFile.createNewFile()) {
                    CSend.error("Could not create SQLite database file.");
                    throw new RuntimeException("Could not create SQLite database file");
                }
            } catch (IOException e) {
                CSend.error("Error while creating SQLite database file.");
                CSend.error(e);
                throw new RuntimeException("Error creating SQLite database file", e);
            }
        }

        return dbFile.getPath();
    }
}