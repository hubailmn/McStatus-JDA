package ivory.host.listener;

import ivory.host.Database.table.GuildTable;
import ivory.host.data.cache.GuildCache;
import ivory.host.util.classes.DEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import org.jetbrains.annotations.NotNull;

public class GuildReadyListener extends DEvent {

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent e) {
        Long guildID = e.getGuild().getIdLong();
        if (GuildTable.getInstance().getGuild(guildID) == null) {
            GuildTable.getInstance().addGuild(guildID);
        }

        GuildCache.addGuild(guildID, GuildTable.getInstance().getGuild(guildID));
    }
}
