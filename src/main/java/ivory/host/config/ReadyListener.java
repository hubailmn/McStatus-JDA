package ivory.host.config;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashSet;
import java.util.Set;

public class ReadyListener extends ListenerAdapter {

    private final GuildSettingsManager guildSettingsManager;

    public ReadyListener(GuildSettingsManager guildSettingsManager) {
        this.guildSettingsManager = guildSettingsManager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        Set<String> guildIdsInBot = new HashSet<>();

        for (Guild guild : event.getJDA().getGuilds()) {
            guildIdsInBot.add(guild.getId());
        }

        for (String guildId : guildIdsInBot) {
            JsonObject settings = guildSettingsManager.loadGuildSettings(guildId);
            if (settings.size() == 0) {
                guildSettingsManager.saveGuildSettings(guildId, new JsonObject());
            }
        }
    }
}
