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

public class VelocityDecoder extends MessageToMessageDecoder<ByteBuf> {

    public final User user;

    public VelocityDecoder(final User user) {
        this.user = user;
    }

    public void read(final ChannelHandlerContext ctx, final ByteBuf byteBuf, final List<Object> output) throws Exception {

        final int firstReaderIndex = byteBuf.readerIndex();
        final int readable = byteBuf.readableBytes();
        boolean isLegacyServerListPing = false;

        // Detect every legacy server list ping variant by peeking bytes directly (never consuming
        // the reader index), rather than parsing a VarInt. A VarInt read would throw on the
        // single-byte pre-1.4 variant since there's no second byte to complete it.
        if (readable >= 1 && byteBuf.getUnsignedByte(firstReaderIndex) == 0xFE) {

            // Bare single 0xFE, nothing trailing, pre-1.4 clients (1.1, 1.2, 1.3).
            if (readable == 1) {

                isLegacyServerListPing = true;

            } else if (byteBuf.getUnsignedByte(firstReaderIndex + 1) == 0x01) {

                if (readable == 2) {

                    // FE 01, nothing trailing, 1.4/1.5 clients.
                    isLegacyServerListPing = true;

                } else if (readable >= 3 && byteBuf.getUnsignedByte(firstReaderIndex + 2) == 0xFA) {

                    // FE 01 FA ..., 1.6 clients.
                    isLegacyServerListPing = true;

                }

            }

        }

        if (!isLegacyServerListPing) {

            output.add(byteBuf.retain());
            return;

        }

        final PacketHandshakeReceiveEvent packetReceiveEvent = new PacketHandshakeReceiveEvent(ctx.channel(), this.user, null, byteBuf, false);
        PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> byteBuf.readerIndex(byteBuf.readerIndex()));

        // No action is taken about the legacy packet here.
        // NetworkProcessor receives the packet (like any other packet), and then decides what to do,
        // including which legacy reply format to use (it re-derives that from the buffer itself).
        // If the packet is canceled, it's cleared. Else we just pass it as any normal packet would.
        if (packetReceiveEvent.isCancelled()) {
            ByteBufHelper.clear(byteBuf);
        } else {

            byteBuf.readerIndex(firstReaderIndex);
            output.add(byteBuf.retain());

        }

        if (packetReceiveEvent.hasPostTasks()) {

            for (final Runnable task : packetReceiveEvent.getPostTasks()) {
                task.run();
            }

        }

    }

    @Override
    public void decode(final ChannelHandlerContext ctx, final ByteBuf buffer, final List<Object> out) throws Exception {

        if (buffer.isReadable()) {
            read(ctx, buffer, out);
        }

    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {

        ServerConnectionInitializer.destroyChannel(ctx.channel());
        super.channelInactive(ctx);

    }

}
