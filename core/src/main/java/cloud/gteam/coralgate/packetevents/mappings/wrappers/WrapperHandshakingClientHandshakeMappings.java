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

package cloud.gteam.coralgate.packetevents.mappings.wrappers;

import cloud.gteam.coralgate.packetevents.mappings.enums.ConnectionStateMappings;

public class WrapperHandshakingClientHandshakeMappings {

    private final int protocolVersion;
    //private final ClientVersion; //TODO: map
    private final String serverAddress;
    private final int serverPort;
    private final ConnectionIntentionMappings intention;
    private final ConnectionStateMappings targetState;

    public WrapperHandshakingClientHandshakeMappings(final int protocolVersion, final String serverAddress, final int serverPort, final ConnectionIntentionMappings intention, final ConnectionStateMappings targetState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.intention = intention;
        this.targetState = targetState;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public ConnectionIntentionMappings getIntention() {
        return this.intention;
    }

    public ConnectionStateMappings getNextConnectionState() {
        return this.targetState;
    }

    public enum ConnectionIntentionMappings {

        STATUS,
        LOGIN,
        TRANSFER

    }

}
