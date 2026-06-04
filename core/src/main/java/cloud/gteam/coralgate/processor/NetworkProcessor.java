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
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;

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

        /*
        * SOURCE PORT FILTERING.
        */

        // This is the lowest dynamic port used by Linux.
        // Anything bellow means the port was forced to use that port and is therefore, not a real Minecraft client.
        if (inetSocketAddress.getPort() < 32768) {

            // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid port used by client.");

            // Block further logic.
            return;

        }

        /* Check different condition to trigger a MOTD packet check and blockage. */

        // Match MOTD related packets.
        final boolean isStatusPacket = packetTypeCommon == PacketType.Status.Client.PING
                || packetTypeCommon == PacketType.Status.Client.REQUEST
                || packetTypeCommon == PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING;

        /* Filter only packets that are used to get information about the server. */

        // Handshake is also a MOTD related packet in a certain state.
        if (isStatusPacket || packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            // This is the lowest dynamic port used by Windows & Mac.
            // Since Linux players are "rare", we'll issue a warning statement about them.
            // Alongside that, we will block any server list ping to prevent bots from getting information about the server.
            // (server version, online players, player count...).
            final boolean suspiciousPort = inetSocketAddress.getPort() < 49152;

            // Block the first incoming MOTD related packet of an IP.
            final boolean processMOTD = suspiciousPort
                    || !this.corePlugin.getApiManager().isIpBlockedCache(ipAddress)
                    || !this.corePlugin.getApiManager().isHealthy();

            if (suspiciousPort) {

                // Log this suspicious connection.
                CorePlugin.getLogger().warning("Suspicious port used by client. Keep an eye out for " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + "]");

                // Report the IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

            }

            if (processMOTD) {

                // Packet responsible for the latency showup.
                switch (packetTypeCommon) {

                    case PacketType.Status.Client.PING, PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING -> {

                        // Cancel packet and send a "forged" response.
                        packetReceiveEvent.setCancelled(true);
                        packetReceiveEvent.getUser().sendPacketSilently(new WrapperStatusClientPing(packetReceiveEvent));

                        // Block further logic.
                        return;

                    }


                    // Packet responsible for the MOTD message and server related information (player count, version).
                    case PacketType.Status.Client.REQUEST -> {

                        // Cancel the packet and send a forged generic looking MOTD.
                        packetReceiveEvent.setCancelled(true);
                        packetReceiveEvent.getUser().sendPacketSilently(new WrapperStatusServerResponse(getForgedMOTD()));

                        // Block further logic.
                        return;

                    }


                    // Specific 'STATUS' handshake state.
                    case PacketType.Handshaking.Client.HANDSHAKE -> {

                        final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

                        // 'STATUS' only.
                        if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.STATUS && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.STATUS) {

                            packetReceiveEvent.setCancelled(true);

                            // Block further logic.
                            return;

                        }
                    }

                    default -> {}

                }

            }

        }

        /* Source port is ok, check IP now. */

        /*
        * API IP CHECK.
        */

        // Match every login sequence related packet.
        final boolean isLoginSequencePacket = packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE
                || packetTypeCommon == PacketType.Login.Client.LOGIN_START
                || packetTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE
                || packetTypeCommon == PacketType.Login.Client.LOGIN_SUCCESS_ACK;

        if (isStatusPacket || isLoginSequencePacket) {

            if (this.corePlugin.getApiManager().isHealthy()) {

                this.corePlugin.getApiManager().isIpBlocked(ipAddress).thenAccept(blocked -> {

                    if (blocked)
                        // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                        logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "IP is blocked by the API.");

                });

            }

        }

        /*
        * PACKET ORDER FILTERING.
        */

        // Specific 'LOGIN' handshake login, the first packet in a legitimate connection sequence.
        if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

            if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.LOGIN)
                this.connectionState.put(inetSocketAddress, packetTypeCommon);

            // Block further logic.
            return;

        }

        // Handshake has passed (or skipped).
        if (packetTypeCommon == PacketType.Login.Client.LOGIN_START) {

            if (this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Handshaking.Client.HANDSHAKE) {

                // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing handshake procedure.");

                // Block further logic.
                return;

            }

            final WrapperLoginClientLoginStart wrapperLoginClientLoginStart = new WrapperLoginClientLoginStart(packetReceiveEvent);

            // Filter debug usernames.
            if (wrapperLoginClientLoginStart.getUsername().startsWith("Player")) {

                // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Bot-like username pattern detected.");

                // Block further logic.
                return;

            }

            this.connectionState.put(inetSocketAddress, packetTypeCommon);

            // Block further logic.
            return;

        }

        // After login start has passed and the server sent an encryption request. This is only for servers that are in online mode.
        if (packetTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE && this.corePlugin.isOnlineMode()) {

            // Validate state against certain conditions: previous ENCRYPTION_REQUEST
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Server.ENCRYPTION_REQUEST, packetTypeCommon, "Missing login start procedure.");

            // Block further logic.
            return;

        }

        if (packetTypeCommon == PacketType.Login.Client.LOGIN_SUCCESS_ACK) {

            // Validate state against certain conditions: previous LOGIN_SUCCESS
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Server.LOGIN_SUCCESS, packetTypeCommon, "Missing login success procedure.");

            // Block further logic.
            return;

        }

        final PacketTypeCommon expectedFinalState = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_20_2)
                ? PacketType.Login.Server.LOGIN_SUCCESS
                : PacketType.Login.Client.LOGIN_SUCCESS_ACK;

        if (this.connectionState.getOrDefault(inetSocketAddress, null) != expectedFinalState)
            logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing full connection procedure.");

        /* All checks passed! */

    }

    // Outgoing packets (Server to Client) [S->C]
    public void onPacketSend(final PacketSendEvent packetSendEvent) {

        final InetSocketAddress inetSocketAddress = packetSendEvent.getSocketAddress();
        final String ipAddress = inetSocketAddress.getHostString();

        final PacketTypeCommon packetTypeCommon = packetSendEvent.getPacketType();

        /*
        * PACKET ORDER FILTERING.
        */

        // Whitelisted MOTD related packets.
        if (packetTypeCommon == PacketType.Status.Server.RESPONSE || packetTypeCommon == PacketType.Status.Server.PONG)
            return;

        // Client should have passed login start procedure.
        if (packetTypeCommon == PacketType.Login.Server.ENCRYPTION_REQUEST && this.corePlugin.isOnlineMode()) {

            // Validate state against certain conditions: previous LOGIN_START
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Client.LOGIN_START, packetTypeCommon, "Missing login start procedure.");

            // Block further logic.
            return;

        }

        // Client should have sent the encryption response (if online mode). The compression level can be set to -1 (disabled) on proxies for less network/cpu overhead.
        if (packetTypeCommon == PacketType.Login.Server.SET_COMPRESSION && this.corePlugin.getCompressionThreshold() >= 0) {

            final PacketTypeCommon requiredPacketTypeCommon = this.corePlugin.isOnlineMode()
                    ? PacketType.Login.Client.ENCRYPTION_RESPONSE
                    : PacketType.Login.Client.LOGIN_START;

            // Validate state against certain conditions: previous ENCRYPTION_RESPONSE (if online mode) else LOGIN_START
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing encryption response or login start procedure.");

            // Block further logic.
            return;

        }

        if (packetTypeCommon == PacketType.Login.Server.LOGIN_SUCCESS) {

            final PacketTypeCommon requiredPacketTypeCommon = this.corePlugin.isOnlineMode()
                    ? (this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.ENCRYPTION_RESPONSE)
                    : (this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.LOGIN_START);

            // Validate state against certain conditions: previous SET_COMPRESSION (if above 0) else ENCRYPTION_RESPONSE (if online mode) else LOGIN_START
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing set compression procedure.");

            // Block further logic.
            return;

        }

        // Get the proper login packet based off the client version.
        final PacketTypeCommon requiredPacketTypeCommon = packetSendEvent.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_2)
                ? PacketType.Login.Client.LOGIN_SUCCESS_ACK
                : PacketType.Login.Server.LOGIN_SUCCESS;

        // Connection procedure is not done yet, block outgoing packets.
        if (this.connectionState.getOrDefault(inetSocketAddress, null) != requiredPacketTypeCommon)
            packetSendEvent.setCancelled(true);

        /* All checks passed! */

    }

    private void logAndClose(final PacketReceiveEvent packetReceiveEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason) {

        CorePlugin.getLogger().severe(reason + " Closing connection from " + inetSocketAddress + ". [C->S | " + packetTypeCommon.getName() + "]");
        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetReceiveEvent.setCancelled(true);
        packetReceiveEvent.getUser().closeConnection();

    }

    private void logAndClose(final PacketSendEvent packetSendEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason) {

        CorePlugin.getLogger().severe(reason + " Closing connection from " + inetSocketAddress + ". [S->C | " + packetTypeCommon.getName() + "]");

        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetSendEvent.setCancelled(true);
        packetSendEvent.getUser().closeConnection();

    }

    private void verifyAndTransitionState(final PacketReceiveEvent packetReceiveEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final PacketTypeCommon requiredState, final PacketTypeCommon targetState, final String reason) {

        if (this.connectionState.getOrDefault(inetSocketAddress, null) == requiredState) {

            this.connectionState.put(inetSocketAddress, targetState);

        } else logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason);

    }

    private void verifyAndTransitionState(final PacketSendEvent packetSendEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final PacketTypeCommon requiredState, final PacketTypeCommon targetState, final String reason) {

        if (this.connectionState.getOrDefault(inetSocketAddress, null) == requiredState) {

            this.connectionState.put(inetSocketAddress, targetState);

        } else logAndClose(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason);

    }

    private String getForgedMOTD() {
        return "{\"description\":{\"text\":\"\",\"extra\":[\"A Minecraft Server\"]},\"players\":{\"max\":20,\"online\":0},\"version\":{\"name\":\"CraftBukkit 26.1.1\",\"protocol\":775},\"enforcesSecureChat\":true}";
    }

}
