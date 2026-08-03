package cloud.gteam.coralgate.injector.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;

public class BungeeLegacyKickRewriter extends ChannelOutboundHandlerAdapter {

    public BungeeLegacyKickRewriter() {}

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

        if (!(msg instanceof ByteBuf)) {
            super.write(ctx, msg, promise);
            return;
        }

        final ByteBuf buf = (ByteBuf) msg;

        final int readable = buf.readableBytes();

        // Legacy kick shape: 0xFF + short length (UTF-16 char count) + UTF-16BE string.
        if (readable < 3 || (buf.getUnsignedByte(buf.readerIndex()) != 0xFF)) {

            super.write(ctx, msg, promise);
            return;

        }

        final int readerIndex = buf.readerIndex();
        final byte[] raw = new byte[readable];
        buf.getBytes(readerIndex, raw);

        final String decoded = new String(raw, 3, readable - 3, StandardCharsets.UTF_16BE);
        if (!decoded.startsWith("§cOutdated client! Please use ")) {

            super.write(ctx, msg, promise);
            return;

        }

        final String rewritten = "§cOutdated client! Please use " + "1.21.11"; // TODO: make it read from config.
        final byte[] rewrittenBytes = rewritten.getBytes(StandardCharsets.UTF_16BE);

        final ByteBuf newBuf = ctx.alloc().buffer();
        newBuf.writeByte(0xFF);
        newBuf.writeShort(rewritten.length());
        newBuf.writeBytes(rewrittenBytes);

        // We're replacing this buffer entirely, release the original.
        buf.release();
        super.write(ctx, newBuf, promise);

    }

}
