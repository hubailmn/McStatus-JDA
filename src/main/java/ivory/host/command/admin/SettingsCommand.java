package ivory.host.command.admin;

import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SettingsCommand extends DCommand {

    public SettingsCommand() {
        super("settings", "Provides bot creator info and support Discord link.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        if (e.getMember() != null && !e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            e.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        e.reply("Test!")
                .setEphemeral(true)
                .queue();

    }

}
