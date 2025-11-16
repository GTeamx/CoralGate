package cloud.gteam.coralgate.utils;

import cloud.gteam.coralgate.PluginCore;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public final class ConfigUtils {

    public static boolean isOnlineMode(final String fileName) {

        // Read 'online-mode' field from provided config file
        try {

            final List<String> lines = Files.readAllLines(Paths.get(fileName));
            for (final String line : lines) {
                if (line.trim().startsWith("online-mode")) return line.contains("true");
            }

        } catch (final Exception e) {
            PluginCore.getLogger().severe("Couldn't fetch '" + fileName + "' online-mode field. CoralGate might not work as expected. See error: " + e.getMessage());
        }

        // Default 'true' online mode for most servers
        return true;

    }

    public static int getCompressionThreshold(final String fileName) {

        final Properties properties = new Properties();

        try (final FileInputStream fileInputStream = new FileInputStream(fileName)) {

            properties.load(fileInputStream);

            // Try to get normal server.properties 'network-compression-threshold' field, else try velocity's 'compression-threshold', else try BungeeCord's 'network_compression_threshold', if all fail put 0
            return Integer.parseInt(properties.getProperty("network-compression-threshold", properties.getProperty("compression-threshold", properties.getProperty("network_compression_threshold", "0"))));

        } catch (final IOException e) {
            PluginCore.getLogger().severe("Couldn't fetch '" + fileName + "' compression threshold field. CoralGate might not work as expected. See error: " + e.getMessage());
        }

        // Default 256
        return 256;

    }

}
