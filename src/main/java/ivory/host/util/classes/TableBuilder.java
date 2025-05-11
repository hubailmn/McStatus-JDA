package ivory.host.util.classes;

import ivory.host.Database.DataBaseConnection;
import ivory.host.util.CSend;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class TableBuilder {

    protected Connection getConnection() {
        return DataBaseConnection.getConnection();
    }

    public TableBuilder() {
        try {
            createTable();
            init();
        } catch (SQLException e) {
            CSend.error("Failed to initialize table");
            CSend.error(e);
        }
    }

    protected abstract void createTable() throws SQLException;

    protected void init() throws SQLException {
    }

    protected void executeUpdate(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}