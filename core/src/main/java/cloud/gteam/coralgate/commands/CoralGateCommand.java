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

package cloud.gteam.coralgate.commands;

import cloud.gteam.coralgate.CorePlugin;
import cloud.gteam.coralgate.commands.permissions.CommandPermission;
import cloud.gteam.coralgate.config.ConfigManager;
import cloud.gteam.coralgate.config.ConfigModel;
import com.github.retrooper.packetevents.PacketEvents;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.command.CommandActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Command({"coralgate", "cg"})
public class CoralGateCommand {

    private final CorePlugin corePlugin;

    public CoralGateCommand(final CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    // /coralgate|cg (help)
    @Subcommand("help")
    @Description("Show the help message for CoralGate.")
    @CommandPermission("coralgate.commands.help")
    public void help(final CommandActor actor) {

        final ConfigManager configManager = this.corePlugin.getConfigManager();
        final ConfigModel config = configManager.getConfig();

        final String[] lines = {
                "§7-----------------------------------------------------",
                "",
                config.getNormalPrefix() + "§f" + this.corePlugin.getPlatformProperties().getProperty("platform-version") + " | §7§o(" + this.corePlugin.getPlatformProperties().getProperty("core-version") + ")§r",
                "§7Made by XIII___ and Vagdedes2 with(out) love!",
                "",
                "§7Available commands:",
                "",
                " - /coralgate§8|cg§r help §7- Show this menu.",
                " - /coralgate§8|cg§r version§8|ver§r §7- Show versions.",
                "",
                "§bHaving troubles ? Need help ? Found a bug ?",
                "§7Join our Discord: §bhttps://discord.gteam.cloud",
                "",
                "§7-----------------------------------------------------"
        };

        for (String line : lines) {

            // Velocity only sees colors via "&" and not "§".
            // However, other platforms only see colors via "§", so we'll have to handle this specifically for Velocity.
            if (this.corePlugin.getPlatformProperties().getProperty("platform-name").equals("velocity")) {
                line = line.replace("§", "&");
            }

            actor.reply(line);

        }

    }

    // /coralgate|cg version|ver
    @Subcommand({"version", "ver"})
    @Description("Show the different versions for CoralGate.")
    @CommandPermission("coralgate.commands.version")
    public void version(final CommandActor actor) {

        final ConfigManager configManager = this.corePlugin.getConfigManager();
        final ConfigModel config = configManager.getConfig();

        final String currentVersion = this.corePlugin.getPlatformProperties().getProperty("core-version") + "_" + this.corePlugin.getPlatformProperties().getProperty("platform-version");

        final String versionColor = Objects.equals(this.corePlugin.getUpdateChecker().getLatestVersion(), currentVersion) ? "§a" : "§e";
        final String configColor = Objects.equals(this.corePlugin.getConfigManager().getLatestConfigVersion(), config.getConfigVersion()) ? "§a" : "§e";
        final String packeteventsColor = PacketEvents.getAPI().getVersion().toString().equals(this.corePlugin.getPlatformProperties().getProperty("packetevents-version")) ? "§a" : "§e";

        // Create an array list so we can freely handle conditional statements with {} blocks
        final List<String> lines = new ArrayList<>();

        lines.add("§7-----------------------------------------------------");
        lines.add("");
        lines.add(config.getNormalPrefix() + "§fVersions information");
        lines.add("");
        lines.add("§7Platform: §f" + this.corePlugin.getPlatformProperties().getProperty("platform-name"));
        lines.add("§7Version: " + versionColor + this.corePlugin.getPlatformProperties().getProperty("platform-version"));
        lines.add("");
        lines.add("§7Core version: " + versionColor + this.corePlugin.getPlatformProperties().getProperty("core-version"));
        lines.add("");
        lines.add("§7Configuration version: §f" + configColor + config.getConfigVersion());
        lines.add("");

        if (config.isAllowApiUsage()) {
            lines.add("§7API host: §f" + config.getApiHost());
            lines.add("§7API version: §f" + config.getApiVersion());
        } else {
            lines.add("§7API usage is disabled.");
        }

        lines.add("");
        lines.add("§7packetevents version: " + packeteventsColor + this.corePlugin.getPlatformProperties().getProperty("packetevents-version"));
        lines.add("");
        lines.add("§7-----------------------------------------------------");

        for (String line : lines) {

            // Velocity only sees colors via "&" and not "§".
            // However, other platforms only see colors via "§", so we'll have to handle this specifically for Velocity.
            if (this.corePlugin.getPlatformProperties().getProperty("platform-name").equals("velocity")) {
                line = line.replace("§", "&");
            }

            actor.reply(line);

        }

    }

}
