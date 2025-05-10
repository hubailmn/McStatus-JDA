package ivory.host;

import com.google.gson.JsonObject;
import ivory.host.config.GuildSettingsManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Command extends ListenerAdapter {

    private final GuildSettingsManager guildSettingsManager;

    public Command(GuildSettingsManager guildSettingsManager) {
        this.guildSettingsManager = guildSettingsManager;
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String[] args = event.getMessage().getContentRaw().split("\\s+");
        if (!args[0].equalsIgnoreCase("mc")) {
            return;
        }

        String guildId = event.getGuild().getId();
        if (!this.guildSettingsManager.isMCCommandEnabled(guildId)) {
            return;
        }

        JsonObject settings = this.guildSettingsManager.loadGuildSettings(guildId);
        if (!(settings.has("option1") && settings.has("option2"))) {
            event.getChannel().sendMessage("No settings found for this server.").queue();
            return;
        }

        // Get base settings from configuration
        String ip = settings.get("option1").getAsString().trim();
        String port = settings.get("option2").getAsString().trim();
        String serverName = settings.has("name") && !settings.get("name").isJsonNull()
                ? settings.get("name").getAsString()
                : "server_name";

        // Determine whether the port should be shown or removed based on config
        boolean isRemovePort = this.guildSettingsManager.isRemovePort(guildId);
        String displayIp = isRemovePort ? ip : ip + ":" + port;

        // Check if this is a Bedrock server (and get status accordingly)
        boolean isBedrock = this.guildSettingsManager.isBedrockMcEnabled(guildId);

        // Retrieve online and max players. (This example uses a renderer API which might throw an IOException.)
        int onlinePlayers = -1;
        String maxPlayersStr = "-1";

        try {
            if(!isBedrock) {
                onlinePlayers = Integer.parseInt(JavaMinecraft.onlineplayer(ip)
                        .port(Integer.parseInt(port))
                        .getOnlinePlayer());
                maxPlayersStr = JavaMinecraft.maxplayer(ip)
                        .port(Integer.parseInt(port))
                        .getMaxPlayer();
            }else {
                String serverData = getServerStatusBedrock(ip + ":"+ port);
                assert serverData != null;
                onlinePlayers = extractPlayerCount(serverData, "online");
                 int maxPlayers = extractPlayerCount(serverData, "max");
                 maxPlayersStr = Integer.toString(maxPlayers);
            }
        } catch (IOException ignored) {
        }

        String online = Integer.toString(onlinePlayers);
        String max = maxPlayersStr; // Already a string representation of max players

        // Start building the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(serverName);
        embedBuilder.addField("Server IP", "`" + displayIp + "`", true);

        byte[] iconBytes = null;
        byte[] motdBytes = null;
        // Also, prepare offline resource byte arrays
        byte[] offlineIconBytes = null;
        byte[] offlineMotdBytes = null;

        if (!isBedrock) {
            try {
                BufferedImage motdImage = JavaMinecraft.generate(ip)
                        .port(Integer.parseInt(port))
                        .motd();
                BufferedImage iconImage = JavaMinecraft.icon(ip)
                        .port(Integer.parseInt(port))
                        .getIcon();
                // Convert the icon BufferedImage to a byte array.
                ByteArrayOutputStream iconBaos = new ByteArrayOutputStream();
                ImageIO.write(iconImage, "png", iconBaos);
                iconBytes = iconBaos.toByteArray();

                // Convert the MOTD BufferedImage to a byte array.
                ByteArrayOutputStream motdBaos = new ByteArrayOutputStream();
                ImageIO.write(motdImage, "png", motdBaos);
                motdBytes = motdBaos.toByteArray();

                // Set the thumbnail and image in the embed using attachment URLs.
                embedBuilder.setThumbnail("attachment://icon.png");
                embedBuilder.setImage("attachment://motd.png");
            } catch (IOException ignored) {
            }
        }

        // Add field for online players or show offline status.
        if (onlinePlayers >= 0) {
            embedBuilder.addField("Online Players", "`" + online + "`/`" + max + "`:video_game:", true);
        } else {
            embedBuilder.addField("Server Status", "`Offline`", true);

            // Load the offline resources
            try (InputStream is = getClass().getResourceAsStream("/offline-icon.png")) { // note the leading '/'
                if (is != null) {
                    offlineIconBytes = is.readAllBytes();
                } else {
                    System.err.println("Resource file offline-icon.png not found.");
                }
            } catch (IOException ignored) {
            }

            try (InputStream is = getClass().getResourceAsStream("/offline-motd.png")) { // note the leading '/'
                if (is != null) {
                    offlineMotdBytes = is.readAllBytes();
                } else {
                    System.err.println("Resource file offline-motd.png not found.");
                }
            } catch (IOException ignored) {
            }

            // For offline, also update the embed image if necessary
            if (offlineMotdBytes != null) {
                embedBuilder.setImage("attachment://offline-motd.png");
            } else {
                // Fallback if offline MOTD resource is missing.
                embedBuilder.setImage("https://api.loohpjames.com/serverbanner.png?ip=" + ip + "&port=" + port + "&name=" + ip);
            }
            if (offlineIconBytes != null) {
                embedBuilder.setThumbnail("attachment://offline-icon.png");
            }
        }

        embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
        embedBuilder.setColor(0x8F00FF);

        MessageEmbed embed = embedBuilder.build();

        // Use FileUpload.fromData to attach the files, checking if they're non-null.
        List<FileUpload> attachments = new ArrayList<>();
        if (onlinePlayers >= 0) {
            // Online server: attach the online images if they were successfully loaded.
            if (iconBytes != null) {
                attachments.add(FileUpload.fromData(iconBytes, "icon.png"));
            }
            if (motdBytes != null) {
                attachments.add(FileUpload.fromData(motdBytes, "motd.png"));
            }
        } else {
            if (offlineIconBytes != null) {
                attachments.add(FileUpload.fromData(offlineIconBytes, "offline-icon.png"));
            }
            if (offlineMotdBytes != null) {
                attachments.add(FileUpload.fromData(offlineMotdBytes, "offline-motd.png"));
            }
        }

        if (!attachments.isEmpty()) {
            event.getChannel().sendMessageEmbeds(embed)
                    .addFiles(attachments)
                    .queue();
        } else {
            event.getChannel().sendMessageEmbeds(embed).queue();
        }
    }




    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {


        OptionMapping messageOption = event.getOption("ip");
        OptionMapping messageOption1 = event.getOption("port");

        OptionMapping enable_disable;
        Boolean true_false;
        enable_disable = event.getOption("enable");
        assert enable_disable != null;

        String guildId = Objects.requireNonNull(event.getGuild()).getId();
        String command = event.getName();

        switch (command) {
            case "about" ->
                    event.reply("This bot was created by 5MS_.\nFor support, join our Discord server: https://discord.gg/nr4JYefgP3")
                            .setEphemeral(true)
                            .queue();
            case "mcstatus" -> {

                assert messageOption != null;
                String ip = messageOption.getAsString();
                assert messageOption1 != null;
                String port = messageOption1.getAsString();


                // Retrieve online and max players. (This example uses a renderer API which might throw an IOException.)
                int onlinePlayers = -1;
                String maxPlayersStr = "-1";
                try {
                    onlinePlayers = Integer.parseInt(JavaMinecraft.onlineplayer(ip)
                            .port(Integer.parseInt(port))
                            .getOnlinePlayer());
                    maxPlayersStr = JavaMinecraft.maxplayer(ip)
                            .port(Integer.parseInt(port))
                            .getMaxPlayer();
                } catch (IOException ignored) {
                }
                if (onlinePlayers >= 0) {
                    EmbedBuilder embedBuilder = new EmbedBuilder();

                    byte[] iconBytes = null;
                    byte[] motdBytes = null;

                    try {
                        BufferedImage motdImage = JavaMinecraft.generate(ip)
                                .port(Integer.parseInt(port))
                                .motd();
                        BufferedImage iconImage = JavaMinecraft.icon(ip)
                                .port(Integer.parseInt(port))
                                .getIcon();
                        // Convert the icon BufferedImage to a byte array.
                        ByteArrayOutputStream iconBaos = new ByteArrayOutputStream();
                        ImageIO.write(iconImage, "png", iconBaos);
                        iconBytes = iconBaos.toByteArray();

                        // Convert the MOTD BufferedImage to a byte array.
                        ByteArrayOutputStream motdBaos = new ByteArrayOutputStream();
                        ImageIO.write(motdImage, "png", motdBaos);
                        motdBytes = motdBaos.toByteArray();

                        // Set the thumbnail and image in the embed using attachment URLs.
                        embedBuilder.setThumbnail("attachment://icon.png");
                        embedBuilder.setImage("attachment://motd.png");
                    } catch (IOException ignored) {
                    }

                    embedBuilder.setTitle(ip);
                    embedBuilder.addField("Server IP", "`" + port + ":" + port + "`", true);
                    embedBuilder.addField("Online Players", "`" + onlinePlayers + "`" + "/" + "`" + maxPlayersStr + "`:video_game:", true);
                    embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
                    embedBuilder.setColor(0x8F00FF);

                    // Build the embed object
                    MessageEmbed embed = embedBuilder.build();
                    assert iconBytes != null;
                    assert motdBytes != null;
                    event.replyEmbeds(embed)
                            .addFiles(FileUpload.fromData(iconBytes, "icon.png"))
                            .addFiles(FileUpload.fromData(motdBytes, "motd.png"))
                            .queue();
                } else {
                    event.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
                }
            }
            case "mcstatusbedrock" -> {

                assert messageOption != null;
                String serverip = messageOption.getAsString();
                assert messageOption1 != null;
                String serverport = messageOption1.getAsString();

                String firstInput = serverip.trim();
                String secondInput = serverport.trim();
                String finalOutput = secondInput.isEmpty() ? firstInput : firstInput + ":" + secondInput;

                String serverData = getServerStatusBedrock(finalOutput);
                if (serverData != null) {
                    int port = extractPlayerCount(serverData, "port");
                    int onlinePlayers = extractPlayerCount(serverData, "online");
                    int maxPlayers = extractPlayerCount(serverData, "max");

                    String online = Integer.toString(onlinePlayers);
                    String max = Integer.toString(maxPlayers);


                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle(serverip);
                    embedBuilder.addField("Server IP", "`" + serverip + ":" + port + "`", true);
                    embedBuilder.addField("Online Players", "`" + online + "`" + "/" + "`" + max + "`:video_game:", true);
                    embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
                    embedBuilder.setColor(0x8F00FF);

                    // Build the embed object
                    MessageEmbed embed = embedBuilder.build();

                    if (!(onlinePlayers == -1)) {
                        event.replyEmbeds(embed).queue();
                    } else {
                        event.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
                    }
                } else {
                    event.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
                }

            }
            case "setup" -> {
                if (event.getMember() != null &&  !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
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

                JsonObject settings = this.guildSettingsManager.loadGuildSettings(guildId);
                boolean enableMCCommand = true;

                this.guildSettingsManager.setMCCommandEnabled(guildId, enableMCCommand);
                this.guildSettingsManager.saveGuildSettings(guildId, settings);

                event.replyModal(setup).queue();
            }
            case "setupmc" -> {
                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    return;
                }
                true_false = enable_disable.getAsBoolean();

                boolean enableMCCommand = Boolean.parseBoolean(String.valueOf(true_false));

                this.guildSettingsManager.setMCCommandEnabled(guildId, enableMCCommand);
                event.reply("Done").setEphemeral(true).queue();
            }
            case "setupbedrockmc" -> {
                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    return;
                }
                true_false = enable_disable.getAsBoolean();

                boolean enableBedrockMC = Boolean.parseBoolean(String.valueOf(true_false));

                this.guildSettingsManager.setBedrockMcEnabled(guildId, enableBedrockMC);
                event.reply("Done").setEphemeral(true).queue();
            }
            case "setupremoveport" -> {
                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    return;
                }
                true_false = enable_disable.getAsBoolean();

                boolean removePortEnabled = Boolean.parseBoolean(String.valueOf(true_false));

                this.guildSettingsManager.setRemovePort(guildId, removePortEnabled);
                event.reply("Done").setEphemeral(true).queue();
            }
            case "settings" -> {
                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    return;
                }
                event.reply("test").queue();
            }
            case "terms-of-service" -> {
                event.reply("Done").setEphemeral(true).queue();
            }
            case "privacy-policy" -> {
                event.reply("Done1").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("setup")) {
            String guildId = Objects.requireNonNull(event.getGuild()).getId();
            String userInputname = Objects.requireNonNull(event.getValue("name")).getAsString();
            String userInputip = Objects.requireNonNull(event.getValue("option1")).getAsString();
            String userInputport = Objects.requireNonNull(event.getValue("option2")).getAsString();



            JsonObject settings = this.guildSettingsManager.loadGuildSettings(guildId);
            settings.addProperty("name", userInputname);
            settings.addProperty("option1", userInputip);
            settings.addProperty("option2", userInputport);
            this.guildSettingsManager.saveGuildSettings(guildId, settings);

            event.reply("✅ Server setup complete!\n" +
                    "Name: " + userInputname + "\n" +
                    "IP: " + userInputip + "\n" +
                    "Port: " + userInputport).setEphemeral(true).queue();
        }
    }

    public static String getServerStatusBedrock(String address) {
        try {
            URL url = new URL("https://api.mcsrvstat.us/bedrock/3/" + address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to retrieve server status: " + e.getMessage());
        }
        return null;
    }

    public static int extractPlayerCount(String serverData, String key) {
        int index = serverData.indexOf("\"" + key + "\":");
        if (index != -1) {
            int startIndex = serverData.indexOf(":", index) + 1;
            int endIndex = serverData.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = serverData.indexOf("}", startIndex);
            }
            String countStr = serverData.substring(startIndex, endIndex).trim();
            // Remove any non-numeric characters and whitespace
            countStr = countStr.replaceAll("\\D", "");
            // Check if the cleaned player count string is not empty
            if (!countStr.isEmpty()) {
                try {
                    return Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    // Do nothing, handle as unknown count
                }
            }
        }
        return -1;
    }


    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        commandData.add(Commands.slash("about", "Provides bot creator info and support Discord link."));

        OptionData option1 = new OptionData(OptionType.STRING, "ip", "Specify an IP address or domain.", true);
        OptionData option2 = new OptionData(OptionType.STRING, "port", "Specify a port number.",false);
        commandData.add(Commands.slash("mcstatus", "Effortlessly monitors the status and details of Java Minecraft servers.").addOptions(option1, option2));

        OptionData option3 = new OptionData(OptionType.STRING, "ip", "Specify an IP address or domain.", true);
        OptionData option4 = new OptionData(OptionType.STRING, "port", "Specify a port number.",false);
        commandData.add(Commands.slash("mcstatusbedrock", "Effortlessly monitors the status and details of Bedrock Minecraft servers.").addOptions(option3, option4));

        commandData.add(Commands.slash("setup", "Setting up server IP and port."));

        commandData.add(Commands.slash("tos", "Setting up server IP and port."));
        commandData.add(Commands.slash("privacy", "Setting up server IP and port."));


        OptionData option7 = new OptionData(OptionType.BOOLEAN, "enable", "Enable/Disable.", true);
        commandData.add(Commands.slash("setupmc", "Enabling/Disabling the mc command.").addOptions(option7));

        OptionData option8 = new OptionData(OptionType.BOOLEAN, "enable", "Enable/Disable.", true);
        commandData.add(Commands.slash("setupbedrockmc", "Enabling/Disabling the bedrock mc command.").addOptions(option8));

        OptionData option10 = new OptionData(OptionType.BOOLEAN, "enable", "Enable/Disable.", true);
        commandData.add(Commands.slash("setupremoveport", "It won't show port in mc command.").addOptions(option10));

        event.getJDA().updateCommands().addCommands(commandData).queue();
    }
}
