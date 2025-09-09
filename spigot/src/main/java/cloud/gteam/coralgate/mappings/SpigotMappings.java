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

package cloud.gteam.coralgate.mappings;

import cloud.gteam.coralgate.packetevents.mappings.enums.ConnectionStateMappings;
import cloud.gteam.coralgate.packetevents.mappings.enums.client.ClientPacketType;
import cloud.gteam.coralgate.packetevents.mappings.enums.server.ServerPacketType;
import cloud.gteam.coralgate.packetevents.mappings.others.SaltSignatureMappings;
import cloud.gteam.coralgate.packetevents.mappings.others.SignatureDataMappings;
import cloud.gteam.coralgate.packetevents.mappings.wrappers.*;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.util.crypto.SaltSignature;
import com.github.retrooper.packetevents.util.crypto.SignatureData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerPong;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;

public final class SpigotMappings {

    /// Client to Server

    public static ClientPacketType mapToClientType(final PacketReceiveEvent packetReceiveEvent) {

        final PacketTypeCommon packetType = packetReceiveEvent.getPacketType();

        if (packetType == PacketType.Status.Client.REQUEST) return ClientPacketType.REQUEST;
        if (packetType == PacketType.Status.Client.PING) return ClientPacketType.PING;
        if (packetType == PacketType.Handshaking.Client.HANDSHAKE) return ClientPacketType.HANDSHAKE;
        if (packetType == PacketType.Login.Client.LOGIN_START) return ClientPacketType.LOGIN_START;
        if (packetType == PacketType.Login.Client.ENCRYPTION_RESPONSE) return ClientPacketType.ENCRYPTION_RESPONSE;
        if (packetType == PacketType.Login.Client.LOGIN_SUCCESS_ACK) return ClientPacketType.LOGIN_SUCCESS_ACK;

        return ClientPacketType.UNKNOWN;

    }

    public static Object mapToWrapper(final PacketReceiveEvent packetReceiveEvent, final ClientPacketType clientPacketType) {

        switch (clientPacketType) {

            case HANDSHAKE:

                final WrapperHandshakingClientHandshake originalHandshake = new WrapperHandshakingClientHandshake(packetReceiveEvent);
                return new WrapperHandshakingClientHandshakeMappings(
                        originalHandshake.getProtocolVersion(),
                        originalHandshake.getServerAddress(),
                        originalHandshake.getServerPort(),
                        SpigotMappings.mapConnectionIntention(originalHandshake.getIntention()),
                        SpigotMappings.mapConnectionState(originalHandshake.getNextConnectionState())
                );

            case LOGIN_START:

                final WrapperLoginClientLoginStart originalLogin = new WrapperLoginClientLoginStart(packetReceiveEvent);
                return new WrapperLoginClientLoginStartMappings(
                        originalLogin.getUsername(),
                        SpigotMappings.mapSignatureData(originalLogin.getSignatureData().orElse(null)),
                        originalLogin.getPlayerUUID().orElse(null)
                );

            case ENCRYPTION_RESPONSE:

                final WrapperLoginClientEncryptionResponse originalEncryption = new WrapperLoginClientEncryptionResponse(packetReceiveEvent);
                return new WrapperLoginClientEncryptionResponseMappings(
                        originalEncryption.getEncryptedSharedSecret(),
                        originalEncryption.getEncryptedVerifyToken().orElse(null),
                        SpigotMappings.mapSaltSignature(originalEncryption.getSaltSignature().orElse(null))
                );

            default:
                return null;

        }

    }

    public static WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings mapConnectionIntention(final WrapperHandshakingClientHandshake.ConnectionIntention originalIntention) {

        switch (originalIntention) {

            case STATUS:
                return WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings.STATUS;

            case LOGIN:
                return WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings.LOGIN;

            case TRANSFER:
                return WrapperHandshakingClientHandshakeMappings.ConnectionIntentionMappings.TRANSFER;

            default:
                return null;

        }

    }

    public static ConnectionStateMappings mapConnectionState(final ConnectionState originalConnectionState) {

        switch (originalConnectionState) {

            case HANDSHAKING:
                return ConnectionStateMappings.HANDSHAKING;

            case STATUS:
                return ConnectionStateMappings.STATUS;

            case LOGIN:
                return ConnectionStateMappings.LOGIN;

            case PLAY:
                return ConnectionStateMappings.PLAY;

            case CONFIGURATION:
                return ConnectionStateMappings.CONFIGURATION;

            default:
                return null;

        }

    }

    public static SignatureDataMappings mapSignatureData(final SignatureData signatureData) {

        if (signatureData == null) return null;

        return new SignatureDataMappings(signatureData.getTimestamp(), signatureData.getPublicKey(), signatureData.getSignature());

    }

    public static SaltSignatureMappings mapSaltSignature(final SaltSignature saltSignature) {

        if (saltSignature == null) return null;

        return new SaltSignatureMappings(saltSignature.getSalt(), saltSignature.getSignature());

    }

    ///  Server to Client

    public static ServerPacketType mapToServerType(final PacketSendEvent packetSendEvent) {

        final PacketTypeCommon packetType = packetSendEvent.getPacketType();

        if (packetType == PacketType.Status.Server.RESPONSE) return ServerPacketType.RESPONSE;
        if (packetType == PacketType.Status.Server.PONG) return ServerPacketType.PONG;
        if (packetType == PacketType.Login.Server.ENCRYPTION_REQUEST) return ServerPacketType.ENCRYPTION_REQUEST;
        if (packetType == PacketType.Login.Server.SET_COMPRESSION) return ServerPacketType.SET_COMPRESSION;
        if (packetType == PacketType.Login.Server.LOGIN_SUCCESS) return ServerPacketType.LOGIN_SUCCESS;

        return ServerPacketType.UNKNOWN;

    }

    public static Object mapToWrapper(final PacketSendEvent packetSendEvent, final ServerPacketType serverPacketType) {

        switch (serverPacketType) {

            case RESPONSE:
                final WrapperStatusServerResponse originalResponse = new WrapperStatusServerResponse(packetSendEvent);
                return new WrapperStatusServerResponseMappings(originalResponse.getComponentJson());

            case PONG:
                final WrapperStatusServerPong originalPong = new WrapperStatusServerPong(packetSendEvent);
                return new WrapperStatusServerPongMappings(originalPong.getTime());

            default:
                return null;

        }

    }

    public static PacketWrapper<?> mapToPacketWrapper(final ServerPacketType serverPacketType, final Object packetWrapper) {

        switch (serverPacketType) {

            case RESPONSE:
                final WrapperStatusServerResponseMappings mappedResponse = (WrapperStatusServerResponseMappings) packetWrapper;
                return new WrapperStatusServerResponse(mappedResponse.getComponentJson());

            case PONG:
                final WrapperStatusServerPongMappings mappedPong = (WrapperStatusServerPongMappings) packetWrapper;
                return new WrapperStatusServerPong(mappedPong.getTime());

            default:
                return null;

        }

    }

}
