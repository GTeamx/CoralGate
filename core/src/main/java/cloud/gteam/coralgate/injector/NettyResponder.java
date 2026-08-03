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

package cloud.gteam.coralgate.injector;

import com.github.retrooper.packetevents.protocol.player.User;

public interface NettyResponder {

    // 1.4+ format: "§1\0protocol\0version\0motd\0online\0max".
    void sendLegacyPingResponse(final User user, final int protocolVersion, final String serverVersion, final String motd, final int onlinePlayers, final int maxPlayers);

    // Pre-1.4 (<=1.3) format: "motd§online§max", no §1 prefix, no protocol/version fields.
    void sendOldLegacyPingResponse(final User user, final String motd, final int onlinePlayers, final int maxPlayers);

}
