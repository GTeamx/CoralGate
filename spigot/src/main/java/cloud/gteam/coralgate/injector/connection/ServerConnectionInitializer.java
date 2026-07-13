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

    public static void initChannel(final Channel ch, final ConnectionState connectionState) {

        if (FakeChannelUtil.isFakeChannel(ch)) {
            return;
        }

        relocateHandlers(ch, new User(ch, connectionState, null, new UserProfile(null, null)));

    }

    public static void relocateHandlers(final Channel ctx, final User user) {

        try {
            ctx.pipeline().addBefore("legacy_query", SpigotInjector.DECODER_NAME, new SpigotDecoder(user));
        } catch (final NoSuchElementException e) {
            throw new IllegalStateException("CoralGateInjector failed to add a decoder to the netty pipeline. Pipeline handlers: " + ChannelHelper.pipelineHandlerNamesAsString(ctx), e);
        }

    }

}
