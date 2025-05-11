package ivory.host.command.setup;

import ivory.host.Database.table.GuildTable;
import ivory.host.data.GuildManager;
import ivory.host.data.cache.CacheManager;
import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Objects;

public class SetUpCommand extends DCommand {

    public SetUpCommand() {
        super("setup", "Setting up server IP and port.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        if (e.getMember() != null && !e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            e.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        Modal setup = Modal.create("setup", "Setup")
                .addActionRow(
                        TextInput.create("name", "Enter server name.", TextInputStyle.SHORT)
                                .setPlaceholder("Server Name")
                                .setRequired(true)
                                .build()
                )
                .addActionRow(
                        TextInput.create("option1", "Enter server ip.", TextInputStyle.SHORT)
                                .setPlaceholder("Server IP")
                                .setRequired(true)
                                .build()
                )
                .addActionRow(
                        TextInput.create("option2", "Enter server port.", TextInputStyle.SHORT)
                                .setPlaceholder("Server Port")
                                .setRequired(true)
                                .build()
                )
                .build();

        e.replyModal(setup).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent e) {
        if (!e.getModalId().equals("setup")) return;
        if (e.getGuild() == null) return;

        String name = Objects.requireNonNull(e.getValue("name")).getAsString();
        String ip = Objects.requireNonNull(e.getValue("option1")).getAsString();
        int port = Integer.parseInt(Objects.requireNonNull(e.getValue("option2")).getAsString());

        GuildManager guildManager = CacheManager.getGuildManager(e.getGuild().getIdLong());
        guildManager.setName(name);
        guildManager.setIp(ip);
        guildManager.setPort(port);
        guildManager.setMcCommandEnabled(true);

        e.reply("✅ Server setup complete!\n" +
                "Name: " + name + "\n" +
                "IP: " + ip + "\n" +
                "Port: " + port).setEphemeral(true).queue();


        GuildTable.getInstance().updateGuild(guildManager);
    }
}
