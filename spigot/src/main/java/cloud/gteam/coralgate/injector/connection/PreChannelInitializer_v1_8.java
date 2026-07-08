package cloud.gteam.coralgate.injector.connection;

import com.github.retrooper.packetevents.protocol.ConnectionState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class PreChannelInitializer_v1_8 extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                ServerConnectionInitializer.initChannel(channel, ConnectionState.HANDSHAKING);
            }
        });
    }
}