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

package cloud.gteam.coralgate.injector.handlers;

import cloud.gteam.coralgate.injector.NettyResponder;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

import java.nio.charset.StandardCharsets;

public class BungeeNettyResponder implements NettyResponder {

    public BungeeNettyResponder() {}

    @Override
    public void sendLegacyPingResponse(final User user, final int protocolVersion, final String serverVersion, final String motd, final int onlinePlayers, final int maxPlayers) {

        final String response = "§1\0"
                + protocolVersion + "\0"
                + serverVersion + "\0"
                + motd + "\0"
                + onlinePlayers + "\0"
                + maxPlayers;
        write(user, response);

    }

    @Override
    public void sendOldLegacyPingResponse(final User user, final String motd, final int onlinePlayers, final int maxPlayers) {

        final String response = motd + "§" + onlinePlayers + "§" + maxPlayers;
        write(user, response);

    }

    private void write(final User user, final String response) {

        final Channel channel = (Channel) user.getChannel();
        final byte[] responseBytes = response.getBytes(StandardCharsets.UTF_16BE);

        final ByteBuf buf = channel.alloc().buffer();
        buf.writeByte(0xFF);
        buf.writeShort(response.length());
        buf.writeBytes(responseBytes);

        channel.pipeline().firstContext()
                .writeAndFlush(buf)
                .addListener(ChannelFutureListener.CLOSE);

    }

}
