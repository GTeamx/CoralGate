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