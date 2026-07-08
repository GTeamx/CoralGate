package cloud.gteam.coralgate.injector;

import cloud.gteam.coralgate.CorePlugin;
import cloud.gteam.coralgate.injector.connection.ServerChannelHandler;
import com.github.retrooper.packetevents.util.reflection.ReflectionObject;
import io.github.retrooper.packetevents.util.InjectedList;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoralGateInjector {
    public static String DECODER_NAME = "cg-decoder";
    public static String CONNECTION_HANDLER_NAME = "cg-connection-handler";
    public static String SERVER_CHANNEL_HANDLER_NAME = "cg-connection-initializer";

    //Channels that process connecting clients.
    public final Set<Channel> injectedConnectionChannels = new HashSet<>();
    private int connectionChannelsListIndex = -1;

    public boolean hasInjected = false;

    public boolean isServerBound() {
        //We want to check if the server has been bound to the port already.
        Object serverConnection = SpigotReflectionUtil.getMinecraftServerConnectionInstance();
        if (serverConnection != null) {
            ReflectionObject reflectServerConnection = new ReflectionObject(serverConnection);
            //There should only be 2 lists.
            for (int i = 0; i < 2; i++) {
                List<?> list = reflectServerConnection.readList(i);
                for (Object value : list) {
                    if (value instanceof ChannelFuture) {
                        connectionChannelsListIndex = i;
                        //Found the right list.
                        //It has connection channels, so the server has been bound.
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public void inject() {
        Object serverConnection = SpigotReflectionUtil.getMinecraftServerConnectionInstance();
        if (serverConnection != null) {
            ReflectionObject reflectServerConnection = new ReflectionObject(serverConnection);
            List<ChannelFuture> connectionChannelFutures = reflectServerConnection.readList(connectionChannelsListIndex);
            InjectedList<ChannelFuture> wrappedList = new InjectedList<>(connectionChannelFutures, future -> {
                //Each time a channel future is added, we run this.
                //This is automatically also ran for the elements already added before we wrapped the list.
                Channel channel = future.channel();
                //Inject into the server connection channel.
                injectServerChannel(channel);
                //Make sure to store it, so we can uninject later on.
                injectedConnectionChannels.add(channel);
            });
            //Replace the list with our wrapped one.
            reflectServerConnection.writeList(connectionChannelsListIndex, wrappedList);

            hasInjected = true;
        }
    }


    public void uninject() {
        //Uninject our connection handler from these connection channels.
        for (Channel connectionChannel : injectedConnectionChannels) {
            uninjectServerChannel(connectionChannel);
        }
        injectedConnectionChannels.clear();
        Object serverConnection = SpigotReflectionUtil.getMinecraftServerConnectionInstance();
        if (serverConnection != null) {
            ReflectionObject reflectServerConnection = new ReflectionObject(serverConnection);
            List<ChannelFuture> connectionChannelFutures = reflectServerConnection.readList(connectionChannelsListIndex);
            if (connectionChannelFutures instanceof InjectedList) {
                //Let us unwrap this. We no longer want to listen to connecting channels.
                reflectServerConnection.writeList(connectionChannelsListIndex,
                        ((InjectedList<ChannelFuture>) connectionChannelFutures).originalList());
            }
        }
    }

    private void injectServerChannel(Channel serverChannel) {
        ChannelPipeline pipeline = serverChannel.pipeline();
        ChannelHandler connectionHandler = pipeline.get(CoralGateInjector.CONNECTION_HANDLER_NAME);
        if (connectionHandler != null) {
            //Why does it already exist? Remove it.
            pipeline.remove(CoralGateInjector.CONNECTION_HANDLER_NAME);
        }
        //Make sure we handle connections after ProtocolSupport.
        if (pipeline.get("SpigotNettyServerChannelHandler#0") != null) {
            pipeline.addAfter("SpigotNettyServerChannelHandler#0", CoralGateInjector.CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        }
        //Make sure we handle connections after Geyser.
        else if (pipeline.get("floodgate-init") != null) {
            pipeline.addAfter("floodgate-init", CoralGateInjector.CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        }
        //Some forks add a handler which adds the other necessary vanilla handlers like (decoder, encoder, etc...)
        else if (pipeline.get("MinecraftPipeline#0") != null) {
            pipeline.addAfter("MinecraftPipeline#0", CoralGateInjector.CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        }
        //Otherwise, make sure we are first.
        else {
            pipeline.addFirst(CoralGateInjector.CONNECTION_HANDLER_NAME, new ServerChannelHandler());
        }
    }

    private void uninjectServerChannel(Channel serverChannel) {
        if (serverChannel.pipeline().get(CoralGateInjector.CONNECTION_HANDLER_NAME) != null) {
            serverChannel.pipeline().remove(CoralGateInjector.CONNECTION_HANDLER_NAME);
        } else {
            CorePlugin.getLogger().warning("Failed to uninject server channel, handler not found");
        }
    }

}
