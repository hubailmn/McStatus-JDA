package ivory.host.util.classes;

import ivory.host.Database.DataBaseConnection;
import ivory.host.util.CSend;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Getter
public abstract class TableBuilder {

    private final Connection connection;

    public TableBuilder() {
        this(DataBaseConnection.getConnection());
    }

    public TableBuilder(Connection connection) {
        this.connection = connection;

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
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

}
