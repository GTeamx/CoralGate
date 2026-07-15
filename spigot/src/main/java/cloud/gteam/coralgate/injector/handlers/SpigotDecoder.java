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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.simple.PacketHandshakeReceiveEvent;
import com.github.retrooper.packetevents.exception.PacketProcessException;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.ExceptionUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class SpigotDecoder extends MessageToMessageDecoder<ByteBuf> {

    public final User user;

    public SpigotDecoder(final User user) {
        this.user = user;
    }

    public void read(final ChannelHandlerContext ctx, final ByteBuf input, final List<Object> out) {

        try {

            handleServerBoundPacket(ctx.channel(), this.user, input);
            out.add(ByteBufHelper.retain(input));

        } catch (final Throwable t) {

            // We must be sure all the exceptions caused by our handlers are PacketProcessExceptions.
            // In the case we have thrown an exception that is not a PacketProcessException, let's wrap it in order to
            // allow exceptionCaught to handle it properly.
            if (ExceptionUtil.isException(t, PacketProcessException.class)) {
                throw t;
            } else {
                throw new PacketProcessException(t);
            }

        }

    }

    public static void handleServerBoundPacket(final Object channel, final User user, final ByteBuf buffer) {

        final int preProcessIndex = ByteBufHelper.readerIndex(buffer);

        try {
            int id = ByteBufHelper.readVarInt(buffer);

            if (id != 0xFE) return;
        } catch (Exception e) {
            return;
        } finally {
            buffer.readerIndex(preProcessIndex);
        }

        final PacketReceiveEvent packetReceiveEvent = new PacketHandshakeReceiveEvent(channel, user, null, buffer, true);

        PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> ByteBufHelper.readerIndex(buffer, ByteBufHelper.readerIndex(buffer)));

        if (!packetReceiveEvent.isCancelled()) {

            // Did they ever use a wrapper?
            if (packetReceiveEvent.getLastUsedWrapper() != null) {

                // Rewrite the buffer.
                ByteBufHelper.clear(buffer);
                packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                packetReceiveEvent.getLastUsedWrapper().write();

            } else {

                // If no wrappers were used, just pass on the original buffer.
                // Correct the reader index, basically what the next handler is expecting.
                ByteBufHelper.readerIndex(buffer, preProcessIndex);

            }

        } else {

            // Cancelling the packet, lets clear the buffer.
            ByteBufHelper.clear(buffer);

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
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {

        // If we didn't cause the exception, let the server handle it.
        if (!ExceptionUtil.isException(cause, PacketProcessException.class)) {

            super.exceptionCaught(ctx, cause);
            return;

        }

        // We log exceptions only if the server is in debug mode.
        if (PacketEvents.getAPI().getSettings().isDebugEnabled() || SpigotReflectionUtil.isMinecraftServerInstanceDebugging()) {

            if (PacketEvents.getAPI().getSettings().isFullStackTraceEnabled()) {

                final String state = this.user != null ? this.user.getDecoderState().name() : "null";
                final String clientVersion = this.user != null ? this.user.getClientVersion().getReleaseName() : "null";

                PacketEvents.getAPI().getLogManager().warn("An error occurred while processing a packet from "
                        + this.user.getProfile().getName() + " (state: " + state + ", clientVersion: " + clientVersion +
                        ", serverVersion: " + PacketEvents.getAPI().getServerManager().getVersion().getReleaseName() + ")", cause);

            } else {
                PacketEvents.getAPI().getLogManager().warn(cause.getMessage());
            }

        }

        if (PacketEvents.getAPI().getSettings().isKickOnPacketExceptionEnabled()) {

            ctx.channel().close();

            if (this.user != null && this.user.getProfile().getName() != null) {
                PacketEvents.getAPI().getLogManager().warn("Disconnected " + this.user.getProfile().getName() + " due to an invalid packet!");
            }

        }

    }

}
