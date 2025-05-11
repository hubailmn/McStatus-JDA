package ivory.host.command.info;

import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class TOSCommand extends DCommand {

    public TOSCommand() {
        super("terms-of-service", "View Terms of Service.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        e.reply("https://github.com/I5MS/McStatus/blob/main/TERMS_OF_SERVICE.md")
                .setEphemeral(true)
                .queue();

    }

}
