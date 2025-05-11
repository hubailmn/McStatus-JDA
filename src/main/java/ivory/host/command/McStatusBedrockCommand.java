package ivory.host.command;

import ivory.host.util.classes.DCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import static ivory.host.listener.McMessageListener.extractKey;
import static ivory.host.listener.McMessageListener.getServerStatusBedrock;

public class McStatusBedrockCommand extends DCommand {

    public McStatusBedrockCommand() {
        super("mc-status-bedrock", "Effortlessly monitors the status and details of Bedrock Minecraft servers.");
    }

    @Override
    public CommandData register() {
        return Commands.slash(getName(), getDescription())
                .addOptions(
                        new OptionData(OptionType.STRING, "ip", "Specify an IP address or domain.", true),
                        new OptionData(OptionType.STRING, "port", "Specify a port number.", false)
                );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        String ip = e.getOption("ip").getAsString();
        String portString = e.getOption("port").getAsString();

        String firstInput = ip.trim();
        String secondInput = portString.trim();
        String finalOutput = secondInput.isEmpty() ? firstInput : firstInput + ":" + secondInput;

        String serverData = getServerStatusBedrock(finalOutput);
        if (serverData != null) {
            int port = extractKey(serverData, "port");
            int onlinePlayers = extractKey(serverData, "online");
            int maxPlayers = extractKey(serverData, "max");

            String online = Integer.toString(onlinePlayers);
            String max = Integer.toString(maxPlayers);

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(ip);
            embedBuilder.addField("Server IP", "`" + ip + ":" + port + "`", true);
            embedBuilder.addField("Online Players", "`" + online + "`" + "/" + "`" + max + "`:video_game:", true);
            embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
            embedBuilder.setColor(0x8F00FF);

            // Build the embed object
            MessageEmbed embed = embedBuilder.build();

            if (!(onlinePlayers == -1)) {
                e.replyEmbeds(embed).queue();
            } else {
                e.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
            }
        } else {
            e.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
        }

    }

}
