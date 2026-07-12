/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
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

package cloud.gteam.coralgate.injector.connection;

import cloud.gteam.coralgate.injector.SpigotInjector;
import cloud.gteam.coralgate.injector.handlers.SpigotDecoder;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.FakeChannelUtil;
import io.netty.channel.Channel;

import java.util.NoSuchElementException;

public class ServerConnectionInitializer {

    public static void initChannel(Channel ch, ConnectionState connectionState) {
        if (FakeChannelUtil.isFakeChannel(ch)) {
            return;
        }

        User user = new User(ch, connectionState, null, new UserProfile(null, null));

        relocateHandlers(ch, user);
    }

    public static void relocateHandlers(Channel ctx, User user) {
        try {
            SpigotDecoder decoder = new SpigotDecoder(user);
            ctx.pipeline().addBefore("legacy_query", SpigotInjector.DECODER_NAME, decoder);
        } catch (NoSuchElementException ex) {
            String handlers = ChannelHelper.pipelineHandlerNamesAsString(ctx);
            throw new IllegalStateException("CoralGateInjector failed to add a decoder to the netty pipeline. Pipeline handlers: " + handlers, ex);
        }
    }
}