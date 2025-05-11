package ivory.host.command;

import ivory.host.util.server.McEmbedBuilder;
import ivory.host.util.classes.DCommand;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class McStatusCommand extends DCommand {

    public McStatusCommand() {
        super("mc-status", "Effortlessly monitors the status and details of Java Minecraft servers.");
    }

    @Override
    public CommandData register() {
        return Commands.slash(getName(), getDescription())
                .addOptions(new OptionData(OptionType.STRING, "ip", "Specify an IP address or domain.", true),
                        new OptionData(OptionType.STRING, "port", "Specify a port number.", false));
    }

    @SneakyThrows
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (!e.getName().equals(getName())) return;

        String ip = e.getOption("ip").getAsString();
        String port = e.getOption("port").getAsString();

        McEmbedBuilder server = new McEmbedBuilder(ip, Integer.parseInt(port));

        int onlinePlayers = Integer.parseInt(server.getOnlinePlayer());
        String maxPlayersStr = server.getMaxPlayer();

        if (onlinePlayers >= 0) {
            EmbedBuilder embedBuilder = new EmbedBuilder();

            byte[] iconBytes;
            byte[] motdBytes;

            BufferedImage motdImage = server.motd();
            BufferedImage iconImage = server.getIcon();

            ByteArrayOutputStream iconBaos = new ByteArrayOutputStream();
            ImageIO.write(iconImage, "png", iconBaos);
            iconBytes = iconBaos.toByteArray();

            ByteArrayOutputStream motdBaos = new ByteArrayOutputStream();
            ImageIO.write(motdImage, "png", motdBaos);
            motdBytes = motdBaos.toByteArray();

            embedBuilder.setThumbnail("attachment://icon.png");
            embedBuilder.setImage("attachment://motd.png");

            embedBuilder.setTitle(ip);
            embedBuilder.addField("Server IP", "`" + port + ":" + port + "`", true);
            embedBuilder.addField("Online Players", "`" + onlinePlayers + "`" + "/" + "`" + maxPlayersStr + "`:video_game:", true);
            embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
            embedBuilder.setColor(0x8F00FF);

            MessageEmbed embed = embedBuilder.build();
            e.replyEmbeds(embed)
                    .addFiles(FileUpload.fromData(iconBytes, "icon.png"))
                    .addFiles(FileUpload.fromData(motdBytes, "motd.png"))
                    .queue();
        } else {
            e.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
        }

    }

}
