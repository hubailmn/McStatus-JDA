package ivory.host.util.classes;

import ivory.host.McStatusBot;
import lombok.Getter;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;

@Getter
public class DEvent extends ListenerAdapter {

    ShardManager shardManager = McStatusBot.getShardManager();

}
