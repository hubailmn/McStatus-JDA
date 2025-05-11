package ivory.host.command.info;

import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PrivacyPolicyCommand extends DCommand {

    public PrivacyPolicyCommand() {
        super("privacy-and-policy", "View Privacy Policy");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        e.reply("https://github.com/I5MS/McStatus/blob/main/PRIVACY_POLICY.md")
                .setEphemeral(true)
                .queue();

    }

}
