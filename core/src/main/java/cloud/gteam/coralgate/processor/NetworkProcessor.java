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

package cloud.gteam.coralgate.processor;

import cloud.gteam.coralgate.CorePlugin;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkProcessor implements PacketListener {

    private final CorePlugin corePlugin;

    private final ConcurrentHashMap<SocketAddress, PacketTypeCommon> connectionState = new ConcurrentHashMap<>();

    public NetworkProcessor(final CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    // Incoming packets (Client -> Server) [C->S]
    public void onPacketReceive(final PacketReceiveEvent packetReceiveEvent) {

        final InetSocketAddress inetSocketAddress = packetReceiveEvent.getSocketAddress();
        final String ipAddress = inetSocketAddress.getHostString();

        final PacketTypeCommon packetTypeCommon = packetReceiveEvent.getPacketType();

        /// Port filtering.

        // This is the lowest dynamic port used by Linux.
        // Anything bellow means the port was forced to use that port and is therefore, not a real Minecraft client.
        if (inetSocketAddress.getPort() < 32768) {

            // TODO: Add config to "silent" those messages.
            CorePlugin.getLogger().severe("Invalid port used by client. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + "]");

            // Report IP to CoralGate API.
            this.corePlugin.getApiManager().reportIp(ipAddress);

            // Cancel the packet and close the connection.
            packetReceiveEvent.getUser().closeConnection();
            packetReceiveEvent.setCancelled(true);
            return;

        // The port is ok, continue.
        } else if (packetTypeCommon == PacketType.Status.Client.PING || packetTypeCommon == PacketType.Status.Client.REQUEST || packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            // This is the lowest dynamic port used by Windows & Mac.
            // Since Linux players are "rare", we'll issue a warning statement about them.
            // Alongside that, we will block any server list ping to prevent bots from getting information about the server.
            // (server version, online players, player count...).
            if (inetSocketAddress.getPort() < 49152) {

                // Log about this suspicious connection.
                CorePlugin.getLogger().warning("Suspicious port used by client. Keep an eye out for " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + "]");

                // Handshake logic is handled bellow.
                if (packetTypeCommon != PacketType.Handshaking.Client.HANDSHAKE) {

                    packetReceiveEvent.setCancelled(true);
                    return;

                }

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

            }

            if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

                final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

                // Filter only 'STATUS' to not block logins.
                if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.STATUS && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.STATUS) {

                    // Prevent suspicious ports from getting 'STATUS'.
                    if (inetSocketAddress.getPort() < 49152) {

                        packetReceiveEvent.setCancelled(true);

                    }

                    // Block further logic.
                    return;

                }

            // Run RESPONSE | PING logic.
            } else {

                // Check if IP is not blocked and not in cache. This blocks first ping.
                if (!this.corePlugin.getApiManager().isIpBlockedCache(ipAddress) || !this.corePlugin.getApiManager().isHealthy()) packetReceiveEvent.setCancelled(true);

                // Block processing if the API is unhealthy (down).
                if (!this.corePlugin.getApiManager().isHealthy()) return;

                // Don't send back the packet if the IP is blocked by the API.
                this.corePlugin.getApiManager().isIpBlocked(ipAddress).thenAccept(blocked -> {

                    if (blocked) {

                        // Log blocked ip.
                        CorePlugin.getLogger().severe("IP is blocked by the API. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                        // Cancel the packet and close the connection.
                        packetReceiveEvent.setCancelled(true);
                        packetReceiveEvent.getUser().closeConnection();

                    }

                });

                // Block further logic.
                return;

            }

        }

        /// Proper login procedure checking.

        // First packet fired when a client initiates a connection.
        if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

            // Client login.
            if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.LOGIN) {

                //CorePlugin.getLogger().info("Version " + wrapperHandshakingClientHandshake.getClientVersion() + " for " + ipAddress); TODO: remove debug

                // First connection step.
                this.connectionState.put(inetSocketAddress, packetTypeCommon);

            }

            // Block further logic.
            return;

        // After handshake has passed.
        } else if (packetTypeCommon == PacketType.Login.Client.LOGIN_START) {

            // Checking if handshake has passed successfully.
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == PacketType.Handshaking.Client.HANDSHAKE) {

                final WrapperLoginClientLoginStart wrapperLoginClientLoginStartMappings = new WrapperLoginClientLoginStart(packetReceiveEvent);

                // Check for obvious Bot names, most of the time "Player".
                if (wrapperLoginClientLoginStartMappings.getUsername().equalsIgnoreCase("Player")) {

                    // Log bot looking name.
                    CorePlugin.getLogger().severe("Bot looking name detected. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                    // Report IP to CoralGate API.
                    this.corePlugin.getApiManager().reportIp(ipAddress);

                    // Cancel the packet and close the connection.
                    packetReceiveEvent.getUser().closeConnection();
                    packetReceiveEvent.setCancelled(true);
                    return;

                // If the handshake and login procedure are good while not having a bot looking name, continue.
                } else {

                    // Check if the IP is blocked by the API.
                    try {

                        if (this.corePlugin.getApiManager().isIpBlocked(ipAddress).get()) {

                            // Log blocked ip.
                            CorePlugin.getLogger().severe("IP is blocked by the API. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                            packetReceiveEvent.getUser().closeConnection();
                            packetReceiveEvent.setCancelled(true);
                            return;

                        }

                    } catch (final Exception e) {
                        CorePlugin.getLogger().severe("Couldn't fetch the blocked status of an IP. Is the API down? See error: " + e.getMessage());
                    }

                    this.connectionState.put(inetSocketAddress, packetTypeCommon);

                }

            // User has skipped the proper handshake.
            } else {

                // Log invalid handshake procedure.
                CorePlugin.getLogger().severe("Missing handshake procedure. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetReceiveEvent.getUser().closeConnection();
                packetReceiveEvent.setCancelled(true);
                return;

            }

            // Block further logic.
            return;

        // After login start has passed. This is only for servers that are in online mode.
        } else if (packetTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE && this.corePlugin.isOnlineMode()) {

            // Checking if encryption request has passed successfully.
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == PacketType.Login.Server.ENCRYPTION_REQUEST) {

                //final WrapperLoginClientEncryptionResponse wrapperLoginClientEncryptionResponse = new WrapperLoginClientEncryptionResponse(packetReceiveEvent);

                // TODO: Check stuff with the encryption keys.

                // Everything was validated, continue.
                this.connectionState.put(inetSocketAddress, packetTypeCommon);

            // User has skipped the proper login start.
            } else {

                // Log invalid handshake procedure.
                CorePlugin.getLogger().severe("Missing login start procedure. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetReceiveEvent.getUser().closeConnection();
                packetReceiveEvent.setCancelled(true);
                return;

            }

            // Block further logic.
            return;

        // After everything was completed, the server lets them in and the user finalizes their connection.
        } else if (packetTypeCommon == PacketType.Login.Client.LOGIN_SUCCESS_ACK) {

            // Checking if login success and encryption response passed successfully (or not if server is not in online mode).
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == PacketType.Login.Server.LOGIN_SUCCESS) {

                // Everything was validated, continue.
                this.connectionState.put(inetSocketAddress, PacketType.Login.Client.LOGIN_SUCCESS_ACK);

            // User has skipped the encryption response (if the server is in online mode) or the server did not send login success.
            } else {

                // Log invalid encryption response procedure.
                CorePlugin.getLogger().severe("Missing login success procedure. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetReceiveEvent.getUser().closeConnection();
                packetReceiveEvent.setCancelled(true);
                return;

            }

            // Block further logic.
            return;

        }

        /// Block packets if the login procedure is not followed.

        // The player (most likely bot) didn't follow the proper login procedure, close their connection.
        // Still need to account for the no encryption response on non-online mode servers.
        if (this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Login.Client.LOGIN_SUCCESS_ACK) {

            // Log invalid encryption response procedure.
            CorePlugin.getLogger().severe("Missing full connection procedure. Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

            // Report IP to CoralGate API.
            this.corePlugin.getApiManager().reportIp(ipAddress);

            // Cancel the packet and close the connection.
            packetReceiveEvent.getUser().closeConnection();
            packetReceiveEvent.setCancelled(true);

        }

        // Everything is good!

    }

    // Outgoing packets (Server to Client) [S->C]
    public void onPacketSend(final PacketSendEvent packetSendEvent) {

        final InetSocketAddress inetSocketAddress = packetSendEvent.getSocketAddress();
        final String ipAddress = inetSocketAddress.getHostString();

        final PacketTypeCommon packetTypeCommon = packetSendEvent.getPacketType();

        /// Proper login procedure checking.

        // Client should have passed login start procedure.
        if (packetTypeCommon == PacketType.Login.Server.ENCRYPTION_REQUEST && this.corePlugin.isOnlineMode()) {

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == PacketType.Login.Client.LOGIN_START) {

                this.connectionState.put(inetSocketAddress, PacketType.Login.Server.ENCRYPTION_REQUEST);

            } else {

                // Log invalid encryption response procedure.
                CorePlugin.getLogger().severe("Missing login start procedure. Closing connection from " + inetSocketAddress + ". [S->C | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetSendEvent.setCancelled(true);
                packetSendEvent.getUser().closeConnection();
                return;

            }

            // Block further logic.
            return;

        // Client should have sent the encryption response (if online mode).
        } else if (packetTypeCommon == PacketType.Login.Server.SET_COMPRESSION && this.corePlugin.getCompressionThreshold() >= 0) {

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == (this.corePlugin.isOnlineMode() ? PacketType.Login.Client.ENCRYPTION_RESPONSE : PacketType.Login.Client.LOGIN_START)) {

                this.connectionState.put(inetSocketAddress, PacketType.Login.Server.SET_COMPRESSION);

            } else {

                // Log invalid encryption response procedure.
                CorePlugin.getLogger().severe("Missing encryption response or login start procedure. Closing connection from " + inetSocketAddress + ". [S->C | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetSendEvent.setCancelled(true);
                packetSendEvent.getUser().closeConnection();
                return;

            }

            // Block further logic.
            return;

        // Server should have set compression.
        } else if (packetTypeCommon == PacketType.Login.Server.LOGIN_SUCCESS) {

            // Establish the expected packet type.
            final PacketTypeCommon expectedPacketTypeCommon = (this.corePlugin.isOnlineMode() ? this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.ENCRYPTION_RESPONSE
                    : this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.LOGIN_START);

            // Check if it did. If yes, continue.
            if (this.connectionState.getOrDefault(inetSocketAddress, null) == expectedPacketTypeCommon) {

                this.connectionState.put(inetSocketAddress, PacketType.Login.Server.LOGIN_SUCCESS);

            } else {

                // Log invalid encryption response procedure.
                CorePlugin.getLogger().severe("Missing set compression procedure. Closing connection from " + inetSocketAddress + ". [S->C | " + packetTypeCommon.getName() + " | " + this.connectionState.getOrDefault(inetSocketAddress, null) + "]");

                // Report IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Cancel the packet and close the connection.
                packetSendEvent.setCancelled(true);
                packetSendEvent.getUser().closeConnection();
                return;

            }

            // Block further logic.
            return;

        }

        // Whitelisted packets that should not be blocked, even if procedure is not complete.
        if (packetTypeCommon == PacketType.Status.Server.RESPONSE || packetTypeCommon == PacketType.Status.Server.PONG) {
            return;
        }

        /// Block packets if the login procedure is not followed.

        // Connection procedure is not done yet, block outgoing packets.
        if (this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Login.Client.LOGIN_SUCCESS_ACK) {

            // Cancel the packet until the connection procedure is fulfilled.
            packetSendEvent.setCancelled(true);

        }

        // Everything is good!

    }

}
