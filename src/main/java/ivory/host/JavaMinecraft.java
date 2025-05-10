package ivory.host;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.naming.directory.*;
import com.google.gson.*;
import java.util.Base64;

public class JavaMinecraft {

    // Host and port info.
    private String host;
    private int port;

    // Retrieved data fields.
    private boolean retrieved = false;
    private BufferedImage motdImage;
    private String onlinePlayer;
    private String maxPlayer;
    private BufferedImage serverIcon;

    // Set some default dimensions. You can adjust these as needed.
    private final int imageWidth = 766;
    private final int imageHeight = 110;

    // Private constructor.
    private JavaMinecraft(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // --- Static builder methods ---
    public static JavaMinecraft generate(String host) {
        return new JavaMinecraft(host, 25565);
    }

    public static JavaMinecraft onlineplayer(String host) {
        return new JavaMinecraft(host, 25565);
    }

    public static JavaMinecraft maxplayer(String host) {
        return new JavaMinecraft(host, 25565);
    }

    public static JavaMinecraft icon(String host) {
        return new JavaMinecraft(host, 25565);
    }

    // Set the port.
    public JavaMinecraft port(int port) {
        this.port = port;
        return this;
    }

    // Get the generated MOTD image.
    public BufferedImage motd() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return motdImage;
    }

