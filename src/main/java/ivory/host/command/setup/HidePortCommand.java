package ivory.host.command.setup;

import ivory.host.Database.table.GuildTable;
import ivory.host.data.GuildManager;
import ivory.host.data.cache.CacheManager;
import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class HidePortCommand extends DCommand {

    public HidePortCommand() {
        super("hide-port", "It won't show port in mc command.");
    }

    @Override
    public CommandData register() {
        return Commands.slash(getName(), getDescription());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;
        if (e.getGuild() == null) return;

        if (e.getMember() != null && !e.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        GuildManager guildManager = CacheManager.getGuildManager(e.getGuild().getIdLong());

        if (guildManager == null) {
            GuildTable.getInstance().addGuild(e.getGuild().getIdLong());
            guildManager = GuildTable.getInstance().getGuild(e.getGuild().getIdLong());
        }

        guildManager.setHidePort(!guildManager.isHidePort());

        e.reply("The port has been " + (guildManager.isHidePort() ? "Hidden" : "Shown")).setEphemeral(true).queue();

        GuildTable.getInstance().updateGuild(guildManager);
    }

}
