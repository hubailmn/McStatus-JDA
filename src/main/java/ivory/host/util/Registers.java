package ivory.host.util;

import ivory.host.McStatusBot;
import ivory.host.util.classes.DCommand;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Registers extends ListenerAdapter {

    @Getter
    private static final ShardManager shardManager = McStatusBot.getShardManager();
    private static final Set<Class<? extends DCommand>> registeredCommandClasses = new HashSet<>();

    public Registers() {
        McStatusBot.getShardManager().addEventListener(this);
    }

    @Override
    public void onGuildReady(GuildReadyEvent e) {
        registerCommands(e.getGuild());
    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {
        registerCommands(e.getGuild());
    }

    private void registerCommands(Guild guild) {

        List<CommandData> commandList = new ArrayList<>();

        System.out.println("\t- Registering Commands");
        Reflections reflections = new Reflections("ivory.host.command");
        Set<Class<? extends DCommand>> commandClasses = reflections.getSubTypesOf(DCommand.class);
        for (Class<? extends DCommand> clazz : commandClasses) {
            try {
                DCommand dCommand = clazz.getDeclaredConstructor().newInstance();
                CommandData commandData = dCommand.register();
                if (commandData != null) {
                    commandList.add(commandData);

                    if (!registeredCommandClasses.contains(clazz)) {
                        getShardManager().addEventListener(dCommand);
                        registeredCommandClasses.add(clazz);
                    }

                    System.out.println("\t\t- Registered Command: " + dCommand.getName());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (!commandList.isEmpty()) {
            guild.updateCommands().addCommands(commandList).queue();
        }
    }

}
