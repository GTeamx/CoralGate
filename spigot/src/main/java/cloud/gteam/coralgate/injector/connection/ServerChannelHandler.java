package cloud.gteam.coralgate.injector.connection;

import cloud.gteam.coralgate.injector.SpigotInjector;
import com.github.retrooper.packetevents.util.PEVersion;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Version;

import java.util.Map;

import static io.github.retrooper.packetevents.injector.connection.ServerChannelHandler.*;

public class ServerChannelHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Channel)) return;

        Channel channel = (Channel) msg;
        //Resolve netty version only once.

        if (NETTY_VERSION == null && !CHECKED_NETTY_VERSION) {
            NETTY_VERSION = resolveNettyVersion();
            CHECKED_NETTY_VERSION = true;
        }

//        Depends on netty version. If we cannot resolve that we just check server version.
        if ((NETTY_VERSION != null && NETTY_VERSION.isNewerThan(MODERN_NETTY_VERSION))
                || SpigotReflectionUtil.V_1_12_OR_HIGHER) {
            channel.pipeline().addLast(SpigotInjector.SERVER_CHANNEL_HANDLER_NAME, new PreChannelInitializer_v1_12());
        } else {
            channel.pipeline().addFirst(SpigotInjector.SERVER_CHANNEL_HANDLER_NAME, new PreChannelInitializer_v1_8());
        }
        super.channelRead(ctx, msg);
    }


    private static PEVersion resolveNettyVersion() {
        Map<String, Version> nettyArtifacts = Version.identify();

        Version version = nettyArtifacts.getOrDefault("netty-common", nettyArtifacts.get("netty-all"));

        if (version == null && !nettyArtifacts.isEmpty()) {
            version = nettyArtifacts.values().iterator().next();
        }

        if (version != null) {
            String stringVersion = version.artifactVersion();

            // Remove the ".Final" from the version by just removing any words (non numbers or dots)
            stringVersion = stringVersion.replaceAll("[^\\d.]", "");

            // Make sure stringVersion only contains 3 values like 4.2.0 but not 4.2.0.2
            String[] splitVersion = stringVersion.split("\\.");
            if (splitVersion.length > 3) {
                stringVersion = splitVersion[0] + "." + splitVersion[1] + "." + splitVersion[2];
            }

            // If the string ends with a dot, remove it
            stringVersion = stringVersion.endsWith(".") ? stringVersion.substring(0, stringVersion.length() - 1) : stringVersion;

            return PEVersion.fromString(stringVersion);
        }
        return null;
    }
}
