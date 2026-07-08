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
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkProcessor implements PacketListener {

    private final CorePlugin corePlugin;

    private final ConcurrentHashMap<SocketAddress, PacketTypeCommon> connectionState = new ConcurrentHashMap<>();

    public NetworkProcessor(final CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
    }

    // Incoming packets (Client -> Server) [C->S].
    public void onPacketReceive(final PacketReceiveEvent packetReceiveEvent) {

        final InetSocketAddress inetSocketAddress = packetReceiveEvent.getSocketAddress();

        // Exempt local IP addresses according to configuration file.
        if (exemptLocalIpAddress(inetSocketAddress)) {
            return;
        }

        final String ipAddress = inetSocketAddress.getHostString();
        final User packetUser = packetReceiveEvent.getUser();
        final PacketTypeCommon packetTypeCommon = packetReceiveEvent.getPacketType();

        // Match MOTD related packets.
        final boolean isMOTDPacket = packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE
                || packetTypeCommon == PacketType.Status.Client.REQUEST
                || packetTypeCommon == PacketType.Status.Client.PING
                || packetTypeCommon == PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING;

        // Match login sequence related packets.
        final boolean isLoginSequencePacket = packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE
                || packetTypeCommon == PacketType.Login.Client.LOGIN_START
                || packetTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE
                || packetTypeCommon == PacketType.Login.Client.LOGIN_SUCCESS_ACK;

        boolean isInvalidProtocol = false;

        // Check client's protocol & client version.
        // Only match MOTD/login sequence related packets to save resources.
        if (isMOTDPacket || isLoginSequencePacket) {

            // If the client's version/protocol version is invalid.
            //noinspection ConstantValue
            if (packetReceiveEvent.getUser().getClientVersion() == null || packetReceiveEvent.getUser().getClientVersion().getProtocolVersion() <= 4) {

                // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid protocol version.", !isMOTDPacket);

                // To send fake MOTD later down the line.
                isInvalidProtocol = true;

                // Block further logic.
                if (!isMOTDPacket) return;

            }

        }

        // This is the lowest dynamic port used by Windows & Mac.
        // Since Linux players are "rare", we'll issue a warning statement about them.
        // Alongside that, we will block any server list ping to prevent bots from getting information about the server.
        // (server version, online players, player count...).
        final boolean isSuspiciousPort = inetSocketAddress.getPort() < 49152;

        // This is the lowest dynamic port used by Linux.
        // Anything bellow means the port was forcefully used and is therefore not a real Minecraft client.
        final boolean isInvalidPort = inetSocketAddress.getPort() < 32768;

        // Englobe suspicious port (which also includes invalid port) and bad protocol version.
        final boolean isBadPacket = isSuspiciousPort || isInvalidProtocol;

        // Connection initialization for both MOTD and login procedures. (cross versions, cross-platform).
        if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            this.connectionState.put(inetSocketAddress, packetTypeCommon);

            final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

            // Drop the packet if it's suspicious and if it's a STATUS packet.
            if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.STATUS
                    && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.STATUS) {

                if (isBadPacket) {

                    // Decide reason based on port.
                    String reason = isInvalidPort
                            ? "Invalid port used by client."
                            : "Suspicious port used by client.";

                    // Decide reason based on invalid protocol (or port).
                    reason = isInvalidProtocol
                            ? "Invalid protocol version."
                            : reason;

                    // Don't close the connection yet.
                    log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, false);

                }

            // Drop the packet if it's suspicious, close connection if it's invalid.
            // Only if it's a LOGIN packet.
            } else if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN
                    && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.LOGIN) {

                // Close the connection.
                if (isInvalidPort || isInvalidProtocol) {

                    // Decide reason based off if the port is invalid or if the protocol version is.
                    final String reason = isInvalidPort
                            ? "Invalid port used by client."
                            : "Invalid protocol version.";

                    // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                    log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, true);

                // Log an alert but don't close the connection.
                } else if (isSuspiciousPort) {

                    // Don't cancel the packet else the LOGIN_START procedure fails.
                    CorePlugin.getLogger().warning("Suspicious port used by client. Keep an eye on " + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");

                }

            }

            // Block further logic.
            return;

        }

        // Request to get server's information (MOTD, version, player count).
        if (packetTypeCommon == PacketType.Status.Client.REQUEST) {

            final boolean badHandshake = this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Handshaking.Client.HANDSHAKE;
            final boolean sendForgedMOTD = isBadPacket || badHandshake;

            // Either suspicious port or bad handshake.
            if (sendForgedMOTD) {

                // Send forged MOTD.
                packetUser.sendPacketSilently(new WrapperStatusServerResponse(getForgedMOTD()));

                // Custom logging based off port number.
                //noinspection ExtractMethodRecommender
                String reason = isInvalidPort
                        ? "Invalid port used by client."
                        : "Suspicious port used by client.";

                // Custom logging based off the factor (suspicious port/bad handshake).
                reason = badHandshake
                        ? "Missing proper handshake."
                        : reason;

                // Custom logging based off if the protocol version is bad.
                reason = isInvalidProtocol
                        ? "Invalid protocol version."
                        : reason;

                // Log the violation, report the IP to CoralGate API, cancel the packet and send forged MOTD.
                log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, false);

            }

            // Block further logic.
            return;

        }

        // Ping to get the latency between the client and the server.
        // Doesn't really expose any server information, but we'll keep it under control.
        if (packetTypeCommon == PacketType.Status.Client.PING) {

            final boolean badHandshake = this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Handshaking.Client.HANDSHAKE;
            final boolean sendForgedPong = isBadPacket || badHandshake;

            // Either suspicious port or bad handshake.
            if (sendForgedPong) {

                // Send forged pong.
                packetUser.sendPacketSilently(new WrapperStatusServerPong(new WrapperStatusClientPing(packetReceiveEvent).getTime()));

                // Custom logging based off port number.
                //noinspection ExtractMethodRecommender
                String reason = isInvalidPort
                        ? "Invalid port used by client."
                        : "Suspicious port used by client.";

                // Custom logging based off the factor (suspicious port/bad handshake).
                reason = badHandshake
                        ? "Missing proper handshake."
                        : reason;

                // Custom logging based off if the protocol version is bad.
                reason = isInvalidProtocol
                        ? "Invalid protocol version."
                        : reason;

                // Log the violation, report the IP to CoralGate API, cancel the packet and send forged pong.
                log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, false);

            }

            // Block further logic.
            return;

        }

        // TODO: find some way to block this cuz PE ain't seeing the goddamn packet
        // TODO: Netty pipeline injection?
        // Legacy ping exposes information like MOTD, version & player count.
        // No wrapper exists for it so we'll have to drop the packet.
        if (packetTypeCommon == PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING) {

            // Cancel the packet.
            packetReceiveEvent.setCancelled(true);

            // Block further logic.
            return;

        }

        // Handle invalid port incoming connections.
        if (isInvalidPort) {

            // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid port used by client.", true);

            // Block further logic.
            return;

        }

        // Login start procedure after handshake has been established.
        if (packetTypeCommon == PacketType.Login.Client.LOGIN_START) {

            // Validate state against certain conditions: previous HANDSHAKE.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Handshaking.Client.HANDSHAKE, packetTypeCommon, "Missing proper handshake.");

            final WrapperLoginClientLoginStart wrapperLoginClientLoginStart = new WrapperLoginClientLoginStart(packetReceiveEvent);

            // Filter debug usernames.
            if (wrapperLoginClientLoginStart.getUsername().startsWith("Player")) {

                // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Bot-like username pattern detected.", true);

            }

            // Block further logic.
            return;

        }

        // After login start has passed and the server sent an encryption request. This is only for servers that are in online mode.
        if (packetTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE && this.corePlugin.isOnlineMode()) {

            // Validate state against certain conditions: previous ENCRYPTION_REQUEST.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Server.ENCRYPTION_REQUEST, packetTypeCommon, "Missing login start procedure.");

            // Block further logic.
            return;

        }

        // The client acknowledging the server sent LOGIN_SUCCESS. This only applies for 1.20.2+ clients and 1.20.2+ servers (if using Via).
        if (packetTypeCommon == PacketType.Login.Client.LOGIN_SUCCESS_ACK) {

            // Validate state against certain conditions: previous LOGIN_SUCCESS.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Server.LOGIN_SUCCESS, packetTypeCommon, "Missing login success procedure.");

            // Block further logic.
            return;

        }

        // Decide if we should use the new LOGIN_SUCCESS_ACK (1.20.2+) or the older LOGIN_SUCCESS (< 1.20.2).
        // If we are on a backend, the server version is straight forward to get.
        // However, if we're on a proxy, the server version is marked as 1.8 since it's the lowest Bungee supports (when it could be anything else).
        // Therefore, we should base ourselves off the client's version ONLY if we are running on a proxy.
        final boolean useLoginSuccessAck = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)
                || (this.corePlugin.getPlatformProperties().getProperty("platform-type").equals("proxy")
                && packetUser.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_2));

        final PacketTypeCommon expectedFinalState = useLoginSuccessAck
                ? PacketType.Login.Client.LOGIN_SUCCESS_ACK
                : PacketType.Login.Server.LOGIN_SUCCESS;

        // Connection procedure is not done yet, block incoming packets.
        if (this.connectionState.getOrDefault(inetSocketAddress, null) != expectedFinalState) {
            log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing full connection procedure.", true);
        }

        /* All checks passed! */

    }

    // Outgoing packets (Server to Client) [S->C]
    public void onPacketSend(final PacketSendEvent packetSendEvent) {

        final InetSocketAddress inetSocketAddress = packetSendEvent.getSocketAddress();

        // Exempt local IP addresses according to configuration file.
        if (exemptLocalIpAddress(inetSocketAddress)) {
            return;
        }

        final String ipAddress = inetSocketAddress.getHostString();
        final PacketTypeCommon packetTypeCommon = packetSendEvent.getPacketType();

        // Whitelisted packets.
        if (packetTypeCommon == PacketType.Status.Server.RESPONSE || packetTypeCommon == PacketType.Status.Server.PONG) {
            return;
        }

        // Client should have passed login start procedure.
        if (packetTypeCommon == PacketType.Login.Server.ENCRYPTION_REQUEST && this.corePlugin.isOnlineMode()) {

            // Validate state against certain conditions: previous LOGIN_START.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Client.LOGIN_START, packetTypeCommon, "Missing login start procedure.");

            // Block further logic.
            return;

        }

        // Client should have sent the encryption response (if online mode).
        // The compression level can be set to -1 (disabled) on proxies for less network/cpu overhead.
        if (packetTypeCommon == PacketType.Login.Server.SET_COMPRESSION && this.corePlugin.getCompressionThreshold() >= 0) {

            // Offline servers do not have encryption, the previous packet is therefore simply LOGIN_START.
            final PacketTypeCommon requiredPacketTypeCommon = this.corePlugin.isOnlineMode()
                    ? PacketType.Login.Client.ENCRYPTION_RESPONSE
                    : PacketType.Login.Client.LOGIN_START;

            // Determine proper message based off previous statement.
            final String reason = requiredPacketTypeCommon == PacketType.Login.Client.ENCRYPTION_RESPONSE
                    ? "encryption response."
                    : "login start procedure.";

            // Validate state against certain conditions: previous ENCRYPTION_RESPONSE (if online mode) else LOGIN_START.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing " + reason);

            // Block further logic.
            return;

        }

        if (packetTypeCommon == PacketType.Login.Server.LOGIN_SUCCESS) {

            // If it's an online server with compression: SET_COMPRESSION.
            // If it's an online server WITHOUT compression: ENCRYPTION_RESPONSE.
            //noinspection ExtractMethodRecommender
            final PacketTypeCommon onlineModePreviousStatus = this.corePlugin.getCompressionThreshold() >= 0
                    ? PacketType.Login.Server.SET_COMPRESSION
                    : PacketType.Login.Client.ENCRYPTION_RESPONSE;

            // If it's an offline server with compression: SET_COMPRESSION.
            // If it's an offline server WITHOUT compression: LOGIN_START.
            final PacketTypeCommon offlineModePreviousStatus = this.corePlugin.getCompressionThreshold() >= 0
                    ? PacketType.Login.Server.SET_COMPRESSION
                    : PacketType.Login.Client.LOGIN_START;

            // Choose required packet type based off previous statements.
            final PacketTypeCommon requiredPacketTypeCommon = this.corePlugin.isOnlineMode()
                    ? onlineModePreviousStatus
                    : offlineModePreviousStatus;

            // Validate state against certain conditions: previous SET_COMPRESSION (if above 0) else ENCRYPTION_RESPONSE (if online mode) else LOGIN_START.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing set compression procedure.");

            // Block further logic.
            return;

        }

        // Fired when a connection is closed.
        if (packetTypeCommon == PacketType.Login.Server.DISCONNECT) {

            final WrapperLoginServerDisconnect wrapperLoginServerDisconnect = new WrapperLoginServerDisconnect(packetSendEvent);

            final Component disconnectReason = wrapperLoginServerDisconnect.getReason();
            final String serverVersion = PacketEvents.getAPI().getServerManager().getVersion().getReleaseName();

            if (disconnectReason instanceof TextComponent) {

                final TextComponent disconnectReasonTextComponent = (TextComponent) disconnectReason;
                final String disconnectReasonString = disconnectReasonTextComponent.content();

                // Intercept "Outdated server! I'm still on X.XX.X" messages to not show the server's version.
                if (disconnectReasonString.contains(serverVersion)) {

                    // Replace the real server's version by the spoofed one.
                    final String newDisconnectReason = disconnectReasonString.replace(serverVersion, "26.2");

                    // Reconstruct reason with spoofed server version.
                    final Component safeDisconnectReason = Component.text()
                            .content(newDisconnectReason)
                            .style(disconnectReasonTextComponent.style())
                            .build();

                    // Set the new reason and tell packetevents to re-encode the packet and send it.
                    wrapperLoginServerDisconnect.setReason(safeDisconnectReason);

                    packetSendEvent.markForReEncode(true);

                }

            }

            // Block further logic.
            return;

        }

        // Decide if we should use the new LOGIN_SUCCESS_ACK (1.20.2+) or the older LOGIN_SUCCESS (< 1.20.2).
        // If we are on a backend, the server version is straight forward to get.
        // However, if we're on a proxy, the server version is marked as 1.8 since it's the lowest Bungee supports (when it could be anything else).
        // Therefore, we should base ourselves off the client's version ONLY if we are running on a proxy.
        final boolean useLoginSuccessAck = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)
                || (this.corePlugin.getPlatformProperties().getProperty("platform-type").equals("proxy")
                && packetSendEvent.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_2));

        final PacketTypeCommon expectedFinalState = useLoginSuccessAck
                ? PacketType.Login.Client.LOGIN_SUCCESS_ACK
                : PacketType.Login.Server.LOGIN_SUCCESS;

        // Connection procedure is not done yet, block incoming packets.
        if (this.connectionState.getOrDefault(inetSocketAddress, null) != expectedFinalState) {
            log(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing full connection procedure.");
        }

        /* All checks passed! */

    }

    @Override
    public void onUserDisconnect(final UserDisconnectEvent userDisconnectEvent) {

        final User user = userDisconnectEvent.getUser();
        //noinspection ConstantValue
        if (user == null || user.getAddress() == null) {
            return;
        }

        // Clean up.
        this.connectionState.remove(user.getAddress());

    }

    private void log(final PacketReceiveEvent packetReceiveEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason, final boolean closeConnection) {

        final String messageComplement = closeConnection
                ? " Closing connection from "
                : " Dropping packet from ";

        // Log based if the connection should be closed or not.
        if (closeConnection) {
            CorePlugin.getLogger().severe(reason + messageComplement + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");
        } else {
            CorePlugin.getLogger().warning(reason + messageComplement + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");
        }

        // Report IP to CoralGate's API.
        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetReceiveEvent.setCancelled(true);
        if (closeConnection) {
            packetReceiveEvent.getUser().closeConnection();
        }

        // Clean up.
        this.connectionState.remove(inetSocketAddress);

    }

    private void log(final PacketSendEvent packetSendEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason) {

        // Log based if the connection should be closed or not.
        CorePlugin.getLogger().severe(reason + " Closing connection from " + inetSocketAddress + ". [S->C | " + packetSendEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");

        // Report IP to CoralGate's API.
        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetSendEvent.setCancelled(true);
        packetSendEvent.getUser().closeConnection();

        // Clean up.
        this.connectionState.remove(inetSocketAddress);

    }

    private void verifyAndTransitionState(final PacketReceiveEvent packetReceiveEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final PacketTypeCommon requiredState, final PacketTypeCommon targetState, final String reason) {

        if (this.connectionState.getOrDefault(inetSocketAddress, null) == requiredState) {

            this.connectionState.put(inetSocketAddress, targetState);

        } else {

            log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, true);

        }

    }

    private void verifyAndTransitionState(final PacketSendEvent packetSendEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final PacketTypeCommon requiredState, final PacketTypeCommon targetState, final String reason) {

        if (this.connectionState.getOrDefault(inetSocketAddress, null) == requiredState) {

            this.connectionState.put(inetSocketAddress, targetState);

        } else {

            log(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason);

        }

    }

    private String getForgedMOTD() {
        return "{\"description\":{\"text\":\"\",\"extra\":[\"A Minecraft Server\"]},\"players\":{\"max\":20,\"online\":0},\"version\":{\"name\":\"Paper 26.2\",\"protocol\":776},\"enforcesSecureChat\":true}";
    }

    private boolean exemptLocalIpAddress(final InetSocketAddress inetSocketAddress) {

        // Exempt local IP addresses according to configuration file.
        if (this.corePlugin.getConfigManager().getConfig().isIgnoreLocalAddresses() && !this.corePlugin.isTestMode()) {

            final InetAddress inetAddress = inetSocketAddress.getAddress();
            return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress();

        }

        return false;

    }

}
