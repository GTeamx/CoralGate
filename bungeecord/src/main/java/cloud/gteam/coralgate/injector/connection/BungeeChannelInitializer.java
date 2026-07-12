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

import com.github.retrooper.packetevents.protocol.ConnectionState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class BungeeChannelInitializer extends ChannelInitializer<Channel> {
    private static final Method INIT_CHANNEL_METHOD;

    static {
        try {
            INIT_CHANNEL_METHOD = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            INIT_CHANNEL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    private final Object oldInitializer;
    public BungeeChannelInitializer(Object oldInitializer) {
        this.oldInitializer = oldInitializer;
    }

    @Override
    protected void initChannel(@NotNull Channel channel) throws Exception {
        if (!channel.isActive()) return;
        INIT_CHANNEL_METHOD.invoke(oldInitializer, channel);

        //No injection if "legacy-decoder" is not present.
        if (channel.pipeline().get("legacy-decoder") == null) return;

        ServerConnectionInitializer.initChannel(channel, ConnectionState.HANDSHAKING);
    }
}