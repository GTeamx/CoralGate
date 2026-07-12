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

package cloud.gteam.coralgate.injector.handlers;

import cloud.gteam.coralgate.injector.connection.ServerConnectionInitializer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.simple.PacketHandshakeReceiveEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BungeeDecoder extends MessageToMessageDecoder<ByteBuf> {
    public User user;

    public BungeeDecoder(User user) {
        this.user = user;
    }

    public void read(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> output) throws Exception {

        int firstReaderIndex = byteBuf.readerIndex();

        PacketHandshakeReceiveEvent packetReceiveEvent = new PacketHandshakeReceiveEvent(ctx.channel(), user, null, byteBuf, false);

        int readerIndex = byteBuf.readerIndex();
        PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> byteBuf.readerIndex(readerIndex));
        if (!packetReceiveEvent.isCancelled()) {
            if (packetReceiveEvent.getLastUsedWrapper() != null) {
                ByteBufHelper.clear(byteBuf);
                packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                packetReceiveEvent.getLastUsedWrapper().write();
            }
            byteBuf.readerIndex(firstReaderIndex);
            output.add(byteBuf.retain());
        } else {
            //Cancelling the packet, lets clear the buffer
            ByteBufHelper.clear(byteBuf);
        }
        if (packetReceiveEvent.hasPostTasks()) {
            for (Runnable task : packetReceiveEvent.getPostTasks()) {
                task.run();
            }
        }

    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (buffer.isReadable()) {
            read(ctx, buffer, out);
        }
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        ServerConnectionInitializer.destroyChannel(ctx.channel());
        super.channelInactive(ctx);
    }

}