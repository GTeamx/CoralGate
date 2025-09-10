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

package cloud.gteam.coralgate;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Plugin(
    id = "velocity",
    name = "velocity",
    version = "0.1.0"
    ,description = "A simple plugin to prevent server scanners from reaching your server."
    ,authors = {"XIII___, Vagdedes2"}
)
public class VelocityPlugin {

    private final PluginCore pluginCore = new PluginCore();

    @Inject private Logger logger;

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent event) {

        this.pluginCore.onEnable(isOnlineMode());

    }

    private boolean isOnlineMode() {

        try {

            final List<String> lines = Files.readAllLines(Paths.get("velocity.toml"));
            for (final String line : lines) {

                if (line.trim().startsWith("online-mode")) {

                    return line.contains("true");

                }

            }

        } catch (final Exception e) {
            this.logger.error("Couldn't fetch velocity.toml online-mode. CoralGate might not work as expected. See error:{}", e.getMessage());
        }

        return false;

    }

}
