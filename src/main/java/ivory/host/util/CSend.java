package ivory.host.util;

import ivory.host.McStatusBot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSend {

    private static final File DEBUG_LOG = new File("debug/debug.log");
    private static final File ERROR_LOG = new File("debug/error.log");
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private CSend() {
        throw new UnsupportedOperationException("This is a utility class.");
    }

    private static String getPrefix() {
        return McStatusBot.getPrefix() != null ? McStatusBot.getPrefix() : "";
    }

    public static void plain(String message) {
        System.out.println(message);
    }

    public static void prefixed(String message) {
        plain(getPrefix() + " " + message);
    }

    public static void info(String message) {
        prefixed("[INFO] " + message);
    }

    public static void warn(String message) {
        prefixed("[WARNING] " + message);
    }

    public static void error(String message) {
        String fullMessage = "[ERROR] " + message;
        prefixed(fullMessage);
        logToFile(ERROR_LOG, fullMessage);
    }

    public static void debug(String message) {
        if (McStatusBot.isDebug()) {
            String fullMessage = "[DEBUG] " + message;
            prefixed(fullMessage);
            logToFile(DEBUG_LOG, fullMessage);
        }
    }

    public static void error(Throwable throwable) {
        if (throwable == null) {
            error("Unknown error (null throwable).");
            return;
        }

        error("Exception: " + throwable.getMessage());
        for (StackTraceElement ste : throwable.getStackTrace()) {
            debug("  at " + ste.toString());
            logToFile(ERROR_LOG, "  at " + ste);
        }
    }

    private static void logToFile(File file, String message) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write("[" + FORMAT.format(new Date()) + "] " + message + System.lineSeparator());
            }
        } catch (IOException e) {
            System.out.println("[Logger Error] Failed to write to log file: " + file.getName());
        }
    }

}
