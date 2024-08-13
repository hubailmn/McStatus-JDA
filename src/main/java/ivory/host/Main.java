package ivory.host;

import ivory.host.config.GuildJoinListener;
import ivory.host.config.GuildSettingsManager;
import ivory.host.config.ReadyListener;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Main {
    public static void main(String[] args){
        GuildSettingsManager guildSettingsManager = new GuildSettingsManager();

        DefaultShardManagerBuilder.createDefault("")
                .addEventListeners(new ReadyListener(guildSettingsManager))
                .addEventListeners(new GuildJoinListener(guildSettingsManager))
                .addEventListeners(new Command(guildSettingsManager))
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Minecraft Server Status"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .build();
    }
}