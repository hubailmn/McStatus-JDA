package ivory.host.data.cache;

import ivory.host.Database.table.GuildTable;
import ivory.host.data.GuildManager;

public class CacheManager {

    public static GuildManager getGuildManager(Long guildID) {
        if (GuildCache.containsGuild(guildID)) {
            return GuildCache.getGuild(guildID);
        }

        return GuildTable.getInstance().getGuild(guildID);
    }
}
