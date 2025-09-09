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

package cloud.gteam.coralgate.processor;

import cloud.gteam.coralgate.PluginCore;
import cloud.gteam.coralgate.packetevents.mappings.enums.ConnectionStateMappings;
import cloud.gteam.coralgate.packetevents.mappings.enums.PacketAction;
import cloud.gteam.coralgate.packetevents.mappings.enums.client.ClientPacketType;
import cloud.gteam.coralgate.packetevents.mappings.enums.server.ServerPacketType;
import cloud.gteam.coralgate.packetevents.mappings.wrappers.WrapperHandshakingClientHandshakeMappings;
import cloud.gteam.coralgate.packetevents.mappings.wrappers.WrapperLoginClientEncryptionResponseMappings;
import cloud.gteam.coralgate.packetevents.mappings.wrappers.WrapperLoginClientLoginStartMappings;
import cloud.gteam.coralgate.util.APIUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkProcessor {

    private final PluginCore pluginCore;

    private final ConcurrentHashMap<SocketAddress, Integer> connectionState = new ConcurrentHashMap<>();

    public NetworkProcessor(final PluginCore pluginCore) {
        this.pluginCore = pluginCore;
    }

    // Incoming packets (Client -> Server) [C->S]
    public PacketAction handlePacketReceive(final SocketAddress socketAddress, final ClientPacketType clientPacketType, final Object packetWrapper) {

        final InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        final String socketIp = inetSocketAddress.getHostString();

        /// Port filtering.

        // This is the lowest dynamic port used by Linux.
        // Anything bellow means the port was forced to use that port and is therefore, not a real Minecraft client.
        if (inetSocketAddress.getPort() < 32768) {

            // TODO: Add config to "silent" those messages
            PluginCore.getLogger().severe("Invalid port used by client. Closing connection from " + socketAddress + " [C->S | " + clientPacketType.name() + "]");

            // Report IP to CoralGate API
            APIUtils.reportIp(socketIp);

            // Cancel the packet and close the connection.
            return PacketAction.DISCONNECT;

        // The port is ok, continue.
        } else if (clientPacketType == ClientPacketType.PING || clientPacketType == ClientPacketType.REQUEST || clientPacketType == ClientPacketType.HANDSHAKE) {

            // This is the lowest dynamic port used by Windows & Mac.
            // Since Linux players are "rare", we'll issue a warning statement about them.
            // Alongside that, we will block any server list ping to prevent bots from getting information about the server.
            // (server version, online players, player count...)
            if (inetSocketAddress.getPort() < 49152) {

                // Log about this suspicious connection.
                PluginCore.getLogger().warning("Suspicious port used by client. Keep an eye out for " + socketAddress + ". [C->S | " + clientPacketType.name() + "]");

                // Handshake logic is handled bellow.
                if (clientPacketType != ClientPacketType.HANDSHAKE) return PacketAction.CANCEL;

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

            }

            if (clientPacketType == ClientPacketType.HANDSHAKE) {

                final WrapperHandshakingClientHandshakeMappings wrapperHandshakingClientHandshakeMappings = (WrapperHandshakingClientHandshakeMappings) packetWrapper;

                // Filter only 'STATUS' to not block logins.
                if (wrapperHandshakingClientHandshakeMappings.getIntention() == WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings.STATUS && wrapperHandshakingClientHandshakeMappings.getNextConnectionState() == ConnectionStateMappings.STATUS) {

                    // Prevent suspicious ports from getting 'STATUS'.
                    if (inetSocketAddress.getPort() >= 49152) {

                        return PacketAction.NONE;

                    } else return  PacketAction.CANCEL;

                }

            } else return PacketAction.NONE;

        }

        /// Proper login procedure checking.

        // First packet fired when a client initiates a connection.
        if (clientPacketType == ClientPacketType.HANDSHAKE) {

            final WrapperHandshakingClientHandshakeMappings wrapperHandshakingClientHandshakeMappings = (WrapperHandshakingClientHandshakeMappings) packetWrapper;

            // Client login.
            if (wrapperHandshakingClientHandshakeMappings.getIntention() == WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings.LOGIN && wrapperHandshakingClientHandshakeMappings.getNextConnectionState() == ConnectionStateMappings.LOGIN) {

                //PluginCore.getLogger().info("Version " + wrapperHandshakingClientHandshakeMappings.getClientVersion() + " for " + socketIp);

                // First connection step.
                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            }

            // Block further logic.
            return PacketAction.NONE;

        // After handshake has passed.
        } else if (clientPacketType == ClientPacketType.LOGIN_START) {

            // Checking if handshake has passed successfully.
            if (this.connectionState.getOrDefault(socketAddress, 0) == 1) {

                final WrapperLoginClientLoginStartMappings wrapperLoginClientLoginStartMappings = (WrapperLoginClientLoginStartMappings) packetWrapper;

                // Check for obvious Bot names, most of the time "Player".
                if (wrapperLoginClientLoginStartMappings.getUsername().equalsIgnoreCase("Player")) {

                    // Log bot looking name.
                    PluginCore.getLogger().severe("Bot looking name detected. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                    // Report IP to CoralGate API
                    APIUtils.reportIp(socketIp);

                    // Cancel the packet and close the connection.
                    return PacketAction.DISCONNECT;

                // If the handshake and login procedure are good while not having a bot looking name, continue.
                } else {

                    // Check if the IP is blocked by CoralGate's API.
                    try {

                        if (APIUtils.isIpBlocked(socketIp).get()) {

                            // Log blocked ip.
                            PluginCore.getLogger().severe("IP is blocked by CoralGate's API. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                            return PacketAction.DISCONNECT;

                        }

                    } catch (final Exception e) {
                        PluginCore.getLogger().severe("Couldn't fetch the blocked status of an IP. Is the API down? See error: " + e.getMessage());
                    }

                    this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

                }

            // User has skipped the proper handshake.
            } else {

                // Log invalid handshake procedure.
                PluginCore.getLogger().severe("Missing handshake procedure. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            // Block further logic.
            return PacketAction.NONE;

        // After login start has passed. This is only for servers that are in online mode.
        } else if (clientPacketType == ClientPacketType.ENCRYPTION_RESPONSE && this.pluginCore.isOnlineMode()) {

            // Checking if encryption request has passed successfully.
            if (this.connectionState.getOrDefault(socketAddress, 0) == 3) {

                final WrapperLoginClientEncryptionResponseMappings wrapperLoginClientEncryptionResponseMappings = (WrapperLoginClientEncryptionResponseMappings) packetWrapper;

                // TODO: Check stuff with the encryption keys

                // Everything was validated, continue.
                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            // User has skipped the proper login start.
            } else {

                // Log invalid handshake procedure.
                PluginCore.getLogger().severe("Missing login start procedure. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            // Block further logic.
            return PacketAction.NONE;

        // After everything was completed, the server lets them in and the user finalizes their connection.
        } else if (clientPacketType == ClientPacketType.LOGIN_SUCCESS_ACK) {

            // Checking if login success and encryption response passed successfully (or not if server is not in online mode).
            if (this.connectionState.getOrDefault(socketAddress, 0) == (this.pluginCore.isOnlineMode() ? 6 : 4)) {

                // Everything was validated, continue.
                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            // User has skipped the encryption response (if the server is in online mode) or the server did not send login success.
            } else {

                // Log invalid encryption response procedure.
                PluginCore.getLogger().severe("Missing login success procedure. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            // Block further logic.
            return PacketAction.NONE;

        }

        /// Block packets if the login procedure is not followed.

        // The player (most likely bot) didn't follow the proper login procedure, close their connection.
        // Still need to account for the no encryption response on non-online mode servers.
        if (this.connectionState.getOrDefault(socketAddress, 0) != (this.pluginCore.isOnlineMode() ? 7 : 5)) {

            // Log invalid encryption response procedure.
            PluginCore.getLogger().severe("Missing full connection procedure. Closing connection from " + socketAddress + ". [C->S | " + clientPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

            // Report IP to CoralGate API
            APIUtils.reportIp(socketIp);

            // Cancel the packet and close the connection.
            return PacketAction.DISCONNECT;

        }

        // Everything is good!
        return PacketAction.NONE;

    }

    // Outgoing packets (Server to Client) [S->C]
    public PacketAction handlePacketSend(final SocketAddress socketAddress, final ServerPacketType serverPacketType, final Object packetWrapper) {

        final InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
        final String socketIp = inetSocketAddress.getHostString();

        /// Proper login procedure checking.

        // Client should have passed login start procedure.
        if (serverPacketType == ServerPacketType.ENCRYPTION_REQUEST && this.pluginCore.isOnlineMode()) {

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(socketAddress, 0) == 2) {

                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            } else {

                // Log invalid encryption response procedure.
                PluginCore.getLogger().severe("Missing login start procedure. Closing connection from " + socketAddress + ". [S->C | " + serverPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            return PacketAction.NONE;

        // Client should have sent the encryption response (if online mode).
        } else if (serverPacketType == ServerPacketType.SET_COMPRESSION) { // TODO: Check if 'network-compression-threshold' is not -1, if it is, skip this

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(socketAddress, 0) == (this.pluginCore.isOnlineMode() ? 4 : 2)) {

                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            } else {

                // Log invalid encryption response procedure.
                PluginCore.getLogger().severe("Missing encryption response or login start procedure. Closing connection from " + socketAddress + ". [S->C | " + serverPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            return PacketAction.NONE;

        // Server should have set compression
        } else if (serverPacketType == ServerPacketType.LOGIN_SUCCESS) {

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(socketAddress, 0) == (this.pluginCore.isOnlineMode() ? 5 : 3)) {

                this.connectionState.put(socketAddress, this.connectionState.getOrDefault(socketAddress, 0) + 1);

            } else {

                // Log invalid encryption response procedure.
                PluginCore.getLogger().severe("Missing set compression procedure. Closing connection from " + socketAddress + ". [S->C | " + serverPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

                // Report IP to CoralGate API
                APIUtils.reportIp(socketIp);

                // Cancel the packet and close the connection.
                return PacketAction.DISCONNECT;

            }

            return PacketAction.NONE;

        }

        // Whitelisted packets that should not be blocked, even if procedure is not complete.
        if (serverPacketType == ServerPacketType.RESPONSE || serverPacketType == ServerPacketType.PONG) {

            // Don't send back the packet if the IP is blocked by CoralGate's API.
            APIUtils.isIpBlocked(socketIp).thenAccept(blocked -> {

                if (!blocked) {

                    //PluginCore.getLogger().info("Allowed connection to " + socketIp);

                    this.pluginCore.getPlatformBridge().sendPacket(socketAddress, serverPacketType, packetWrapper);

                // Log blocked ip.
                } else PluginCore.getLogger().severe("IP is blocked by CoralGate's API. Closing connection from " + socketAddress + ". [S->C | " + serverPacketType.name() + " | " + this.connectionState.getOrDefault(socketAddress, 0) + "]");

            });

            // Send packet later if the IP is not blocked.
            return PacketAction.CANCEL;

        }

        /// Block packets if the login procedure is not followed.

        // Connection procedure is not done yet, block outgoing packets.
        if (this.connectionState.getOrDefault(socketAddress, 0) != (this.pluginCore.isOnlineMode() ? 7 : 5)) {

            // Cancel the packet until the connection procedure is fulfilled.
            return PacketAction.CANCEL;

        }

        // Everything is good!
        return PacketAction.NONE;

    }

}
