package ivory.host.util.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class McEmbedBuilder {
    private static final Map<String, Color> MC_COLOR_MAP = new ConcurrentHashMap<>();
    private static final int DEFAULT_IMAGE_WIDTH = 766;
    private static final int DEFAULT_IMAGE_HEIGHT = 110;
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final Map<String, String[]> SRV_CACHE = new ConcurrentHashMap<>();
    private static TexturePaint DIRT_TEXTURE_PAINT = null;
    private static Font MINECRAFT_FONT = null;

    static {
        MC_COLOR_MAP.put("black", Color.decode("#000000"));
        MC_COLOR_MAP.put("dark_blue", Color.decode("#0000AA"));
        MC_COLOR_MAP.put("dark_green", Color.decode("#00AA00"));
        MC_COLOR_MAP.put("dark_aqua", Color.decode("#00AAAA"));
        MC_COLOR_MAP.put("dark_red", Color.decode("#AA0000"));
        MC_COLOR_MAP.put("dark_purple", Color.decode("#AA00AA"));
        MC_COLOR_MAP.put("gold", Color.decode("#FFAA00"));
        MC_COLOR_MAP.put("gray", Color.decode("#AAAAAA"));
        MC_COLOR_MAP.put("dark_gray", Color.decode("#555555"));
        MC_COLOR_MAP.put("blue", Color.decode("#5555FF"));
        MC_COLOR_MAP.put("green", Color.decode("#55FF55"));
        MC_COLOR_MAP.put("aqua", Color.decode("#55FFFF"));
        MC_COLOR_MAP.put("red", Color.decode("#FF5555"));
        MC_COLOR_MAP.put("light_purple", Color.decode("#FF55FF"));
        MC_COLOR_MAP.put("yellow", Color.decode("#FFFF55"));
        MC_COLOR_MAP.put("white", Color.decode("#FFFFFF"));
    }

    private final String host;
    private final int port;
    private boolean retrieved = false;
    private BufferedImage motdImage;
    private String onlinePlayer;
    private String maxPlayer;
    private BufferedImage serverIcon;

    public McEmbedBuilder(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private static String[] resolveSRV(String domain) {
        if (SRV_CACHE.containsKey(domain)) {
            return SRV_CACHE.get(domain);
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_minecraft._tcp." + domain, new String[]{"SRV"});
            Attribute attr = attrs.get("SRV");
            if (attr == null) return null;

            String srvRecord = (String) attr.get(0);
            String[] parts = srvRecord.split(" ");
            if (parts.length >= 4) {
                String port = parts[2];
                String target = parts[3];
                if (target.endsWith(".")) {
                    target = target.substring(0, target.length() - 1);
                }
                String[] result = new String[]{target, port};
                SRV_CACHE.put(domain, result);
                return result;
            }
        } catch (Exception e) {
            System.out.println("Error resolving SRV: " + e.getMessage());
        }
        return null;
    }

    private static String ping(Socket socket, int protocolVersion) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(handshakeBytes);
        writeVarInt(handshake, 0x00);
        writeVarInt(handshake, protocolVersion);
        writeString(handshake, socket.getInetAddress().getHostName());
        handshake.writeShort(socket.getPort());
        writeVarInt(handshake, 1);

        writeVarInt(out, handshakeBytes.size());
        out.write(handshakeBytes.toByteArray());

        writeVarInt(out, 1);
        out.writeByte(0x00);

        int length = readVarInt(in);
        if (length < 10) return null;

        byte[] responseData = new byte[length];
        in.readFully(responseData);

        DataInputStream responseStream = new DataInputStream(new ByteArrayInputStream(responseData));
        if (readVarInt(responseStream) != 0x00) return null;

        int jsonLength = readVarInt(responseStream);
        if (jsonLength < 0) return null;

        byte[] jsonData = new byte[jsonLength];
        responseStream.readFully(jsonData);

        return new String(jsonData, StandardCharsets.UTF_8);
    }

    private static BufferedImage getServerIconFromJson(JsonObject jsonObj) {
        if (!jsonObj.has("favicon")) return null;

        String favicon = jsonObj.get("favicon").getAsString();
        if (favicon == null || favicon.isEmpty()) return null;

        String prefix = "data:image/png;base64,";
        if (favicon.startsWith(prefix)) {
            favicon = favicon.substring(prefix.length());
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(favicon);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            System.out.println("Error decoding server icon: " + e.getMessage());
            return null;
        }
    }

    private static TexturePaint getDirtTexturePaint() {
        if (DIRT_TEXTURE_PAINT != null) {
            return DIRT_TEXTURE_PAINT;
        }

        synchronized (McEmbedBuilder.class) {
            if (DIRT_TEXTURE_PAINT != null) {
                return DIRT_TEXTURE_PAINT;
            }

            BufferedImage dirtImage = null;
            try {
                InputStream is = McEmbedBuilder.class.getResourceAsStream("/dirt_background.png");
                if (is != null) {
                    dirtImage = ImageIO.read(is);
                } else {
                    URL url = new URL("https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/03/Dirt_background_BE2.png/revision/latest/scale-to-width-down/1000?cb=20210820065338");
                    dirtImage = ImageIO.read(url);
                }
            } catch (IOException e) {
                System.out.println("Failed to load dirt texture, using fallback.");
            }

            if (dirtImage == null) {
                dirtImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = dirtImage.createGraphics();
                g2.setColor(new Color(101, 67, 33));
                g2.fillRect(0, 0, 64, 64);
                g2.dispose();
            }

            BufferedImage darkerImage = new BufferedImage(dirtImage.getWidth(), dirtImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = darkerImage.createGraphics();
            g2.drawImage(dirtImage, 0, 0, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, dirtImage.getWidth(), dirtImage.getHeight());
            g2.dispose();

            DIRT_TEXTURE_PAINT = new TexturePaint(darkerImage, new Rectangle(0, 0, darkerImage.getWidth(), darkerImage.getHeight()));
            return DIRT_TEXTURE_PAINT;
        }
    }

    private static Font getMinecraftFont(float size) {
        if (MINECRAFT_FONT != null) {
            return MINECRAFT_FONT.deriveFont(size);
        }

        synchronized (McEmbedBuilder.class) {
            if (MINECRAFT_FONT != null) {
                return MINECRAFT_FONT.deriveFont(size);
            }

            try {
                InputStream is = McEmbedBuilder.class.getResourceAsStream("/Minecraft.ttf");
                if (is != null) {
                    MINECRAFT_FONT = Font.createFont(Font.TRUETYPE_FONT, is);
                } else {
                    File fontFile = new File("Minecraft.ttf");
                    if (!fontFile.exists()) {
                        downloadFont("https://assets.minecraft.net/font/Minecraft.ttf", fontFile);
                    }
                    MINECRAFT_FONT = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                }
                return MINECRAFT_FONT.deriveFont(size);
            } catch (Exception e) {
                System.out.println("Could not load Minecraft font, falling back to monospaced: " + e.getMessage());
                return new Font("Monospaced", Font.PLAIN, (int) size);
            }
        }
    }

    private static void downloadFont(String urlStr, File destination) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    static BufferedImage renderMotd(List<TextSegment> segments, BufferedImage serverIcon, int width, int height, String playerCountText) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        TexturePaint dirtPaint = getDirtTexturePaint();
        g.setPaint(dirtPaint);
        g.fillRect(0, 0, width, height);

        int iconSize = 100;
        int iconMargin = 10;
        int textStartX = iconMargin;
        if (serverIcon != null) {
            Image scaledIcon = serverIcon.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            int iconY = (height - iconSize) / 2;
            g.drawImage(scaledIcon, iconMargin, iconY, null);
            textStartX = iconMargin + iconSize + iconMargin;
        }

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

        int pingMarginRight = 10;
        int numBars = 5;
        int barWidth = 4;
        int gap = 2;
        int[] barHeights = {4, 8, 12, 16, 20};
        int totalBarWidth = numBars * barWidth + (numBars - 1) * gap;
        int pingIndicatorX = width - totalBarWidth - pingMarginRight;
        int pingIndicatorBaseY = countY;
        drawStaticPingIndicator(g, pingIndicatorX, pingIndicatorBaseY);

        int motdStartY = 60;
        Font baseMinecraftFont = getMinecraftFont(28f);
        Font fallbackFont = new Font("Monospaced", Font.PLAIN, 28);
        int marginX = textStartX;
        int x = marginX;
        int y = motdStartY;
        for (TextSegment seg : segments) {
            int style = Font.PLAIN;
            if (seg.isBold())
                style |= Font.BOLD;
            if (seg.isItalic())
                style |= Font.ITALIC;
            Font segMinecraftFont = baseMinecraftFont.deriveFont(style, 25);
            Font segFallbackFont = fallbackFont.deriveFont(style, 25);
            g.setColor(seg.getColor());
            String[] lines = seg.getText().split("\n", -1);
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
        int shadowOffset = 3;

        for (int i = 0; i < numBars; i++) {
            int x = startX + i * (barWidth + gap);
            int barHeight = barHeights[i];
            int y = baseY - barHeight;

            g.setColor(shadowColor);
            g.fillRect(x + shadowOffset, y + shadowOffset, barWidth, barHeight);

            g.setColor(pingColor);
            g.fillRect(x, y, barWidth, barHeight);
        }
    }

    private static int drawStringWithFallback(Graphics2D g, String text, int x, int y, Font primaryFont, Font fallbackFont) {
        int start = 0;
        int currentX = x;
        int length = text.length();

        while (start < length) {
            boolean usePrimary = primaryFont.canDisplay(text.charAt(start));
            int end = start + 1;
            while (end < length && primaryFont.canDisplay(text.charAt(end)) == usePrimary) {
                end++;
            }

            String substring = text.substring(start, end);
            Font fontToUse = usePrimary ? primaryFont : fallbackFont;
            g.setFont(fontToUse);
            g.drawString(substring, currentX, y);
            currentX += g.getFontMetrics().stringWidth(substring);
            start = end;
        }

        return currentX;
    }

    static List<TextSegment> parseTextSegments(JsonElement element, TextFormat inherited) {
        List<TextSegment> segments = new ArrayList<>();
        if (element == null) return segments;

        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            if (text.contains("§")) {
                segments.addAll(parseLegacy(text));
            } else {
                segments.add(new TextSegment(text, inherited.getColor(), inherited.isBold(), inherited.isItalic()));
            }
        } else if (element.isJsonObject()) {
            TextFormat current = new TextFormat(inherited);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("color")) {
                String colorStr = obj.get("color").getAsString();
                current.setColor(colorStr.startsWith("#") ?
                        tryDecodeColor(colorStr) :
                        MC_COLOR_MAP.getOrDefault(colorStr, Color.WHITE));
            }

            if (obj.has("bold")) current.setBold(obj.get("bold").getAsBoolean());
            if (obj.has("italic")) current.setItalic(obj.get("italic").getAsBoolean());

            String text = obj.has("text") ? obj.get("text").getAsString() : "";
            if (!text.isEmpty()) {
                if (text.contains("§")) {
                    segments.addAll(parseLegacy(text));
                } else {
                    segments.add(new TextSegment(text, current.getColor(), current.isBold(), current.isItalic()));
                }
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

    private static Color tryDecodeColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.WHITE;
        }
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

                switch (code) {
                    case 'r':
                        currentColor = Color.WHITE;
                        currentBold = false;
                        currentItalic = false;
                        break;
                    case 'l':
                        currentBold = true;
                        break;
                    case 'o':
                        currentItalic = true;
                        break;
                    case '0':
                        currentColor = MC_COLOR_MAP.get("black");
                        break;
                    case '1':
                        currentColor = MC_COLOR_MAP.get("dark_blue");
                        break;
                    case '2':
                        currentColor = MC_COLOR_MAP.get("dark_green");
                        break;
                    case '3':
                        currentColor = MC_COLOR_MAP.get("dark_aqua");
                        break;
                    case '4':
                        currentColor = MC_COLOR_MAP.get("dark_red");
                        break;
                    case '5':
                        currentColor = MC_COLOR_MAP.get("dark_purple");
                        break;
                    case '6':
                        currentColor = MC_COLOR_MAP.get("gold");
                        break;
                    case '7':
                        currentColor = MC_COLOR_MAP.get("gray");
                        break;
                    case '8':
                        currentColor = MC_COLOR_MAP.get("dark_gray");
                        break;
                    case '9':
                        currentColor = MC_COLOR_MAP.get("blue");
                        break;
                    case 'a':
                        currentColor = MC_COLOR_MAP.get("green");
                        break;
                    case 'b':
                        currentColor = MC_COLOR_MAP.get("aqua");
                        break;
                    case 'c':
                        currentColor = MC_COLOR_MAP.get("red");
                        break;
                    case 'd':
                        currentColor = MC_COLOR_MAP.get("light_purple");
                        break;
                    case 'e':
                        currentColor = MC_COLOR_MAP.get("yellow");
                        break;
                    case 'f':
                        currentColor = MC_COLOR_MAP.get("white");
                        break;
                    default:
                        break;
                }
            } else {
                currentText.append(c);
            }
        }

        if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentColor, currentBold, currentItalic));
        }

        return segments;
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
            if (numRead > 5) {
                throw new IOException("VarInt is too big");
            }
        } while ((read & 0x80) == 0x80);
        return result;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, data.length);
        out.write(data);
    }

    public BufferedImage motd() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return motdImage;
    }

    public String getOnlinePlayer() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return onlinePlayer;
    }

    public String getMaxPlayer() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return maxPlayer;
    }

    public BufferedImage getIcon() throws IOException {
        if (!retrieved) {
            retrieveData();
        }
        return serverIcon;
    }

    private void retrieveData() throws IOException {
        if (retrieved) return;

        String actualHost;
        int actualPort;

        String[] srv = resolveSRV(host);
        if (srv != null) {
            actualHost = srv[0];
            actualPort = Integer.parseInt(srv[1]);
        } else {
            actualHost = host;
            actualPort = port;
        }

        int[] protocolVersions = {
                762, 761, 760, 759, 758, 757, 756, 755, 754, 753, 751, 736, 735, 578, 575, 573, 498,
                490, 485, 480, 477, 404, 401, 393, 340, 338, 335, 316, 315, 210, 110, 47
        };

        String motdJson = null;
        for (int version : protocolVersions) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(actualHost, actualPort), DEFAULT_TIMEOUT);
                socket.setSoTimeout(DEFAULT_TIMEOUT);
                motdJson = ping(socket, version);
                if (motdJson != null) break;
            } catch (IOException e) {
                continue;
            }
        }

        if (motdJson == null) {
            throw new IOException("Failed to retrieve MOTD from server");
        }

        Gson gson = new Gson();
        JsonObject jsonObj = gson.fromJson(motdJson, JsonObject.class);

        if (jsonObj.has("players")) {
            JsonObject playersObj = jsonObj.getAsJsonObject("players");
            int online = playersObj.get("online").getAsInt();
            int max = playersObj.get("max").getAsInt();
            onlinePlayer = String.valueOf(online);
            maxPlayer = String.valueOf(max);
        }

        serverIcon = getServerIconFromJson(jsonObj);

        JsonElement description = jsonObj.get("description");
        List<TextSegment> segments = parseTextSegments(description, new TextFormat());

        String playerCountText = (onlinePlayer != null && maxPlayer != null) ? (onlinePlayer + "/" + maxPlayer) : "";
        motdImage = renderMotd(segments, serverIcon, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, playerCountText);

        retrieved = true;
    }
}