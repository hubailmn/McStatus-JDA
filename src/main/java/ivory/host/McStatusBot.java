package ivory.host;

import ivory.host.Database.DataBaseConnection;
import ivory.host.util.Registers;
import ivory.host.util.classes.DEvent;
import ivory.host.util.classes.TableBuilder;
import lombok.Getter;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.reflections.Reflections;

import java.util.Set;

public class McStatusBot {

    @Getter
    private static final String prefix = "McStatusBot";

    @Getter
    private static final boolean debug = true;

    @Getter
    private static ShardManager shardManager;

    public static void main(String[] args) {

        registerDatabaseTables();

        shardManager = DefaultShardManagerBuilder.createDefault("")
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Minecraft Server Status"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .build();

        registerEvents();
        new Registers();
    }

    public static void registerDatabaseTables() {
        Reflections reflections = new Reflections("ivory.host.Database.table");
        Set<Class<? extends TableBuilder>> databaseTable = reflections.getSubTypesOf(TableBuilder.class);

        for (Class<? extends TableBuilder> clazz : databaseTable) {
            try {
                clazz.getDeclaredConstructor().newInstance();
                System.out.printf("\n\t- Registered database table: %s%n", clazz.getSimpleName());
            } catch (Exception e) {
                System.err.printf("\t\t- Failed to register database table: %s - %s%n", clazz.getSimpleName(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void registerEvents() {
        System.out.println("\n\t- Registering Listeners");

        Reflections reflections = new Reflections("ivory.host.listener");
        Set<Class<? extends DEvent>> subCommandClasses = reflections.getSubTypesOf(DEvent.class);
        for (Class<? extends DEvent> clazz : subCommandClasses) {
            try {
                DEvent eventListener = clazz.getDeclaredConstructor().newInstance();
                getShardManager().addEventListener(eventListener);
                System.out.println("\t\t- Registered listener: " + clazz.getSimpleName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}