package ivory.host.Database.table;

import ivory.host.Database.DataBaseConnection;
import ivory.host.data.GuildManager;
import ivory.host.util.classes.TableBuilder;
import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class GuildTable extends TableBuilder {

    @Getter
    private static final GuildTable instance = new GuildTable();

    @SneakyThrows
    public void addGuild(Long guildID) {
        String query = "INSERT INTO Guild (guild_id, name, ip, port, mc_command_enabled, bedrock_mc, fake_port_enabled, remove_port_enabled) " +
                "VALUES (?, '', '', 0, false, false, false, false)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, guildID.toString());
            ps.executeUpdate();
        }
    }

    @SneakyThrows
    public void deleteGuild(Long guildID) {
        String query = "DELETE FROM Guild WHERE guild_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, guildID.toString());
            ps.executeUpdate();
        }
    }

    @SneakyThrows
    public void updateGuild(GuildManager guildManager) {
        String query = """
                UPDATE Guild SET
                    name = ?,
                    ip = ?,
                    port = ?,
                    mc_command_enabled = ?,
                    bedrock_mc = ?,
                    fake_port_enabled = ?,
                    remove_port_enabled = ?
                WHERE guild_id = ?
                """;
        try (Connection connection = DataBaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, guildManager.getName());
            ps.setString(2, guildManager.getIp());
            ps.setInt(3, guildManager.getPort());
            ps.setBoolean(4, guildManager.isMcCommandEnabled());
            ps.setBoolean(5, guildManager.isBedrockCommandEnabled());
            ps.setBoolean(6, guildManager.isFakePortEnabled());
            ps.setBoolean(7, guildManager.isHidePort());
            ps.setString(8, guildManager.getGuildID().toString());
            ps.executeUpdate();
        }
    }

    @SneakyThrows
    public GuildManager getGuild(Long guildID) {
        String query = "SELECT * FROM Guild WHERE guild_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, guildID.toString());

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GuildManager(
                            guildID,
                            rs.getString("name"),
                            rs.getString("ip"),
                            rs.getInt("port"),
                            rs.getBoolean("mc_command_enabled"),
                            rs.getBoolean("bedrock_mc"),
                            rs.getBoolean("fake_port_enabled"),
                            rs.getBoolean("remove_port_enabled")
                    );
                }
            }
        }
        return null;
    }

    @SneakyThrows
    @Override
    public void createTable() {
        String query = """
                CREATE TABLE IF NOT EXISTS Guild (
                    guild_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    ip TEXT NOT NULL,
                    port INTEGER NOT NULL,
                    mc_command_enabled BOOLEAN NOT NULL,
                    bedrock_mc BOOLEAN NOT NULL,
                    fake_port_enabled BOOLEAN NOT NULL,
                    remove_port_enabled BOOLEAN NOT NULL
                )
                """;

        try (Statement statement = getConnection().createStatement()) {
            statement.execute(query);
        }
    }

}
