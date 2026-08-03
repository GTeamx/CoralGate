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
import cloud.gteam.coralgate.commands.SpigotPermissionChecker;
import cloud.gteam.coralgate.commands.permissions.PermissionFactory;
import cloud.gteam.coralgate.injector.SpigotInjector;
import cloud.gteam.coralgate.injector.handlers.SpigotNettyResponder;
import cloud.gteam.coralgate.processor.NetworkProcessor;
import cloud.gteam.coralgate.utils.PlatformUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.Properties;

public final class SpigotPlugin extends JavaPlugin {

    private final CorePlugin corePlugin = new CorePlugin();

    private final SpigotInjector injector = new SpigotInjector();

    @Override
    public void onLoad() {

        if (!this.injector.isServerBound()) {
            this.injector.inject();
        }

        PacketEvents.getAPI().getEventManager().registerListener(new NetworkProcessor(getCorePlugin()), PacketListenerPriority.HIGHEST);

    }

    @Override
    public void onEnable() {

        // Inject for LEGACY_SERVER_LIST_PING.
        if (!this.injector.hasInjected) {
            this.injector.inject();
        }

        // Start bStats.
        new Metrics(this, 29439);

        // Get properties early to modify the platform name later on if needed.
        final Properties platformProperties = PlatformUtils.loadProperties(this.getClass());

        // Paper's bootstrapper context class is only present when loaded via paper-plugin.yml
        // If it exists we're running as 'paper'.
        try {

            Class.forName("io.papermc.paper.plugin.bootstrap.BootstrapContext");
            platformProperties.setProperty("platform-name", "paper");

        } catch (final ClassNotFoundException ignored) {}

        // Load core.
        this.corePlugin.onEnable(this.getLogger(), getDataFolder(), Bukkit.getOnlineMode(), "server.properties", platformProperties, new SpigotNettyResponder());

        // Load commands.
        final Lamp<BukkitCommandActor> bukkitCommandActor = BukkitLamp.builder(this)
                .permissionFactory(new PermissionFactory(new SpigotPermissionChecker()))
                .build();
        bukkitCommandActor.register(new CoralGateCommand(this.corePlugin));

    }

    @Override
    public void onDisable() {

        this.injector.uninject();

        this.corePlugin.onDisable();

    }

    public CorePlugin getCorePlugin() {
        return this.corePlugin;
    }

}
