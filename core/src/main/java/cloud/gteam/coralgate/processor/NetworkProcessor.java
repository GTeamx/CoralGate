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
import cloud.gteam.coralgate.injector.NettyResponder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerDisconnect;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
                || packetTypeCommon == PacketType.Status.Client.PING;

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
            if (packetReceiveEvent.getUser().getClientVersion() == null
                    || packetReceiveEvent.getUser().getClientVersion().getProtocolVersion() < ClientVersion.getOldest().getProtocolVersion()
                    || packetReceiveEvent.getUser().getClientVersion().getProtocolVersion() > ClientVersion.getLatest().getProtocolVersion()) {

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

            // Forcefully check the IP so it can receive the real MOTD next time (if it's legit/safe).
            // This also allows us to "pre cache" when the client will actually join the server.
            this.corePlugin.getApiManager().checkIp(ipAddress);

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

                    // Don't cancel the packet else the REQUEST/PING fail.
                    CorePlugin.getLogger().warning(reason + " Keep an eye on " + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");

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
            final boolean sendForgedMOTD = isBadPacket
                    || badHandshake
                    || (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage() && !this.corePlugin.getApiManager().isIpCached(ipAddress)) // Force forged MOTD for IPs that are not yet processed by the API.
                    || this.corePlugin.getApiManager().isIpCachedBlocked(ipAddress); // Send forged MOTD if the IP is blocked by the API.

            // Either suspicious port or bad handshake.
            if (sendForgedMOTD) {

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

                // Send forged MOTD.
                packetUser.sendPacketSilently(new WrapperStatusServerResponse(getForgedMOTD()));

            }

            // Block further logic.
            return;

        }

        // Ping to get the latency between the client and the server.
        // Doesn't really expose any server information, but we'll keep it under control.
        if (packetTypeCommon == PacketType.Status.Client.PING) {

            final boolean badHandshake = this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Handshaking.Client.HANDSHAKE;
            final boolean sendForgedPong = isBadPacket
                    || badHandshake
                    || (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage() && !this.corePlugin.getApiManager().isIpCached(ipAddress)) // Force forged pong for IPs that are not yet processed by the API.
                    || this.corePlugin.getApiManager().isIpCachedBlocked(ipAddress); // Send forged pong if the IP is blocked by the API.

            // Either suspicious port or bad handshake.
            if (sendForgedPong) {

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

                // Send forged pong.
                packetUser.sendPacketSilently(new WrapperStatusServerPong(new WrapperStatusClientPing(packetReceiveEvent).getTime()));

            }

            // Block further logic.
            return;

        }

        // Legacy ping exposes information like MOTD, version & player count.
        // We have to use the NettyResponder to send packet a forged (or not) response.
        if (packetTypeCommon == PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING) {

            // Do not use "isBadPacket", it includes "isInvalidProtocol". Since we don't allow < 1.7 and this packet is for older netty-less versions, it will trigger "isInvalidProtocol".
            // Simply use "isSuspiciousPort" part of "isBadPacket".
            final boolean sendForgedPong = isSuspiciousPort
                    || (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage() && !this.corePlugin.getApiManager().isIpCached(ipAddress)) // Force forged pong for IPs that are not yet processed by the API.
                    || this.corePlugin.getApiManager().isIpCachedBlocked(ipAddress); // Send forged pong if the IP is blocked by the API.

            // Suspicious port.
            if (sendForgedPong) {

                // Custom logging based off port number.
                final String reason = isInvalidPort
                        ? "Invalid port used by client."
                        : "Suspicious port used by client.";

                // Log the violation, report the IP to CoralGate API, cancel the packet and send forged response.
                log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, reason, false);

                // This is for older legacy pings.
                final boolean isOldLegacyPing = ByteBufHelper.readableBytes(packetReceiveEvent.getByteBuf()) == 1;

                final NettyResponder nettyResponder = this.corePlugin.getNettyResponder();

                // Send forged response using NettyResponder.
                if (isOldLegacyPing) {
                    nettyResponder.sendOldLegacyPingResponse(packetUser, "A Minecraft Server", 0, 20);
                } else {
                    nettyResponder.sendLegacyPingResponse(packetUser, 774, "1.21.11", "A Minecraft Server", 0, 20);
                }

            }

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

            // This only works on Spigot/Paper due to a bug on BungeeCord/Velocity with packetevents.
            // Proxies pass the API check when reaching "LOGIN_SUCCESS (S->C). It's the only place where it will work like LOGIN_START.
            // Note: this does cause information about ENCRYPTION_REQUEST and SET_COMPRESSION to leak towards the player...
            // See issues: https://github.com/GTeamX/CoralGate/issues/34 and https://github.com/retrooper/packetevents/issues/1465
            // TODO: find better solution/fix PE issue
            if (!PacketEvents.getAPI().getInjector().isProxy()) {

                // The state validation failed, no need to run a check against the API.
                if (packetReceiveEvent.isCancelled()) {
                    return; // Block further logic.
                }

                // Either if health checking is disabled or if the API is truly healthy.
                if (!this.corePlugin.getConfigManager().getConfig().isApiHealthCheck() || this.corePlugin.getApiManager().isHealthy()) {

                    // Cancel the packet to send it later.
                    packetReceiveEvent.setCancelled(true);

                    // Cache wrapper and data to reconstruct and send it back later.
                    final WrapperLoginClientLoginStart wrapperLoginClientLoginStart = new WrapperLoginClientLoginStart(packetReceiveEvent);

                    // Cache username, it's being used twice. Save some CPU for the rest of us!!!1111!!1!1!
                    final String username = wrapperLoginClientLoginStart.getUsername();

                    // Filter debug usernames.
                    if (username.startsWith("Player")) {

                        // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                        log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Bot-like username pattern detected.", true);

                        // The connection is closed and the packet dropped, no need to run a check against the API.
                        // Block further logic.
                        return;

                    }

                    // Fetch API async.
                    this.corePlugin.getApiManager().isIpBlocked(ipAddress).thenAccept(blocked -> {

                        if (blocked) {

                            // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                            log(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "IP is blocked by the API.", true);

                        } else {

                            // Process the packet again, the player is verified by the API.
                            packetUser.receivePacketSilently(new WrapperLoginClientLoginStart(wrapperLoginClientLoginStart.getClientVersion(), username, wrapperLoginClientLoginStart.getSignatureData().orElse(null), wrapperLoginClientLoginStart.getPlayerUUID().orElse(null)));

                        }

                    });

                }

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
                || (PacketEvents.getAPI().getInjector().isProxy()
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

            // This fix is really only for proxies. The API check run way earlier for the Spigot/Paper versions.
            if (PacketEvents.getAPI().getInjector().isProxy()) {

                // The state validation failed, no need to run a check against the API.
                if (packetSendEvent.isCancelled()) {
                    return; // Block further logic.
                }

                // Either if health checking is disabled or if the API is truly healthy.
                if (!this.corePlugin.getConfigManager().getConfig().isApiHealthCheck() || this.corePlugin.getApiManager().isHealthy()) {

                    // Cancel the packet to send it later.
                    packetSendEvent.setCancelled(true);

                    // Cache wrapper and data to reconstruct and send it back later.
                    final WrapperLoginServerLoginSuccess wrapperLoginServerLoginSuccess = new WrapperLoginServerLoginSuccess(packetSendEvent);

                    // Fetch API async.
                    this.corePlugin.getApiManager().isIpBlocked(ipAddress).thenAccept(blocked -> {

                        if (blocked) {

                            // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                            log(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, "IP is blocked by the API.");

                        } else {

                            // Process the packet again, the player is verified by the API.
                            packetSendEvent.getUser().sendPacketSilently(new WrapperLoginServerLoginSuccess(wrapperLoginServerLoginSuccess.getUserProfile(), wrapperLoginServerLoginSuccess.getSessionId(), wrapperLoginServerLoginSuccess.isStrictErrorHandling()));

                        }

                    });

                }

            }

            // Block further logic.
            return;

        }

        // Fired when a connection is closed.
        // From the tests I've run, this gets triggered on Spigot/Paper (all the time) and BungeeCord ("Outdated server!").
        if (packetTypeCommon == PacketType.Login.Server.DISCONNECT) {

            final WrapperLoginServerDisconnect wrapperLoginServerDisconnect = new WrapperLoginServerDisconnect(packetSendEvent);

            // Process DISCONNECT on a separate function, both PLAY, LOGIN and CONFIGURATION DISCONNECT share the same logic.
            final Component safeDisconnectReason = processDisconnect(wrapperLoginServerDisconnect.getReason());

            // Set the new reason and tell packetevents to re-encode the packet and send it.
            wrapperLoginServerDisconnect.setReason(safeDisconnectReason);

            packetSendEvent.markForReEncode(true);

            // Block further logic.
            return;

        }

        // Fired when a connection is closed.
        // From the tests I've run, this gets triggered on Velocity ("Outdated client!") and BungeeCord ("Outdated client!").
        if (packetTypeCommon == PacketType.Play.Server.DISCONNECT && PacketEvents.getAPI().getInjector().isProxy()) {

            final WrapperPlayServerDisconnect wrapperPlayServerDisconnect = new WrapperPlayServerDisconnect(packetSendEvent);

            // Process DISCONNECT on a separate function, both PLAY, LOGIN and CONFIGURATION DISCONNECT share the same logic.
            final Component safeDisconnectReason = processDisconnect(wrapperPlayServerDisconnect.getReason());

            // Set the new reason and tell packetevents to re-encode the packet and send it.
            wrapperPlayServerDisconnect.setReason(safeDisconnectReason);

            packetSendEvent.markForReEncode(true);

            // Block further logic.
            return;

        }

        // Fired when a connection is closed.
        // From the tests I've run, this gets triggered on Velocity ("Outdated server!")
        if (packetTypeCommon == PacketType.Configuration.Server.DISCONNECT && this.corePlugin.getPlatformProperties().getProperty("platform-name").equals("velocity")) {

            final WrapperConfigServerDisconnect wrapperConfigServerDisconnect = new WrapperConfigServerDisconnect(packetSendEvent);

            // Process DISCONNECT on a separate function, both PLAY, LOGIN and CONFIGURATION DISCONNECT share the same logic.
            final Component safeDisconnectReason = processDisconnect(wrapperConfigServerDisconnect.getReason());

            // Set the new reason and tell packetevents to re-encode the packet and send it.
            wrapperConfigServerDisconnect.setReason(safeDisconnectReason);

            packetSendEvent.markForReEncode(true);

            // Block further logic.
            return;

        }

        // Decide if we should use the new LOGIN_SUCCESS_ACK (1.20.2+) or the older LOGIN_SUCCESS (< 1.20.2).
        // If we are on a backend, the server version is straight forward to get.
        // However, if we're on a proxy, the server version is marked as 1.8 since it's the lowest Bungee supports (when it could be anything else).
        // Therefore, we should base ourselves off the client's version ONLY if we are running on a proxy.
        final boolean useLoginSuccessAck = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)
                || (PacketEvents.getAPI().getInjector().isProxy()
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
        this.corePlugin.getApiManager().checkIp(ipAddress);

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
        this.corePlugin.getApiManager().checkIp(ipAddress);

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
        return "{\"description\":{\"text\":\"\",\"extra\":[\"A Minecraft Server\"]},\"players\":{\"max\":20,\"online\":0},\"version\":{\"name\":\"Paper 1.21.11\",\"protocol\":774},\"enforcesSecureChat\":true}";
    }

    private boolean exemptLocalIpAddress(final InetSocketAddress inetSocketAddress) {

        // Exempt local IP addresses according to configuration file.
        if (this.corePlugin.getConfigManager().getConfig().isIgnoreLocalAddresses() && !this.corePlugin.isTestMode()) {

            final InetAddress inetAddress = inetSocketAddress.getAddress();
            return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress();

        }

        return false;

    }

    private Component processDisconnect(final Component disconnectReason) {

        if (disconnectReason instanceof TextComponent) {

            final TextComponent disconnectReasonTextComponent = (TextComponent) disconnectReason;

            // Spigot, Paper and Velocity work this way.
            // BungeeCord loooooveeess to do it their way, we have it handle it specifically.
            // TODO: directly take the kick message from spigot.yml if running on a backend.
            if (!this.corePlugin.getPlatformProperties().getProperty("platform-name").equals("bungeecord")) {

                // This MUST match every of the normal "Outdated...!" message.
                // This ensures no server whatsoever can match this kind of kick message.
                final Style style = disconnectReasonTextComponent.style();

                // Determine expected color based on server type.
                // If it's directly running on a backend, it's white (null) by default.
                // On Velocity, it's red by default.
                final NamedTextColor expectedNamedTextColor = (PacketEvents.getAPI().getInjector().isProxy()
                        ? NamedTextColor.RED
                        : null);

                // Style must be clean/default and color must be exactly Red (#FF5555).
                if (!isCleanStyle(style) || !Objects.equals(style.color(), expectedNamedTextColor)) {
                    return disconnectReason;
                }

                // Must have no children.
                if (!disconnectReasonTextComponent.children().isEmpty()) {
                    return disconnectReason;
                }

                final String disconnectReasonString = disconnectReasonTextComponent.content();

                // Determine regex based on server type.
                // If it's directly running on a backend, it's safe to assume the server version is used in the kick message.
                // However, on Velocity this isn't the case.
                final String versionRegex = (PacketEvents.getAPI().getInjector().isProxy()
                        ? "\\d.*"
                        : PacketEvents.getAPI().getServerManager().getVersion().getReleaseName().replace(".", "\\.") + ".*");

                // Use a regex to match any number and everything after.
                // This ensures the version is completed changed no matter what's after.
                // This helps for versions like "1.21.11 Unobfuscated".
                final String newDisconnectReason = disconnectReasonString.replaceAll(versionRegex, "1.21.11");

                // Reconstruct reason with spoofed server version.
                return Component.text()
                        .content(newDisconnectReason)
                        .style(disconnectReasonTextComponent.style())
                        .build();


            // Specific logic for BungeeCord nested kick messages.
            } else {

                // BungeeCord nested kick message layout handling
                final List<Component> children = disconnectReasonTextComponent.children();

                // A classic native kick structure requires the root to have exactly 2 children:
                // Child 0: "Kicked whilst connecting to lobby: " (Red).
                // Child 1: "Outdated client! Please use 1.20.1" (White).
                if (children.size() != 2) {
                    return disconnectReason;
                }

                final Component kickMessage = children.get(0);
                final Component outdatedMessage = children.get(1);

                if (kickMessage instanceof TextComponent && outdatedMessage instanceof TextComponent) {

                    final TextComponent kickMessageTextComponent = (TextComponent) kickMessage;
                    final TextComponent outdatedMessageTextComponent = (TextComponent) outdatedMessage;

                    // This MUST match every of the normal "Outdated...!" message.
                    // This ensures no server whatsoever can match this kind of kick message.
                    final Style kickMessageStyle = kickMessageTextComponent.style();
                    final Style outdatedMessageStyle = outdatedMessageTextComponent.style();

                    // Style must be clean/default and color must be exactly Red (#FF5555).
                    if (!isCleanStyle(kickMessageStyle) || !NamedTextColor.RED.equals(kickMessageStyle.color())) {
                        return disconnectReason;
                    }

                    // Style must be clean/default and color must be exactly White (#FFFFFF).
                    if (!isCleanStyle(outdatedMessageStyle) || !NamedTextColor.WHITE.equals(outdatedMessageStyle.color())) {
                        return disconnectReason;
                    }

                    final String disconnectReasonString = outdatedMessageTextComponent.content();

                    // Use a regex to match any number and everything after.
                    // This ensures the version is completed changed no matter what's after.
                    // This helps for versions like "1.21.11 Unobfuscated".
                    final String newDisconnectReason = disconnectReasonString.replaceAll("\\d.*", "1.21.11");
                    final TextComponent newOutdatedMessageTextComponent = outdatedMessageTextComponent.content(newDisconnectReason);

                    // Reconstruct reason with spoofed server version.
                    return disconnectReasonTextComponent.children(Arrays.asList(kickMessageTextComponent, newOutdatedMessageTextComponent));

                }

            }

        }

        // In case something fails, but this is unsafe.
        return disconnectReason;

    }

    // Made for less repetitiveness across the processDisconnect function.
    private boolean isCleanStyle(final Style style) {
        return style.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.NOT_SET
                && style.decoration(TextDecoration.BOLD) == TextDecoration.State.NOT_SET
                && style.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.NOT_SET
                && style.decoration(TextDecoration.UNDERLINED) == TextDecoration.State.NOT_SET
                && style.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET
                && style.clickEvent() == null
                && style.hoverEvent() == null
                && style.insertion() == null
                && style.font() == null;
    }

}
