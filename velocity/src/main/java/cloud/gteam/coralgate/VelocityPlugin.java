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
import cloud.gteam.coralgate.utils.ConfigUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;

public final class VelocityPlugin {

    private final PluginCore pluginCore = new PluginCore();

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent proxyInitializeEvent) {

        // onLoad equivalent
        PacketEvents.getAPI().getEventManager().registerListener(
                new NetworkProcessor(getPluginCore()), PacketListenerPriority.HIGHEST);

        this.pluginCore.onEnable(ConfigUtils.isOnlineMode("velocity.toml"), "velocity.toml");

    }

    @Subscribe
    public void onProxyShutdown(final ProxyShutdownEvent proxyShutdownEvent) {

        // Plugin shutdown logic

    }

    public PluginCore getPluginCore() {
        return this.pluginCore;
    }

}
