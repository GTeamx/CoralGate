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

import net.md_5.bungee.api.plugin.Plugin;

public final class BungeePlugin extends Plugin {

    private final PluginCore pluginCore = new PluginCore();
    @Override
    public void onEnable() {

        this.pluginCore.onEnable(this.getProxy().getConfig().isOnlineMode());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
