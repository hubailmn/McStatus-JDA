package ivory.host.config;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildJoinListener extends ListenerAdapter {

    private final GuildSettingsManager guildSettingsManager;

    public GuildJoinListener(GuildSettingsManager guildSettingsManager) {
        this.guildSettingsManager = guildSettingsManager;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        String guildId = event.getGuild().getId();
        JsonObject settings = guildSettingsManager.loadGuildSettings(guildId);

        if (settings.size() == 0) {
            guildSettingsManager.saveGuildSettings(guildId, new JsonObject());
        }
    }
}
