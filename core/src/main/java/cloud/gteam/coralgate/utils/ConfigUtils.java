/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2026 GTeamX (GTeam) and it's contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cloud.gteam.coralgate.utils;

import cloud.gteam.coralgate.CorePlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class ConfigUtils {

    public static boolean isOnlineMode(final String fileName) {

        // Read 'online-mode' field from provided config file.
        try {

            final List<String> lines = Files.readAllLines(Paths.get(fileName));
            for (final String line : lines) {
                if (line.trim().startsWith("online-mode")) return line.contains("true");
            }

        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't fetch '" + fileName + "' online-mode field. CoralGate might not work as expected. See error: " + e.getMessage());
        }

        // Default 'true' online mode for most servers.
        return true;

    }

    public static int getCompressionThreshold(final String fileName) {

        try {

            final List<String> lines = Files.readAllLines(Paths.get(fileName));
            for (final String line : lines) {

                final String trimmed = line.trim();

                // Try to get normal server.properties 'network-compression-threshold' field, else try velocity's 'compression-threshold', else try BungeeCord's 'network_compression_threshold', if all fail put 0.
                if (trimmed.startsWith("network-compression-threshold") || trimmed.startsWith("compression-threshold") || trimmed.startsWith("network_compression_threshold")) {

                    // Extract the number after the = or :
                    return Integer.parseInt(trimmed.split("[=:]")[1].trim());

                }

            }

        } catch (final IOException | NumberFormatException e) {
            CorePlugin.getLogger().severe("Couldn't fetch '" + fileName + "' compression threshold field. CoralGate might not work as expected. See error: " + e.getMessage());
        }

        // Default 256.
        return 256;

    }

}
