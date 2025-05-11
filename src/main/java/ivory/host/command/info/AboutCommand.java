package ivory.host.command.info;

import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class AboutCommand extends DCommand {

    public AboutCommand() {
        super("about", "Provides bot creator info and support Discord link.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        e.reply("This bot was created by Ivory Host." +
                        "\nFor support, join our Discord server: https://discord.gg/nr4JYefgP3")
                .setEphemeral(true)
                .queue();

    }

}
