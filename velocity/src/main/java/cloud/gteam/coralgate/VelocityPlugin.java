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

package cloud.gteam.coralgate;

import cloud.gteam.coralgate.commands.CoralGateCommand;
import cloud.gteam.coralgate.commands.VelocityPermissionChecker;
import cloud.gteam.coralgate.commands.permissions.PermissionFactory;
import cloud.gteam.coralgate.injector.VelocityInjector;
import cloud.gteam.coralgate.injector.handlers.VelocityNettyResponder;
import cloud.gteam.coralgate.processor.NetworkProcessor;
import cloud.gteam.coralgate.utils.ConfigUtils;
import cloud.gteam.coralgate.utils.PlatformUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import revxrsal.commands.Lamp;
import revxrsal.commands.velocity.VelocityLamp;
import revxrsal.commands.velocity.VelocityVisitors;
import revxrsal.commands.velocity.actor.VelocityCommandActor;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class VelocityPlugin {

    private final CorePlugin corePlugin = new CorePlugin();
    private final VelocityInjector injector;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityPlugin(final ProxyServer proxyServer, final @DataDirectory Path dataDirectory, final Metrics.Factory metricsFactory) {

        this.dataDirectory = dataDirectory;
        // Load bStats metrics factory.
        this.metricsFactory = metricsFactory;

        this.injector = new VelocityInjector(proxyServer);

        // Load commands.
        final Lamp<VelocityCommandActor> lamp = VelocityLamp.builder(this, proxyServer)
                .permissionFactory(new PermissionFactory(new VelocityPermissionChecker()))
                .build();
        lamp.register(new CoralGateCommand(this.corePlugin));

        lamp.accept(VelocityVisitors.brigadier(proxyServer));

    }

    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent proxyInitializeEvent) {

        this.injector.inject();

        // Start bStats.
        this.metricsFactory.make(this, 29439);

        // onLoad equivalent.
        PacketEvents.getAPI().getEventManager().registerListener(
                new NetworkProcessor(getCorePlugin()), PacketListenerPriority.HIGHEST);

        // Load core.
        this.corePlugin.onEnable(Logger.getLogger("CoralGate"), this.dataDirectory.toFile(), ConfigUtils.isOnlineMode("velocity.toml"), "velocity.toml", PlatformUtils.loadProperties(this.getClass()), new VelocityNettyResponder());

    }

    @Subscribe
    public void onProxyShutdown(final ProxyShutdownEvent proxyShutdownEvent) {

        this.injector.uninject();

        this.corePlugin.onDisable();

    }

    public CorePlugin getCorePlugin() {
        return this.corePlugin;
    }

}
