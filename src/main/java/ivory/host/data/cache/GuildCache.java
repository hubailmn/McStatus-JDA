package ivory.host.data.cache;

import ivory.host.data.GuildManager;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public class GuildCache {

    @Getter
    private static final ConcurrentHashMap<Long, GuildManager> guildManagers = new ConcurrentHashMap<>();

    public static void addGuild(Long guildID, GuildManager guildManager) {
        guildManagers.put(guildID, guildManager);
    }

    public static void removeGuild(Long guildID) {
        guildManagers.remove(guildID);
    }

    public static GuildManager getGuild(Long guildID) {
        return guildManagers.get(guildID);
    }

    public static boolean containsGuild(Long guildID) {
        return guildManagers.containsKey(guildID);
    }

    public static void clearAll() {
        guildManagers.clear();
    }

}
