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

        actor.sendRawMessage("§7-----------------------------------------------------");
        actor.sendRawMessage("");
        actor.sendRawMessage(config.getNormalPrefix() + "§f" + this.corePlugin.getPlatformProperties().getProperty("platform-version") + " | §7§o(" + this.corePlugin.getPlatformProperties().getProperty("core-version") + ")§r");
        actor.sendRawMessage("§7Made by XIII___ and Vagdedes2 with(out) love!");
        actor.sendRawMessage("");
        actor.sendRawMessage("§7Available commands:");
        actor.sendRawMessage("");
        actor.sendRawMessage(" - /coralgate§8|cg§r help §7- Show this menu.");
        actor.sendRawMessage(" - /coralgate§8|cg§r version§8|ver§r §7- Show versions.");
        actor.sendRawMessage(" - /coralgate§8|cg§r config§8|cfg§r reload§8|rl§r §7- Reload the config.");
        actor.sendRawMessage("");
        actor.sendRawMessage("§bHaving troubles ? Need help ? Found a bug ?");
        actor.sendRawMessage("§7Join our Discord: §bhttps://discord.gg/rxV89DZHEd");
        actor.sendRawMessage("");
        actor.sendRawMessage("§7-----------------------------------------------------");

    }

    // /coralgate|cg version|ver
    @Subcommand({"version", "ver"})
    @Description("Show different versions for CoralGate.")
    @CommandPermission("coralgate.commands.version")
    public void version(final CommandActor actor) {

        final ConfigManager configManager = this.corePlugin.getConfigManager();
        final ConfigModel config = configManager.getConfig();

        final String currentVersion = this.corePlugin.getPlatformProperties().getProperty("core-version") + "_" + this.corePlugin.getPlatformProperties().getProperty("platform-version");

        actor.sendRawMessage("§7-----------------------------------------------------");
        actor.sendRawMessage("");
        actor.sendRawMessage(config.getNormalPrefix() + "§fVersions information");
        actor.sendRawMessage("");
        actor.sendRawMessage("§7Platform: §f" + this.corePlugin.getPlatformProperties().getProperty("platform-name"));
        actor.sendRawMessage("§7Version: " + (Objects.equals(this.corePlugin.getUpdateChecker().getLatestVersion(), currentVersion) ? "§a" : "§e") + this.corePlugin.getPlatformProperties().getProperty("platform-version"));
        actor.sendRawMessage("");
        actor.sendRawMessage("§7Core version: " + (Objects.equals(this.corePlugin.getUpdateChecker().getLatestVersion(), currentVersion) ? "§a" : "§e") + this.corePlugin.getPlatformProperties().getProperty("core-version"));
        actor.sendRawMessage("");
        actor.sendRawMessage("§7Configuration version: §f" + config.getConfigVersion());
        actor.sendRawMessage("");
        actor.sendRawMessage("§7API host: §f" + config.getApiHost());
        actor.sendRawMessage("§7API version: §f" + config.getApiVersion());
        actor.sendRawMessage("");
        actor.sendRawMessage("§7packetevents version: " + (PacketEvents.getAPI().getVersion().toString().equals(this.corePlugin.getPlatformProperties().getProperty("packetevents-version")) ? "§a" : "§e") + this.corePlugin.getPlatformProperties().getProperty("packetevents-version"));
        actor.sendRawMessage("");
        actor.sendRawMessage("§7-----------------------------------------------------");

    }

    // /coralgate|cg config|cfg reload|rl.
    @Subcommand({"config reload", "config rl", "cfg reload", "cfg rl"})
    @Description("Reload the configuration file of CoralGate.")
    @CommandPermission("coralgate.commands.config.reload")
    public void configReload(final CommandActor actor) {

        final ConfigManager configManager = this.corePlugin.getConfigManager();
        ConfigModel config = configManager.getConfig();

        actor.sendRawMessage(config.getNormalPrefix() + "Reloading configuration file...");

        // Get latest config version.
        final String latestConfigVersion = new ConfigModel().getConfigVersion();

        configManager.load();
        // Update config with latest load.
        config = configManager.getConfig();

        // Compare current config version and latest version and alert the user if necessary.
        if (!Objects.equals(latestConfigVersion, config.getConfigVersion())) actor.sendRawMessage(config.getWarningPrefix() + "Please consider upgrading your configuration file to the latest version: '" + latestConfigVersion + "'. Your configuration file is at version '" + config.getConfigVersion() + "'.");

        actor.sendRawMessage(config.getNormalPrefix() + "Configuration file reloaded!");

    }

}