    // Get online players as a string.
    public String getOnlinePlayer() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return onlinePlayer;
    }

    // Get max players as a string.
    public String getMaxPlayer() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return maxPlayer;
    }

    // Get the server icon image.
    public BufferedImage getIcon() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return serverIcon;
    }

    // --- Internal data retrieval method ---
    private void retrieveData() throws IOException {
        // Try a list of protocol versions.
        int timeout = 5000;
        int[] protocolVersions = {769, 755, 754, 751, 736, 735, 573, 498, 477, 401, 393, 340, 338, 315, 210, 110, 47};
        String motdJson = null;
        int usedVersion = -1;
        for (int version : protocolVersions) {
            try {
                motdJson = ping(host, port, version, timeout);
                if (motdJson != null && !motdJson.isEmpty()) {
                    usedVersion = version;
                    break;
                }
            } catch (IOException e) {
                // Try next version.
            }
        }
        if (motdJson == null || motdJson.isEmpty()) {
            throw new IOException("Failed to retrieve MOTD from server");
        }

        // Parse JSON using Gson.
        Gson gson = new Gson();
        JsonObject jsonObj = gson.fromJson(motdJson, JsonObject.class);

        // Extract online and max players.
        if (jsonObj.has("players")) {
            JsonObject playersObj = jsonObj.getAsJsonObject("players");
            int online = playersObj.get("online").getAsInt();
            int max = playersObj.get("max").getAsInt();
            onlinePlayer = String.valueOf(online);
            maxPlayer = String.valueOf(max);
        }

        // Extract server icon.
        serverIcon = getServerIconFromJson(jsonObj);

        // Parse MOTD description.
        JsonElement description = jsonObj.get("description");
        List<TextSegment> segments = parseTextSegments(description, new TextFormat());

        // Use the online player count as text for the top-right (online/max)
        String playerCountText = (onlinePlayer != null && maxPlayer != null) ? (onlinePlayer + "/" + maxPlayer) : "";

        // Generate the MOTD image.
        motdImage = renderMotd(segments, serverIcon, imageWidth, imageHeight, playerCountText);

        retrieved = true;
    }

    // --- Methods from previous implementation ---

    private static TexturePaint getDirtTexturePaint(int width, int height) {
        BufferedImage dirtImage = null;
        try {
            URL url = new URL("https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/03/Dirt_background_BE2.png/revision/latest/scale-to-width-down/1000?cb=20210820065338");
            dirtImage = ImageIO.read(url);
        } catch (IOException e) {
            System.out.println("Failed to load dirt texture from URL, using fallback.");
        }
        if (dirtImage == null) {
            dirtImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = dirtImage.createGraphics();
            g2.setColor(new Color(101, 67, 33));
            g2.fillRect(0, 0, 64, 64);
            g2.dispose();
        }
        // Darken the texture.
        BufferedImage darkerImage = new BufferedImage(dirtImage.getWidth(), dirtImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = darkerImage.createGraphics();
        g2.drawImage(dirtImage, 0, 0, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, dirtImage.getWidth(), dirtImage.getHeight());
        g2.dispose();
        Rectangle rect = new Rectangle(0, 0, darkerImage.getWidth(), darkerImage.getHeight());
        return new TexturePaint(darkerImage, rect);
    }

    private static BufferedImage getServerIconFromJson(JsonObject jsonObj) {
        if (jsonObj.has("favicon")) {
            String favicon = jsonObj.get("favicon").getAsString();
            if (favicon != null && !favicon.isEmpty()) {
                String prefix = "data:image/png;base64,";
                if (favicon.startsWith(prefix)) {
                    favicon = favicon.substring(prefix.length());
                }
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(favicon);
                    return ImageIO.read(new ByteArrayInputStream(imageBytes));
                } catch (Exception e) {
                    System.out.println("Error decoding server icon: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private static Font getMinecraftFont(float size) {
        File fontFile = new File("Minecraft.ttf");
        if (!fontFile.exists()) {
            System.out.println("Minecraft font not found locally. Attempting to download and extract...");
            try {
                String zipUrl = "https://dl.dafont.com/dl/?f=minecraft";
                downloadAndExtractFont(zipUrl, fontFile);
                System.out.println("Minecraft font downloaded and extracted successfully.");
            } catch (Exception e) {
                System.out.println("Failed to download/extract Minecraft font: " + e.getMessage());
            }
        }
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            return font.deriveFont(size);
        } catch (Exception e) {
            System.out.println("Could not load Minecraft font, falling back.");
            return new Font("Monospaced", Font.PLAIN, (int) size);
        }
    }

    private static void downloadAndExtractFont(String urlStr, File destination) throws IOException {
        URL url = new URL(urlStr);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream in = url.openStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        }
        byte[] zipBytes = baos.toByteArray();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            boolean found = false;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".ttf")) {
                    try (FileOutputStream fos = new FileOutputStream(destination)) {
                        byte[] zipBuffer = new byte[4096];
                        int len;
                        while ((len = zis.read(zipBuffer)) != -1) {
                            fos.write(zipBuffer, 0, len);
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("No TTF file found in the zip archive.");
            }
        }
    }

    private static String ping(String host, int port, int protocolVersion, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        socket.setSoTimeout(timeout);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(handshakeBytes);
        writeVarInt(handshake, 0x00);
        writeVarInt(handshake, protocolVersion);
        writeString(handshake, host);
        handshake.writeShort(port);
        writeVarInt(handshake, 1);
        byte[] handshakePacket = handshakeBytes.toByteArray();

        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        DataOutputStream packetOut = new DataOutputStream(packet);
        writeVarInt(packetOut, handshakePacket.length);
        packetOut.write(handshakePacket);
        out.write(packet.toByteArray());

        ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
        DataOutputStream request = new DataOutputStream(requestBytes);
        writeVarInt(request, 0x00);
        byte[] requestPacket = requestBytes.toByteArray();
        ByteArrayOutputStream requestPacketWithLength = new ByteArrayOutputStream();
        DataOutputStream requestOut = new DataOutputStream(requestPacketWithLength);
        writeVarInt(requestOut, requestPacket.length);
        requestOut.write(requestPacket);
        out.write(requestPacketWithLength.toByteArray());

        int length = readVarInt(in);
        if (length < 10)
            throw new IOException("Invalid packet length: " + length);
        byte[] responseData = new byte[length];
        in.readFully(responseData);
        DataInputStream responseStream = new DataInputStream(new ByteArrayInputStream(responseData));
        int packetId = readVarInt(responseStream);
        if (packetId != 0x00)
            throw new IOException("Invalid packet ID: " + packetId);
        int jsonLength = readVarInt(responseStream);
        if (jsonLength < 0)
            throw new IOException("Invalid JSON length: " + jsonLength);
        byte[] jsonData = new byte[jsonLength];
        responseStream.readFully(jsonData);
        socket.close();
        return new String(jsonData, StandardCharsets.UTF_8);
    }

    private static String[] resolveSRV(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_minecraft._tcp." + domain, new String[]{"SRV"});
            Attribute attr = attrs.get("SRV");
            if (attr == null)
                return null;
            String srvRecord = (String) attr.get(0);
            String[] parts = srvRecord.split(" ");
            if (parts.length >= 4) {
                String port = parts[2];
                String target = parts[3];
                if (target.endsWith("."))
                    target = target.substring(0, target.length() - 1);
                return new String[]{ target, port };
            }
        } catch (Exception e) {
            System.out.println("Error resolving SRV: " + e.getMessage());
        }
        return null;
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            result |= (read & 0x7F) << (7 * numRead);
            numRead++;
            if (numRead > 5)
                throw new IOException("VarInt is too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, data.length);
        out.write(data);
    }

    // --- Text Segment and Parsing Methods ---
    static class TextSegment {
        String text;
        Color color;
        boolean bold;
        boolean italic;
        public TextSegment(String text, Color color, boolean bold, boolean italic) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
        }
    }

    static class TextFormat {
        Color color;
        boolean bold;
        boolean italic;
        public TextFormat() {
            color = Color.WHITE;
            bold = false;
            italic = false;
        }
        public TextFormat(TextFormat other) {
            this.color = other.color;
            this.bold = other.bold;
            this.italic = other.italic;
        }
    }

    static Map<String, Color> mcColorMap = new HashMap<>();
    static {
        mcColorMap.put("black", Color.decode("#000000"));
        mcColorMap.put("dark_blue", Color.decode("#0000AA"));
        mcColorMap.put("dark_green", Color.decode("#00AA00"));
        mcColorMap.put("dark_aqua", Color.decode("#00AAAA"));
        mcColorMap.put("dark_red", Color.decode("#AA0000"));
        mcColorMap.put("dark_purple", Color.decode("#AA00AA"));
        mcColorMap.put("gold", Color.decode("#FFAA00"));
        mcColorMap.put("gray", Color.decode("#AAAAAA"));
        mcColorMap.put("dark_gray", Color.decode("#555555"));
        mcColorMap.put("blue", Color.decode("#5555FF"));
        mcColorMap.put("green", Color.decode("#55FF55"));
        mcColorMap.put("aqua", Color.decode("#55FFFF"));
        mcColorMap.put("red", Color.decode("#FF5555"));
        mcColorMap.put("light_purple", Color.decode("#FF55FF"));
        mcColorMap.put("yellow", Color.decode("#FFFF55"));
        mcColorMap.put("white", Color.decode("#FFFFFF"));
    }

    static List<TextSegment> parseTextSegments(JsonElement element, TextFormat inherited) {
        List<TextSegment> segments = new ArrayList<>();
        if (element == null)
            return segments;
        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            if (text.contains("§")) {
                segments.addAll(parseLegacy(text));
            } else {
                segments.add(new TextSegment(text, inherited.color, inherited.bold, inherited.italic));
            }
        } else if (element.isJsonObject()) {
            TextFormat current = new TextFormat(inherited);
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("color")) {
                String colorStr = obj.get("color").getAsString();
                if (colorStr.startsWith("#")) {
                    try {
                        current.color = Color.decode(colorStr);
                    } catch (Exception e) {
                        current.color = Color.WHITE;
                    }
                } else {
                    current.color = mcColorMap.getOrDefault(colorStr, Color.WHITE);
                }
            }
            if (obj.has("bold"))
                current.bold = obj.get("bold").getAsBoolean();
            if (obj.has("italic"))
                current.italic = obj.get("italic").getAsBoolean();
            String text = "";
            if (obj.has("text"))
                text = obj.get("text").getAsString();
            if (!text.isEmpty()) {
                if (text.contains("§"))
                    segments.addAll(parseLegacy(text));
                else
                    segments.add(new TextSegment(text, current.color, current.bold, current.italic));
            }
            if (obj.has("extra")) {
                JsonArray extras = obj.getAsJsonArray("extra");
                for (JsonElement extra : extras) {
                    segments.addAll(parseTextSegments(extra, current));
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                segments.addAll(parseTextSegments(el, inherited));
            }
        }
        return segments;
    }

    static List<TextSegment> parseLegacy(String legacyText) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        Color currentColor = Color.WHITE;
        boolean currentBold = false;
        boolean currentItalic = false;
        for (int i = 0; i < legacyText.length(); i++) {
            char c = legacyText.charAt(i);
            if (c == '§' && i + 1 < legacyText.length()) {
                if (currentText.length() > 0) {
                    segments.add(new TextSegment(currentText.toString(), currentColor, currentBold, currentItalic));
                    currentText.setLength(0);
                }
                char code = Character.toLowerCase(legacyText.charAt(i + 1));
                i++;
                if (code == 'r') {
                    currentColor = Color.WHITE;
                    currentBold = false;
                    currentItalic = false;
                } else if (code == 'l') {
                    currentBold = true;
                } else if (code == 'o') {
                    currentItalic = true;
                } else if (code == 'n' || code == 'm' || code == 'k') {
                    // Ignored.
                } else {
                    switch (code) {
                        case '0': currentColor = mcColorMap.get("black"); break;
                        case '1': currentColor = mcColorMap.get("dark_blue"); break;
                        case '2': currentColor = mcColorMap.get("dark_green"); break;
                        case '3': currentColor = mcColorMap.get("dark_aqua"); break;
                        case '4': currentColor = mcColorMap.get("dark_red"); break;
                        case '5': currentColor = mcColorMap.get("dark_purple"); break;
                        case '6': currentColor = mcColorMap.get("gold"); break;
                        case '7': currentColor = mcColorMap.get("gray"); break;
                        case '8': currentColor = mcColorMap.get("dark_gray"); break;
                        case '9': currentColor = mcColorMap.get("blue"); break;
                        case 'a': currentColor = mcColorMap.get("green"); break;
                        case 'b': currentColor = mcColorMap.get("aqua"); break;
                        case 'c': currentColor = mcColorMap.get("red"); break;
                        case 'd': currentColor = mcColorMap.get("light_purple"); break;
                        case 'e': currentColor = mcColorMap.get("yellow"); break;
                        case 'f': currentColor = mcColorMap.get("white"); break;
                        default: break;
                    }
                    currentBold = false;
                    currentItalic = false;
                }
            } else {
                currentText.append(c);
            }
        }
        if (currentText.length() > 0)
            segments.add(new TextSegment(currentText.toString(), currentColor, currentBold, currentItalic));
        return segments;
    }

    static BufferedImage renderMotd(List<TextSegment> segments, BufferedImage serverIcon, int width, int height, String playerCountText) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Fill the background with a darkened dirt texture.
        TexturePaint dirtPaint = getDirtTexturePaint(width, height);
        g.setPaint(dirtPaint);
        g.fillRect(0, 0, width, height);

        // Draw the server icon on the left.
        int iconSize = 100;  // Increased icon size
        int iconMargin = 10;
        int textStartX = iconMargin;
        if (serverIcon != null) {
            Image scaledIcon = serverIcon.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            int iconY = (height - iconSize) / 2;
            g.drawImage(scaledIcon, iconMargin, iconY, null);
            textStartX = iconMargin + iconSize + iconMargin;
        }

        // Draw the player count (plain, non-bold) in the top-right.
        int countX = 0, countY = 0, countTotalWidth = 0;
        Font countFont = getMinecraftFont(24f).deriveFont(Font.PLAIN, 24f);
        g.setFont(countFont);
        FontMetrics fm = g.getFontMetrics();
        if (playerCountText != null && !playerCountText.isEmpty()) {
            int slashIndex = playerCountText.indexOf('/');
            if (slashIndex == -1) {
                countTotalWidth = fm.stringWidth(playerCountText);
                countX = width - countTotalWidth - 45;
                countY = fm.getAscent() + 10;
                g.setColor(Color.GRAY);
                g.drawString(playerCountText, countX, countY);
            } else {
                String onlinePart = playerCountText.substring(0, slashIndex);
                String slashPart = "/";
                String maxPart = playerCountText.substring(slashIndex + 1);
                int widthOnline = fm.stringWidth(onlinePart);
                int widthSlash = fm.stringWidth(slashPart);
                int widthMax = fm.stringWidth(maxPart);
                countTotalWidth = widthOnline + widthSlash + widthMax;
                countX = width - countTotalWidth - 45;
                countY = fm.getAscent() + 10;
                g.setColor(Color.GRAY);
                g.drawString(onlinePart, countX, countY);
                g.setColor(Color.DARK_GRAY);
                g.drawString(slashPart, countX + widthOnline, countY);
                g.setColor(Color.GRAY);
                g.drawString(maxPart, countX + widthOnline + widthSlash, countY);
            }
        }

        // Calculate the ping indicator position (aligned with the player count baseline).
        int pingMarginRight = 10;
        int numBars = 5;
        int barWidth = 4;
        int gap = 2;
        int[] barHeights = {4, 8, 12, 16, 20};
        int totalBarWidth = numBars * barWidth + (numBars - 1) * gap;
        int pingIndicatorX = width - totalBarWidth - pingMarginRight;
        int pingIndicatorBaseY = countY;
        drawStaticPingIndicator(g, pingIndicatorX, pingIndicatorBaseY);

        // Render the MOTD text using a 28 pt font starting at y = 60.
        int motdStartY = 60;
        Font baseMinecraftFont = getMinecraftFont(28f);
        Font fallbackFont = new Font("Monospaced", Font.PLAIN, 28);
        int marginX = textStartX;
        int x = marginX;
        int y = motdStartY;
        for (TextSegment seg : segments) {
            int style = Font.PLAIN;
            if (seg.bold)
                style |= Font.BOLD;
            if (seg.italic)
                style |= Font.ITALIC;
            Font segMinecraftFont = baseMinecraftFont.deriveFont(style, 25);
            Font segFallbackFont = fallbackFont.deriveFont(style, 25);
            g.setColor(seg.color);
            String[] lines = seg.text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                x = drawStringWithFallback(g, line, x, y, segMinecraftFont, segFallbackFont);
                if (i < lines.length - 1) {
                    y += g.getFontMetrics().getHeight();
                    x = marginX;
                }
            }
        }

        g.dispose();
        return img;
    }

    private static void drawStaticPingIndicator(Graphics2D g, int startX, int baseY) {
        int numBars = 5;
        int barWidth = 4;
        int gap = 2;
        int[] barHeights = {4, 8, 12, 16, 20};
        Color pingColor = Color.decode("#00ff21");
        Color shadowColor = Color.decode("#00870f");
        int shadowOffset = 3;  // Increased shadow offset to make the shadow a bit bigger.
        for (int i = 0; i < numBars; i++) {
            int x = startX + i * (barWidth + gap);
            int barHeight = barHeights[i];
            int y = baseY - barHeight;
            // Draw drop shadow
            g.setColor(shadowColor);
            g.fillRect(x + shadowOffset, y + shadowOffset, barWidth, barHeight);
            // Draw the ping bar
            g.setColor(pingColor);
            g.fillRect(x, y, barWidth, barHeight);
        }
    }

    private static int drawStringWithFallback(Graphics2D g, String text, int x, int y, Font minecraftFont, Font fallbackFont) {
        int start = 0;
        int currentX = x;
        int length = text.length();
        while (start < length) {
            boolean useMinecraft = minecraftFont.canDisplay(text.charAt(start));
            int end = start + 1;
            while (end < length && minecraftFont.canDisplay(text.charAt(end)) == useMinecraft) {
                end++;
            }
            String substring = text.substring(start, end);
            Font fontToUse = useMinecraft ? minecraftFont : fallbackFont;
            g.setFont(fontToUse);
            g.drawString(substring, currentX, y);
            currentX += g.getFontMetrics().stringWidth(substring);
            start = end;
        }
        return currentX;
    }

    private static String getMinecraftColorCode(Color color) {
        if (color.equals(mcColorMap.get("black"))) return "&0";
        else if (color.equals(mcColorMap.get("dark_blue"))) return "&1";
        else if (color.equals(mcColorMap.get("dark_green"))) return "&2";
        else if (color.equals(mcColorMap.get("dark_aqua"))) return "&3";
        else if (color.equals(mcColorMap.get("dark_red"))) return "&4";
        else if (color.equals(mcColorMap.get("dark_purple"))) return "&5";
        else if (color.equals(mcColorMap.get("gold"))) return "&6";
        else if (color.equals(mcColorMap.get("gray"))) return "&7";
        else if (color.equals(mcColorMap.get("dark_gray"))) return "&8";
        else if (color.equals(mcColorMap.get("blue"))) return "&9";
        else if (color.equals(mcColorMap.get("green"))) return "&a";
        else if (color.equals(mcColorMap.get("aqua"))) return "&b";
        else if (color.equals(mcColorMap.get("red"))) return "&c";
        else if (color.equals(mcColorMap.get("light_purple"))) return "&d";
        else if (color.equals(mcColorMap.get("yellow"))) return "&e";
        else if (color.equals(mcColorMap.get("white"))) return "&f";
        else {
            return "&#" + String.format("%06x", (color.getRGB() & 0xFFFFFF));
        }
    }

    private static String getMinecraftFormatCode(TextSegment seg) {
        StringBuilder sb = new StringBuilder();
        sb.append(getMinecraftColorCode(seg.color));
        if (seg.bold)
            sb.append("&l");
        if (seg.italic)
            sb.append("&o");
        return sb.toString();
    }
}

