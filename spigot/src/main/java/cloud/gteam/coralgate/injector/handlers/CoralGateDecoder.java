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

public class CoralGateDecoder extends MessageToMessageDecoder<ByteBuf> {
    public User user;

    public CoralGateDecoder(User user) {
        this.user = user;
    }

    public void read(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) {
        try {
            handleServerBoundPacket(ctx.channel(), user, input);
            out.add(ByteBufHelper.retain(input));
        } catch (Throwable e) {
            // We must be sure all the exceptions caused by our handlers are PacketProcessExceptions
            // In the case we have thrown an exception that is not a PacketProcessException, let's wrap it in order to
            // allow exceptionCaught to handle it properly
            if (ExceptionUtil.isException(e, PacketProcessException.class)) {
                throw e;
            } else {
                throw new PacketProcessException(e);
            }
        }
    }

    static void handleServerBoundPacket(
            Object channel, User user, Object buffer
    ) {
        int preProcessIndex = ByteBufHelper.readerIndex(buffer);

        PacketReceiveEvent packetReceiveEvent = new PacketHandshakeReceiveEvent(channel, user, null, buffer, true);

        int processIndex = ByteBufHelper.readerIndex(buffer);
        PacketEvents.getAPI().getEventManager().callEvent(packetReceiveEvent, () -> ByteBufHelper.readerIndex(buffer, processIndex));
        if (!packetReceiveEvent.isCancelled()) {
            //Did they ever use a wrapper?
            if (packetReceiveEvent.getLastUsedWrapper() != null) {
                //Rewrite the buffer
                ByteBufHelper.clear(buffer);
                packetReceiveEvent.getLastUsedWrapper().writeVarInt(packetReceiveEvent.getPacketId());
                packetReceiveEvent.getLastUsedWrapper().write();
            } else {
                //If no wrappers were used, just pass on the original buffer.
                //Correct the reader index, basically what the next handler is expecting.
                ByteBufHelper.readerIndex(buffer, preProcessIndex);
            }
        } else {
            //Cancelling the packet, lets clear the buffer
            ByteBufHelper.clear(buffer);
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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // If we didn't cause the exception, let the server handle it.
        if (!ExceptionUtil.isException(cause, PacketProcessException.class)) {
            super.exceptionCaught(ctx, cause);
            return;
        }

        boolean debug = PacketEvents.getAPI().getSettings().isDebugEnabled() || SpigotReflectionUtil.isMinecraftServerInstanceDebugging();
        // We log exceptions only if the server is in debug mode.
        if (debug) {
            if (PacketEvents.getAPI().getSettings().isFullStackTraceEnabled()) {
                String state = user != null ? user.getDecoderState().name() : "null";
                String clientVersion = user != null ? user.getClientVersion().getReleaseName() : "null";

                PacketEvents.getAPI().getLogManager().warn("An error occurred while processing a packet from "
                        + user.getProfile().getName() + " (state: " + state + ", clientVersion: " + clientVersion +
                        ", serverVersion: " + PacketEvents.getAPI().getServerManager().getVersion().getReleaseName() + ")", cause);
            } else {
                PacketEvents.getAPI().getLogManager().warn(cause.getMessage());
            }
        }

        if (PacketEvents.getAPI().getSettings().isKickOnPacketExceptionEnabled()) {
            ctx.channel().close();

            if (user != null && user.getProfile().getName() != null) {
                PacketEvents.getAPI().getLogManager().warn("Disconnected " + user.getProfile().getName() + " due to an invalid packet!");
            }
        }
    }

}