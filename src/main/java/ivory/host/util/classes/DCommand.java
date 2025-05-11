package ivory.host.util.classes;

import ivory.host.McStatusBot;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.sharding.ShardManager;

@Getter
public abstract class DCommand extends ListenerAdapter {

    ShardManager shardManager = McStatusBot.getShardManager();

    String name;
    String description;

    public DCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public CommandData register() {
        return Commands.slash(getName(), getDescription());
    }

    public abstract void onSlashCommandInteraction(SlashCommandInteractionEvent e);


}
