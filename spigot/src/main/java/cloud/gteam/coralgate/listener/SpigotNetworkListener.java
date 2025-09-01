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

package cloud.gteam.coralgate.listener;

import cloud.gteam.coralgate.SpigotPlugin;
import cloud.gteam.coralgate.mappings.SpigotMappings;
import cloud.gteam.coralgate.packetevents.mappings.enums.client.ClientPacketType;
import cloud.gteam.coralgate.packetevents.mappings.enums.server.ServerPacketType;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

import java.net.SocketAddress;

public class SpigotNetworkListener implements PacketListener {

    private final SpigotPlugin spigotPlugin;

    public SpigotNetworkListener(final SpigotPlugin spigotPlugin) {
        this.spigotPlugin = spigotPlugin;
    }

    public void onPacketReceive(final PacketReceiveEvent packetReceiveEvent) {

        final SocketAddress socketAddress = packetReceiveEvent.getSocketAddress();

        // Map PacketEvents to CoralGate's core
        final ClientPacketType clientPacketType = SpigotMappings.mapToClientType(packetReceiveEvent);
        final Object packetWrapper = SpigotMappings.mapToWrapper(packetReceiveEvent, clientPacketType);

        // Send packet to core and take potential action.
        switch (this.spigotPlugin.getPluginCore().getNetworkProcessor().handlePacketReceive(socketAddress, clientPacketType, packetWrapper)) {

            case CANCEL:

                packetReceiveEvent.setCancelled(true);
                break;

            case DISCONNECT:

                packetReceiveEvent.getUser().closeConnection();
                packetReceiveEvent.setCancelled(true);
                break;

        }

    }

    public void onPacketSend(final PacketSendEvent packetSendEvent) {

        final SocketAddress socketAddress = packetSendEvent.getSocketAddress();

        // Map PacketEvents to CoralGate's core
        final ServerPacketType serverPacketType = SpigotMappings.mapToServerType(packetSendEvent);
        final Object packetWrapper = SpigotMappings.mapToWrapper(packetSendEvent, serverPacketType);

        // Send packet to core and take potential action.
        switch (this.spigotPlugin.getPluginCore().getNetworkProcessor().handlePacketSend(socketAddress, serverPacketType, packetWrapper)) {

            case CANCEL:

                packetSendEvent.setCancelled(true);
                break;

            case DISCONNECT:

                packetSendEvent.getUser().closeConnection();
                packetSendEvent.setCancelled(true);
                break;

        }

    }

}
