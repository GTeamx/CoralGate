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

import cloud.gteam.coralgate.commands.BungeePermissionChecker;
import cloud.gteam.coralgate.commands.CoralGateCommand;
import cloud.gteam.coralgate.commands.permissions.PermissionFactory;
import cloud.gteam.coralgate.injector.BungeeInjector;
import cloud.gteam.coralgate.processor.NetworkProcessor;
import cloud.gteam.coralgate.utils.PlatformUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import revxrsal.commands.Lamp;
import revxrsal.commands.bungee.BungeeLamp;
import revxrsal.commands.bungee.actor.BungeeCommandActor;

public final class BungeePlugin extends Plugin {

    private final CorePlugin corePlugin = new CorePlugin();
    private final BungeeInjector injector = new BungeeInjector();

    @Override
    public void onLoad() {

        this.injector.inject();

        PacketEvents.getAPI().getEventManager().registerListener(new NetworkProcessor(getCorePlugin()), PacketListenerPriority.HIGHEST);

    }

    @Override
    public void onEnable() {

        // Start bStats.
        new Metrics(this, 29439);

        // Load core.
        this.corePlugin.onEnable(this.getLogger(), getDataFolder(), this.getProxy().getConfig().isOnlineMode(), "config.yml", PlatformUtils.loadProperties(this.getClass()));

        // Load commands.
        final Lamp<BungeeCommandActor> bukkitCommandActor = BungeeLamp.builder(this)
                .permissionFactory(new PermissionFactory(new BungeePermissionChecker()))
                .build();
        bukkitCommandActor.register(new CoralGateCommand(this.corePlugin));

    }

    @Override
    public void onDisable() {

        this.corePlugin.onDisable();

    }

    public CorePlugin getCorePlugin() {
        return this.corePlugin;
    }

}
