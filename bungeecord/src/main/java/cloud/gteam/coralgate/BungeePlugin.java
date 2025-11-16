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

import cloud.gteam.coralgate.processor.NetworkProcessor;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeePlugin extends Plugin {

    private final PluginCore pluginCore = new PluginCore();
    private boolean canRun = false;

    @Override
    public void onLoad() {

        // Check if PacketEvents (code name: 'packetevents) is installed
        if (ProxyServer.getInstance().getPluginManager().getPlugin("packetevents") == null) {

            PluginCore.getLogger().severe("PacketEvents is required to run CoralGate! Please install PacketEvents in your plugins folder.");
            return;

        } else canRun = true;

        PacketEvents.getAPI().getEventManager().registerListener(
                new NetworkProcessor(getPluginCore()), PacketListenerPriority.HIGHEST);

    }

    @Override
    public void onEnable() {

        if (canRun) this.pluginCore.onEnable(this.getProxy().getConfig().isOnlineMode(), "config.yml");

    }

    @Override
    public void onDisable() {

        // Plugin shutdown logic

    }

    public PluginCore getPluginCore() {
        return this.pluginCore;
    }

}
