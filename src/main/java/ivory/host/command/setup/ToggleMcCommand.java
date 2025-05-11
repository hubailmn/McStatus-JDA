package ivory.host.command.setup;

import ivory.host.Database.table.GuildTable;
import ivory.host.data.GuildManager;
import ivory.host.data.cache.CacheManager;
import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ToggleMcCommand extends DCommand {

    public ToggleMcCommand() {
        super("toggle-mc", "Enable/Disable the mc command.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        if (e.getMember() != null && !e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        GuildManager guildManager = CacheManager.getGuildManager(e.getGuild().getIdLong());

        if (guildManager == null) {
            GuildTable.getInstance().addGuild(e.getGuild().getIdLong());
            guildManager = GuildTable.getInstance().getGuild(e.getGuild().getIdLong());
        }

        guildManager.setMcCommandEnabled(!guildManager.isMcCommandEnabled());

        e.reply("The mc command has been " + (guildManager.isMcCommandEnabled() ? "Enabled" : "Disabled")).setEphemeral(true).queue();

        GuildTable.getInstance().updateGuild(guildManager);
    }
}
