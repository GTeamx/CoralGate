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
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientSettings;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.status.client.WrapperStatusClientPing;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

        // Exempt local IP addresses according to configuration file.
        if (this.corePlugin.getConfigManager().getConfig().isIgnoreLocalAddresses() && !this.corePlugin.isTestMode()) {

            final InetAddress inetAddress = inetSocketAddress.getAddress();

            if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress())
                return;

        }

        final PacketTypeCommon packetTypeCommon = packetReceiveEvent.getPacketType();
        final ClientVersion clientVersion = packetReceiveEvent.getUser().getClientVersion();

        System.out.println("C->S | " + packetTypeCommon);

        /*
         * SOURCE PORT FILTERING.
         */

        /* Check different condition to trigger a MOTD packet check and blockage. */

        // This is the lowest dynamic port used by Linux.
        // Anything bellow means the port was forced to use that port and is therefore, not a real Minecraft client.
        final boolean invalidPort = inetSocketAddress.getPort() < 32768;

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
                if (!invalidPort)
                    CorePlugin.getLogger().warning("Suspicious port used by client. Keep an eye out for " + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "."  + packetTypeCommon.getName() + "]");
                else
                    CorePlugin.getLogger().severe("Invalid port used by client. Closing connection from " + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "."  + packetTypeCommon.getName() + "]");

                // Report the IP to CoralGate API.
                this.corePlugin.getApiManager().reportIp(ipAddress);

                // Clean up.
                this.connectionState.remove(inetSocketAddress);

            }

            if (processMOTD) {

                // Packet responsible for the latency showup.
                if (packetTypeCommon == PacketType.Status.Client.PING || packetTypeCommon == PacketType.Handshaking.Client.LEGACY_SERVER_LIST_PING) {

                    // Cancel packet and send a "forged" response.
                    packetReceiveEvent.setCancelled(true);
                    packetReceiveEvent.getUser().sendPacketSilently(new WrapperStatusClientPing(packetReceiveEvent));

                    // Block further logic.
                    return;

                }

                // Packet responsible for the MOTD message and server related information (player count, version).
                if (packetTypeCommon == PacketType.Status.Client.REQUEST) {

                    // Cancel the packet and send a forged generic looking MOTD.
                    packetReceiveEvent.setCancelled(true);
                    packetReceiveEvent.getUser().sendPacketSilently(new WrapperStatusServerResponse(getForgedMOTD()));

                    // Block further logic.
                    return;

                }

                // Specific 'STATUS' handshake state.
                // Yes the condition is "always true", but I prefer to keep this in case the protocol changes in future releases.
                if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

                    final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

                    // 'STATUS' only.
                    if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.STATUS && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.STATUS) {

                        packetReceiveEvent.setCancelled(true);

                        // Block further logic.
                        return;

                    }

                }

            }

        }

        // Ran last so forged MOTD can be sent. This effectively only blocks actual connection packets.
        if (invalidPort) {

            // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid port used by client.");

            // Block further logic.
            return;

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
         * PROTOCOL FILTERING.
         */

        if (isStatusPacket || isLoginSequencePacket) {

            // Exempt for local scanner test.
            final boolean scannerTest = inetSocketAddress.getHostString().equals("127.0.0.1") && inetSocketAddress.getPort() == 65535 && this.corePlugin.isTestMode();
            if (!scannerTest) {

                // If the client's protocol version is invalid.
                if (clientVersion == null || clientVersion.getProtocolVersion() == -1) {

                    // Log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
                    logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid protocol version.");

                    // Block further logic.
                    return;

                }

            }

        }

        // We don't need those packets anymore.
        if (isStatusPacket) return;

        /*
         * PACKET ORDER FILTERING.
         */

        // Specific 'LOGIN' handshake login, the first packet in a legitimate connection sequence.
        if (packetTypeCommon == PacketType.Handshaking.Client.HANDSHAKE) {

            final WrapperHandshakingClientHandshake wrapperHandshakingClientHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);

            if (wrapperHandshakingClientHandshake.getIntention() == WrapperHandshakingClientHandshake.ConnectionIntention.LOGIN && wrapperHandshakingClientHandshake.getNextConnectionState() == ConnectionState.LOGIN) {

                this.connectionState.put(inetSocketAddress, packetTypeCommon);

            }

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

        // Prior to 1.20.2 CLIENT_SETTINGS are sent via the PLAY state right after LOGIN_SUCCESS.
        // On 1.20.2+, CLIENT_SETTINGS is sent exclusively via the CONFIGURATION state.
        // This is exclusively used on < 1.20.2.
        // Ignore the packet if the connection is already done. This can be triggered in game.
        if (packetTypeCommon == PacketType.Play.Client.CLIENT_SETTINGS && this.connectionState.getOrDefault(inetSocketAddress, null) != PacketType.Play.Client.PLUGIN_MESSAGE) {

            final PacketTypeCommon expectedState = clientVersion.isOlderThan(ClientVersion.V_1_20_2)
                    ? PacketType.Configuration.Client.CONFIGURATION_END_ACK
                    : PacketType.Login.Server.LOGIN_SUCCESS;

            // Validate state against certain conditions: previous LOGIN_SUCCESS (< 1.20.2), Configuration.PLUGIN_MESSAGE (>= 1.20.2).
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, expectedState, packetTypeCommon, "Missing login success procedure before client settings.");

            final WrapperPlayClientSettings wrapperPlayClientSettings = new WrapperPlayClientSettings(packetReceiveEvent);

            // If view distance exceeds minimum or maximum (vanilla).
            if (wrapperPlayClientSettings.getViewDistance() < 2 || wrapperPlayClientSettings.getViewDistance() > 32)
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid view distance.");

            // TODO: maybe go deeper in registries or more checks regarding settings?

            // Block further logic.
            return;

        }

        // Minecraft versions prior to 1.20.2 exclusively use PLUGIN_MESSAGE in the PLAY state.
        // Versions from 1.20.2 and above first use PLUGIN_MESSAGE in the CONFIGURATION state to send the brand,
        // then uses the PLAY state to send "minecraft:register".
        if (packetTypeCommon == PacketType.Play.Client.PLUGIN_MESSAGE) {

            final WrapperPlayClientPluginMessage wrapperPlayClientPluginMessage = new WrapperPlayClientPluginMessage(packetReceiveEvent);
            final String channelName = wrapperPlayClientPluginMessage.getChannelName();

            // Calculate expected state since 1.20.2+ also uses the CONFIGURATION state.
            // On < 1.20.2, PLUGIN_MESSAGE is sent twice in a row, once for "minecraft:brand" and another "minecraft:register".
            final PacketTypeCommon expectedState = clientVersion.isOlderThan(ClientVersion.V_1_20_2)
                    ? channelName.equals("minecraft:register")
                      ? PacketType.Play.Client.PLUGIN_MESSAGE
                      : PacketType.Play.Client.CLIENT_SETTINGS
                    : PacketType.Configuration.Client.CONFIGURATION_END_ACK;

            // Validate state against certain conditions: previous Play.CLIENT_SETTINGS/Play.PLUGIN_MESSAGE (< 1.20.2), CONFIGURATION_END_ACK (>= 1.20.2).
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, expectedState, packetTypeCommon, "Missing configuration before client brand.");

            // Took from TotemGuard: https://github.com/Bram1903/TotemGuard/blob/main/src/main/java/com/deathmotion/totemguard/checks/impl/misc/ClientBrand.java
            // All credits to Bram!
            // Check client brand.
            if (!channelName.equals("minecraft:brand") && !channelName.equals("MC|BRAND")) return;

            final byte[] data = wrapperPlayClientPluginMessage.getData();

            // Weird ahhhh brand, im not waisting my time on no namer brands.
            if (data.length > 64 || data.length == 0)
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid client brand. (" + data.length + " bytes).");

            final byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);
            String clientBrand = new String(minusLength).replace(" (Velocity)", ""); // removes velocity's brand suffix
            clientBrand = !clientBrand.isEmpty() ? Pattern.compile("(?i)" + '§' + "[0-9A-FK-ORX]").matcher(clientBrand).replaceAll("") : clientBrand;

            // Special handling for lunar...
            if (clientBrand.startsWith("lunarclient:")) clientBrand = "lunarclient";

            if (!this.corePlugin.getConfigManager().getConfig().getAllowedClientBrands().contains(clientBrand))
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Unauthorized client brand name (" +  clientBrand + ").");

            // Block further logic.
            return;

        }

        // Used exclusively on 1.20.2+, the client brand is sent here.
        if (packetTypeCommon == PacketType.Configuration.Client.PLUGIN_MESSAGE) {

            // Validate state against certain conditions: previous LOGIN_SUCCESS_ACK.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Configuration.Server.CONFIGURATION_END, packetTypeCommon, "Missing configuration end from server.");

            // Took from TotemGuard: https://github.com/Bram1903/TotemGuard/blob/main/src/main/java/com/deathmotion/totemguard/checks/impl/misc/ClientBrand.java
            // All credits to Bram!
            // Check client brand.
            final WrapperConfigClientPluginMessage wrapperConfigClientPluginMessage = new WrapperConfigClientPluginMessage(packetReceiveEvent);
            final String channelName = wrapperConfigClientPluginMessage.getChannelName();

            if (!channelName.equals("minecraft:brand") && !channelName.equals("MC|BRAND")) return;

            final byte[] data = wrapperConfigClientPluginMessage.getData();

            // Weird ahhhh brand, im not waisting my time on no namer brands.
            if (data.length > 64 || data.length == 0)
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid client brand. (" + data.length + " bytes).");

            final byte[] minusLength = new byte[data.length - 1];
            System.arraycopy(data, 1, minusLength, 0, minusLength.length);
            String clientBrand = new String(minusLength).replace(" (Velocity)", ""); // removes velocity's brand suffix
            clientBrand = !clientBrand.isEmpty() ? Pattern.compile("(?i)" + '§' + "[0-9A-FK-ORX]").matcher(clientBrand).replaceAll("") : clientBrand;

            // Special handling for lunar...
            if (clientBrand.startsWith("lunarclient:")) clientBrand = "lunarclient";

            if (!this.corePlugin.getConfigManager().getConfig().getAllowedClientBrands().contains(clientBrand))
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Unauthorized client brand name (" +  clientBrand + ").");

            // Block further logic.
            return;

        }

        // Used exclusively on 1.20.2+.
        if (packetTypeCommon == PacketType.Configuration.Client.CLIENT_SETTINGS) {

            // Validate state against certain conditions: previous Configuration.PLUGIN_MESSAGE.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Configuration.Client.PLUGIN_MESSAGE, packetTypeCommon, "Missing client brand before client settings.");

            final WrapperConfigClientSettings wrapperConfigClientSettings = new WrapperConfigClientSettings(packetReceiveEvent);

            // If view distance exceeds minimum or maximum (vanilla).
            if (wrapperConfigClientSettings.getViewDistance() < 2 || wrapperConfigClientSettings.getViewDistance() > 32)
                logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Invalid view distance.");

            // TODO: maybe go deeper in registries or more checks regarding settings?

            // Block further logic.
            return;

        }

        // Once CLIENT_SETTINGS and PLUGIN_MESSAGE are validated by the server.
        if (packetTypeCommon == PacketType.Configuration.Client.CONFIGURATION_END_ACK) {

            final PacketTypeCommon expectedState = clientVersion.isOlderThan(ClientVersion.V_1_20_2)
                    ? PacketType.Configuration.Server.CONFIGURATION_END
                    : PacketType.Configuration.Client.CLIENT_SETTINGS;

            // Validate state against certain conditions: previous Configuration.CLIENT_SETTINGS.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, expectedState, packetTypeCommon, "Missing full client settings and client brand before finishing configuration.");

            // Block further logic.
            return;

        }

        // Get the proper login packet based off the client version.
        final PacketTypeCommon currentState = this.connectionState.getOrDefault(inetSocketAddress, null);

        // Directly evaluate if the current state is valid for this version context.
        final boolean isValidState = clientVersion.isOlderThan(ClientVersion.V_1_20_2) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)
                ? (currentState == PacketType.Configuration.Client.CONFIGURATION_END_ACK) || (currentState == PacketType.Play.Client.PLUGIN_MESSAGE) || (currentState == PacketType.Play.Client.CLIENT_SETTINGS)
                : currentState == PacketType.Play.Client.PLUGIN_MESSAGE;

        if (!isValidState)
            logAndClose(packetReceiveEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing full connection procedure.");

        /* All checks passed! */

    }

    // Outgoing packets (Server to Client) [S->C]
    public void onPacketSend(final PacketSendEvent packetSendEvent) {

        final InetSocketAddress inetSocketAddress = packetSendEvent.getSocketAddress();
        final String ipAddress = inetSocketAddress.getHostString();

        // Exempt local IP addresses according to configuration file.
        if (this.corePlugin.getConfigManager().getConfig().isIgnoreLocalAddresses() && !this.corePlugin.isTestMode()) {

            final InetAddress inetAddress = inetSocketAddress.getAddress();

            if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) return;

        }

        final PacketTypeCommon packetTypeCommon = packetSendEvent.getPacketType();

        System.out.println("S->C | " + packetTypeCommon);

        /*
        * PACKET ORDER FILTERING.
        */

        // Whitelisted MOTD related packets.
        if (packetTypeCommon == PacketType.Status.Server.RESPONSE || packetTypeCommon == PacketType.Status.Server.PONG)
            return;

        // Client should have passed login start procedure.
        if (packetTypeCommon == PacketType.Login.Server.ENCRYPTION_REQUEST && this.corePlugin.isOnlineMode()) {

            // Validate state against certain conditions: previous LOGIN_START.
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

            // Validate state against certain conditions: previous ENCRYPTION_RESPONSE (if online mode) else LOGIN_START.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing encryption response or login start procedure.");

            // Block further logic.
            return;

        }

        if (packetTypeCommon == PacketType.Login.Server.LOGIN_SUCCESS) {

            final PacketTypeCommon requiredPacketTypeCommon = this.corePlugin.isOnlineMode()
                    ? (this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.ENCRYPTION_RESPONSE)
                    : (this.corePlugin.getCompressionThreshold() >= 0 ? PacketType.Login.Server.SET_COMPRESSION : PacketType.Login.Client.LOGIN_START);

            // Validate state against certain conditions: previous SET_COMPRESSION (if above 0) else ENCRYPTION_RESPONSE (if online mode) else LOGIN_START.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, requiredPacketTypeCommon, packetTypeCommon, "Missing set compression procedure.");

            // Block further logic.
            return;

        }

        // Sent after the server is done sending basic server data.
        // Right before the client starts sending its own client data.
        if (packetTypeCommon == PacketType.Configuration.Server.CONFIGURATION_END) {

            // Validate state against certain conditions: previous LOGIN_SUCCESS_ACK.
            // If something is wrong, log the violation, report the IP to CoralGate API, cancel the packet and close the connection.
            verifyAndTransitionState(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, PacketType.Login.Client.LOGIN_SUCCESS_ACK, packetTypeCommon, "Missing set compression procedure.");

            // Block further logic.
            return;

        }

        // Get the proper login packet based off the client version.
        final PacketTypeCommon currentState = this.connectionState.getOrDefault(inetSocketAddress, null);

        // Directly evaluate if the current state is valid for this version context.
        final boolean isValidState = packetSendEvent.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_20_2) && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_20_2)
                ? (currentState == PacketType.Login.Server.LOGIN_SUCCESS || currentState == PacketType.Play.Client.PLUGIN_MESSAGE)
                : (currentState == PacketType.Login.Client.LOGIN_SUCCESS_ACK || currentState == PacketType.Configuration.Client.CONFIGURATION_END_ACK || currentState == PacketType.Play.Client.PLUGIN_MESSAGE);

       if (!isValidState)
           logAndClose(packetSendEvent, inetSocketAddress, ipAddress, packetTypeCommon, "Missing full connection procedure.");

        /* All checks passed! */

    }

    @Override
    public void onUserDisconnect(final UserDisconnectEvent userDisconnectEvent) {

        final User user = userDisconnectEvent.getUser();

        if (user == null || user.getUUID() == null) return;

        // Clean up.
        this.connectionState.remove(user.getAddress());

    }

    private void logAndClose(final PacketReceiveEvent packetReceiveEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason) {

        CorePlugin.getLogger().severe(reason + " Closing connection from " + inetSocketAddress + ". [C->S | " + packetReceiveEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");
        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetReceiveEvent.setCancelled(true);
        packetReceiveEvent.getUser().closeConnection();

        // Clean up.
        this.connectionState.remove(inetSocketAddress);

    }

    private void logAndClose(final PacketSendEvent packetSendEvent, final InetSocketAddress inetSocketAddress, final String ipAddress, final PacketTypeCommon packetTypeCommon, final String reason) {

        CorePlugin.getLogger().severe(reason + " Closing connection from " + inetSocketAddress + ". [S->C | " + packetSendEvent.getPacketType().getClass().getDeclaringClass().getSimpleName() + "." + packetTypeCommon.getName() + "]");
        this.corePlugin.getApiManager().reportIp(ipAddress);

        packetSendEvent.setCancelled(true);
        packetSendEvent.getUser().closeConnection();

        // Clean up.
        this.connectionState.remove(inetSocketAddress);

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
        return "{\"description\":{\"text\":\"\",\"extra\":[\"A Minecraft Server\"]},\"players\":{\"max\":20,\"online\":0},\"version\":{\"name\":\"CraftBukkit 26.2\",\"protocol\":776},\"enforcesSecureChat\":true}";
    }

}
