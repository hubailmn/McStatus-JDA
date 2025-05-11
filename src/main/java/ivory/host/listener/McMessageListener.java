package ivory.host.listener;

import ivory.host.data.GuildManager;
import ivory.host.data.cache.CacheManager;
import ivory.host.util.server.McEmbedBuilder;
import ivory.host.util.classes.DEvent;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class McMessageListener extends DEvent {

    private static byte[] imageToBytes(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Image conversion failed: " + e.getMessage());
            return null;
        }
    }

    private static byte[] readResourceBytes(String resourcePath) {
        try (InputStream is = McMessageListener.class.getResourceAsStream(resourcePath)) {
            return is != null ? is.readAllBytes() : null;
        } catch (IOException e) {
            System.err.println("Failed to read resource: " + resourcePath);
            return null;
        }
    }

    public static String getServerStatusBedrock(String address) {
        try {
            URL url = new URL("https://api.mcsrvstat.us/bedrock/3/" + address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to retrieve server status: " + e.getMessage());
        }
        return null;
    }

    public static int extractKey(String serverData, String key) {
        if (serverData == null) return -1;
        int index = serverData.indexOf("\"" + key + "\":");
        if (index != -1) {
            int startIndex = serverData.indexOf(":", index) + 1;
            int endIndex = serverData.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = serverData.indexOf("}", startIndex);

            String countStr = serverData.substring(startIndex, endIndex).replaceAll("\\D", "").trim();
            if (!countStr.isEmpty()) {
                try {
                    return Integer.parseInt(countStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) return;

        String[] args = e.getMessage().getContentRaw().split("\\s+");
        if (!args[0].equalsIgnoreCase("mc")) return;

        GuildManager guildManager = CacheManager.getGuildManager(e.getGuild().getIdLong());
        if (!guildManager.isMcCommandEnabled() || guildManager.getIp() == null) {
            e.getChannel().sendMessage("No settings found for this server.").queue();
            return;
        }

        String ip = guildManager.getIp();
        int port = guildManager.getPort();
        String serverName = guildManager.getName();
        boolean isRemovePort = guildManager.isHidePort();
        boolean isBedrock = guildManager.isBedrockCommandEnabled();

        String displayIp = isRemovePort ? ip : ip + ":" + port;

        McEmbedBuilder server = new McEmbedBuilder(ip, port);

        int onlinePlayers;
        String maxPlayers;

        if (!isBedrock) {
            onlinePlayers = Integer.parseInt(server.getOnlinePlayer());
            maxPlayers = server.getMaxPlayer();
        } else {
            String serverData = getServerStatusBedrock(ip + ":" + port);
            onlinePlayers = extractKey(serverData, "online");
            maxPlayers = Integer.toString(extractKey(serverData, "max"));
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(serverName)
                .addField("Server IP", "`" + displayIp + "`", true)
                .setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160")
                .setColor(0x8F00FF);

        List<FileUpload> attachments = new ArrayList<>();

        if (onlinePlayers >= 0) {
            embedBuilder.addField("Online Players", "`" + onlinePlayers + "`/`" + maxPlayers + "` :video_game:", true);

            if (!isBedrock) {
                BufferedImage motdImage = server.motd();
                BufferedImage iconImage = server.getIcon();

                byte[] iconBytes = imageToBytes(iconImage);
                byte[] motdBytes = imageToBytes(motdImage);

                if (iconBytes != null) {
                    attachments.add(FileUpload.fromData(iconBytes, "icon.png"));
                    embedBuilder.setThumbnail("attachment://icon.png");
                }
                if (motdBytes != null) {
                    attachments.add(FileUpload.fromData(motdBytes, "motd.png"));
                    embedBuilder.setImage("attachment://motd.png");
                }
            }
        } else {
            embedBuilder.addField("Server Status", "`Offline`", true);

            byte[] offlineIconBytes = readResourceBytes("/offline-icon.png");
            byte[] offlineMotdBytes = readResourceBytes("/offline-motd.png");

            if (offlineIconBytes != null) {
                attachments.add(FileUpload.fromData(offlineIconBytes, "offline-icon.png"));
                embedBuilder.setThumbnail("attachment://offline-icon.png");
            }
            if (offlineMotdBytes != null) {
                attachments.add(FileUpload.fromData(offlineMotdBytes, "offline-motd.png"));
                embedBuilder.setImage("attachment://offline-motd.png");
            } else {
                embedBuilder.setImage("https://api.loohpjames.com/serverbanner.png?ip=" + ip + "&port=" + port + "&name=" + ip);
            }
        }

        MessageEmbed embed = embedBuilder.build();

        if (!attachments.isEmpty()) {
            e.getChannel().sendMessageEmbeds(embed).addFiles(attachments).queue();
        } else {
            e.getChannel().sendMessageEmbeds(embed).queue();
        }
    }
}