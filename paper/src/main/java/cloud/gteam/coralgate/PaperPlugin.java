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
import cloud.gteam.coralgate.commands.PaperPermissionChecker;
import cloud.gteam.coralgate.commands.permissions.PermissionFactory;
import cloud.gteam.coralgate.processor.NetworkProcessor;
import cloud.gteam.coralgate.utils.PlatformUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

public final class PaperPlugin extends JavaPlugin {

    private final CorePlugin corePlugin = new CorePlugin();

    @Override
    public void onLoad() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new NetworkProcessor(getCorePlugin()), PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onEnable() {

        // Load core.
        this.corePlugin.onEnable(this.getLogger(), getDataFolder(), Bukkit.getOnlineMode(), "server.properties", PlatformUtils.loadProperties(this.getClass()));

        // Load commands.
        final Lamp<BukkitCommandActor> bukkitCommandActor = BukkitLamp.builder(this)
                .permissionFactory(new PermissionFactory(new PaperPermissionChecker()))
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
