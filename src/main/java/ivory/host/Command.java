package ivory.host;

import com.google.gson.JsonObject;
import ivory.host.config.GuildSettingsManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

        if (args[0].equalsIgnoreCase("mc")) {

            boolean isEnabled = this.guildSettingsManager.isMCCommandEnabled(event.getGuild().getId());
            boolean isBedrockEnabled = this.guildSettingsManager.isBedrockMcEnabled(event.getGuild().getId());

            String option1;
            String option2;
            String firstInput;
            String secondInput;
            String finalOutput;
            String serverData;
            int onlinePlayers;
            int maxPlayers;
            String online;
            String max;

            if (isEnabled) {
                String guildId = event.getGuild().getId();
                JsonObject settings = this.guildSettingsManager.loadGuildSettings(guildId);
                if (!isBedrockEnabled) {
                    if (settings.has("option1") && settings.has("option2")) {
                        option1 = settings.get("option1").getAsString();
                        option2 = settings.get("option2").getAsString();

                        firstInput = option1.trim();
                        secondInput = option2.trim();
                        finalOutput = secondInput.isEmpty() ? firstInput : firstInput + ":" + secondInput;
                        serverData = getServerStatus(finalOutput);

                        if (serverData != null) {

                            onlinePlayers = extractPlayerCount(serverData, "online");
                            maxPlayers = extractPlayerCount(serverData, "max");

                            online = Integer.toString(onlinePlayers);
                            max = Integer.toString(maxPlayers);

                            EmbedBuilder embedBuilder = new EmbedBuilder();

                            embedBuilder.setTitle(option1);
                            embedBuilder.setThumbnail("https://api.mcstatus.io/v2/icon/" + option1 + ":" + option2);
                            embedBuilder.addField("Server IP", "`" + option1 + ":" + option2 + "`", true);
                            embedBuilder.addField("Online Players", "`" + online + "`/`" + max + "`:video_game:", true);
                            embedBuilder.setImage("https://api.loohpjames.com/serverbanner.png?ip=" + option1 + "&port=" + option2 + "&name=" + option1);
                            embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
                            embedBuilder.setColor(9371903);

                            MessageEmbed embed = embedBuilder.build();

                            if (!(onlinePlayers == -1)) {
                                event.getChannel().sendMessage("").setEmbeds(embed).queue();
                            } else {
                                event.getChannel().sendMessage("The server is currently offline or the address provided is invalid.").queue();
                            }
                        } else {
                            event.getChannel().sendMessage("The server is currently offline or the address provided is invalid.").queue();
                        }
                    } else {
                        event.getChannel().sendMessage("No settings found for this server.").queue();
                    }
                } else if (settings.has("option1") && settings.has("option2")) {
                    option1 = settings.get("option1").getAsString();
                    option2 = settings.get("option2").getAsString();
                    firstInput = option1.trim();
                    secondInput = option2.trim();
                    finalOutput = secondInput.isEmpty() ? firstInput : firstInput + ":" + secondInput;
                    serverData = getServerStatusBedrock(finalOutput);
                    if (serverData != null) {

                        onlinePlayers = extractPlayerCount(serverData, "online");
                        maxPlayers = extractPlayerCount(serverData, "max");
                        online = Integer.toString(onlinePlayers);
                        max = Integer.toString(maxPlayers);

                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setTitle(option1);
                        embedBuilder.addField("Server IP", "`" + option1 + ":" + option2 + "`", true);
                        embedBuilder.addField("Online Players", "`" + online + "`" + "/" + "`" + max + "`:video_game:", true);
                        embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
                        embedBuilder.setColor(0x8F00FF);

                        // Build the embed object
                        MessageEmbed embed = embedBuilder.build();

                        if (!(onlinePlayers == -1)) {
                            event.getChannel().sendMessage("").setEmbeds(embed).queue();
                        } else {
                            event.getChannel().sendMessage("The server is currently offline or the address provided is invalid.").queue();
                        }
                    } else {
                        event.getChannel().sendMessage("The server is currently offline or the address provided is invalid.").queue();
                    }
                } else {
                    event.getChannel().sendMessage("No settings found for this server.").queue();
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String command = event.getName();
        switch (command) {
            case "about" ->
                    event.reply("This bot was created by 5MS_.\nFor support, join our Discord server: https://discord.gg/nr4JYefgP3")
                            .setEphemeral(true)
                            .queue();
            case "mcstatus" -> {
                OptionMapping messageOption = event.getOption("ip");
                OptionMapping messageOption1 = event.getOption("port");

                assert messageOption != null;
                String serverip = messageOption.getAsString();
                String serverport = (messageOption1 != null) ? messageOption1.getAsString() : "";

                String firstInput = serverip.trim();
                String secondInput = serverport.trim();
                String finalOutput = secondInput.isEmpty() ? firstInput : firstInput + ":" + secondInput;

                String serverData = getServerStatus(finalOutput);
                if (serverData != null) {
                    int port = extractPlayerCount(serverData, "port");
                    int onlinePlayers = extractPlayerCount(serverData, "online");
                    int maxPlayers = extractPlayerCount(serverData, "max");

                    String online = Integer.toString(onlinePlayers);
                    String max = Integer.toString(maxPlayers);

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle(serverip);
                    embedBuilder.setThumbnail("https://api.mcstatus.io/v2/icon/" + serverip + ":" + port);
                    embedBuilder.addField("Server IP", "`" + serverip + ":" + port + "`", true);
                    embedBuilder.addField("Online Players", "`" + online + "`" + "/" + "`" + max + "`:video_game:", true);
                    embedBuilder.setImage("https://api.loohpjames.com/serverbanner.png?ip=" + serverip + "&port=" + port + "&name=" + serverip);
                    embedBuilder.setFooter("Ivory Host", "https://cdn.discordapp.com/avatars/1250796754983452792/17240db181de64e1b6fbf13e27b89d98.webp?size=160");
                    embedBuilder.setColor(0x8F00FF);

                    // Build the embed object
                    MessageEmbed embed = embedBuilder.build();

                    // Send the embed message as a response to the slash command
                    if (!(onlinePlayers == -1)) {
                        event.replyEmbeds(embed).queue();
                    } else {
                        event.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
                    }
                } else {
                    event.reply("The server is currently offline or the address provided is invalid.").setEphemeral(true).queue();
                }
            }
            case "mcstatusbedrock" -> {
                OptionMapping messageOption = event.getOption("ip");
                OptionMapping messageOption1 = event.getOption("port");

                assert messageOption != null;
                String serverip = messageOption.getAsString();
                String serverport = (messageOption1 != null) ? messageOption1.getAsString() : "";

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
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
                    return;
                }

                OptionMapping messageOption = event.getOption("ip");
                OptionMapping messageOption1 = event.getOption("port");

                assert messageOption != null;

                String option1 = messageOption.getAsString();
                String option2 = messageOption1.getAsString();
                String guildId = event.getGuild().getId();

                JsonObject settings = this.guildSettingsManager.loadGuildSettings(guildId);
                settings.addProperty("option1", option1);
                settings.addProperty("option2", option2);
                this.guildSettingsManager.saveGuildSettings(guildId, settings);

                event.reply("Done").setEphemeral(true).queue();
            }
            case "setupmc" -> {

                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
                    return;
                }

                OptionMapping messageOption = event.getOption("enable");
                Boolean option1 = messageOption.getAsBoolean();
                boolean enableMCCommand = Boolean.parseBoolean(String.valueOf(option1));

                this.guildSettingsManager.setMCCommandEnabled(event.getGuild().getId(), enableMCCommand);
                event.reply("Done").setEphemeral(true).queue();

            }
            case "setupbedrockmc" -> {

                if (event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.getChannel().sendMessage("You don't have permission to use this command.").queue();
                    return;
                }

                OptionMapping messageOption = event.getOption("enable");
                Boolean option1 = messageOption.getAsBoolean();
                boolean enableBedrockMC = Boolean.parseBoolean(String.valueOf(option1));

                this.guildSettingsManager.setBedrockMcEnabled(event.getGuild().getId(), enableBedrockMC);
                event.reply("Done").setEphemeral(true).queue();
            }
        }
    }


    public static String getServerStatus(String address) {
        try {
            URL url = new URL("https://api.mcsrvstat.us/3/" + address);
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

        OptionData option5 = new OptionData(OptionType.STRING, "ip", "Specify an IP address or domain.", true);
        OptionData option6 = new OptionData(OptionType.STRING, "port", "Specify a port number.", true);
        commandData.add(Commands.slash("setup", "Setting up server IP and port.").addOptions(option5, option6));

        OptionData option7 = new OptionData(OptionType.BOOLEAN, "enable", "Enable/Disable.", true);
        commandData.add(Commands.slash("setupmc", "Enabling/Disabling the mc command.").addOptions(option7));

        OptionData option8 = new OptionData(OptionType.BOOLEAN, "enable", "Enable/Disable.", true);
        commandData.add(Commands.slash("setupbedrockmc", "Enabling/Disabling the bedrock mc command.").addOptions(option8));




        event.getJDA().updateCommands().addCommands(commandData).queue();
    }
}
