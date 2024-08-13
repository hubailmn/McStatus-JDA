package ivory.host.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



public class GuildSettingsManager {

    private final File file;

    public GuildSettingsManager() {
        this.file = new File("serversettings.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write("{ \"guilds\": {} }"); // Initialize the file with an empty JSON structure
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public JsonObject loadGuildSettings(String guildId) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("guilds")) {
                json.add("guilds", new JsonObject());
            }
            JsonObject guilds = json.getAsJsonObject("guilds");
            if (!guilds.has(guildId)) {
                guilds.add(guildId, new JsonObject());
            }
            return guilds.getAsJsonObject(guildId);
        } catch (IOException e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public void saveGuildSettings(String guildId, JsonObject settings) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject guilds = json.getAsJsonObject("guilds");
            guilds.add(guildId, settings);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // New Methods for handling mcCommandEnabled
    public void setMCCommandEnabled(String guildId, boolean enabled) {
        JsonObject settings = loadGuildSettings(guildId);
        settings.addProperty("mcCommandEnabled", enabled);
        saveGuildSettings(guildId, settings);
    }

    public boolean isMCCommandEnabled(String guildId) {
        JsonObject settings = loadGuildSettings(guildId);
        return settings.has("mcCommandEnabled") && settings.get("mcCommandEnabled").getAsBoolean();
    }

    public void setBedrockMcEnabled(String guildId, boolean enabled) {
        JsonObject settings = loadGuildSettings(guildId);
        settings.addProperty("bedrockMcEnabled", enabled);
        saveGuildSettings(guildId, settings);
    }

    public boolean isBedrockMcEnabled(String guildId) {
        JsonObject settings = loadGuildSettings(guildId);
        return settings.has("bedrockMcEnabled") && settings.get("bedrockMcEnabled").getAsBoolean();
    }
}