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

package cloud.gteam.coralgate.bridge;

import cloud.gteam.coralgate.mappings.SpigotMappings;
import cloud.gteam.coralgate.packetevents.mappings.enums.server.ServerPacketType;
import com.github.retrooper.packetevents.PacketEvents;

import java.net.SocketAddress;

public class SpigotPlatformBridge implements PlatformBridge {

    @Override
    public void sendPacket(final SocketAddress socketAddress, final ServerPacketType serverPacketType, final Object packetWrapper) {
        PacketEvents.getAPI().getProtocolManager().getUsers().stream().filter(filteredUser -> filteredUser.getAddress() == socketAddress).forEach(user -> user.sendPacketSilently(SpigotMappings.mapToPacketWrapper(serverPacketType, packetWrapper)));
    }

}
