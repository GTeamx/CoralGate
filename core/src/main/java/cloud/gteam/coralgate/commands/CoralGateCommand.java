/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2025 GTeamX (GTeam) and it's contributors
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
