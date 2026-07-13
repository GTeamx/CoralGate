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

package cloud.gteam.coralgate.injector;

import cloud.gteam.coralgate.CorePlugin;
import cloud.gteam.coralgate.injector.connection.VelocityChannelInitializer;
import com.github.retrooper.packetevents.util.reflection.Reflection;
import com.github.retrooper.packetevents.util.reflection.ReflectionObject;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.configurate.util.CheckedConsumer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

@ApiStatus.Internal
public class VelocityInjector {

    public static final String DECODER_NAME = "cg-decoder";

    private static Class<?> CONNECTION_MANAGER_CLASS, SERVER_INITIALIZER_HOLDER_CLASS;
    private static Method SET_SERVER_INITIALIZER;

    public boolean hasInjected = false;

    private final ProxyServer server;

    public VelocityInjector(final ProxyServer server) {
        this.server = server;
    }

    public void inject() {

        if (CONNECTION_MANAGER_CLASS == null) {

            CONNECTION_MANAGER_CLASS = Reflection.getClassByNameWithoutException("com.velocitypowered.proxy.network.ConnectionManager");
            SERVER_INITIALIZER_HOLDER_CLASS = Reflection.getClassByNameWithoutException("com.velocitypowered.proxy.network.ServerChannelInitializerHolder");
            SET_SERVER_INITIALIZER = Reflection.getMethod(SERVER_INITIALIZER_HOLDER_CLASS, 0, ChannelInitializer.class);

        }

        final Supplier<ChannelInitializer<Channel>> initializerHolder = getServerChannelInitializerHolder();
        final ChannelInitializer<Channel> wrappedProxyInitializer = initializerHolder.get();
        final VelocityChannelInitializer initializer = new VelocityChannelInitializer(wrappedProxyInitializer);
        try {

            SET_SERVER_INITIALIZER.invoke(initializerHolder, initializer);
            hasInjected = true;

        } catch (final IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void uninject() {

        final Supplier<ChannelInitializer<Channel>> holder = this.getServerChannelInitializerHolder();
        ChannelInitializer<?> wrapper = holder.get();
        CheckedConsumer<ChannelInitializer<Channel>, ReflectiveOperationException> uninjector = (initializer) -> {

            CorePlugin.getLogger().info("Uninjecting from Velocity channel initializer...");
            SET_SERVER_INITIALIZER.invoke(holder, initializer);

        };

        try {

            while (true) {

                // Check if it's our initializer, could be wrapped by other plugins.
                if (wrapper instanceof VelocityChannelInitializer wrappedInitializer) {

                    uninjector.accept(wrappedInitializer.getWrappedInitializer());
                    break;

                } else {

                    // Walk up wrapper tree, if possible to find a single matching field.
                    // This accounts for other plugins (e.g. ViaVersion) also replacing the injector, which may
                    // wrap our already wrapped injector.
                    final Field field = Reflection.getField(wrapper.getClass(), ChannelInitializer.class, 0);
                    if (field == null) {
                        throw new IllegalStateException("Can't unwrap foreign channel initializer: " + wrapper);
                    }

                    field.setAccessible(true);

                    final ChannelInitializer<?> thisWrapper = wrapper;
                    wrapper = (ChannelInitializer<?>) field.get(thisWrapper);
                    uninjector = initializer -> {

                        field.set(thisWrapper, initializer);
                        CorePlugin.getLogger().info("Uninjected from plugin channel initializer " + thisWrapper);

                    };

                }

            }

        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException("Failed to uninject from frontend pipeline", e);
        }

    }

    private Supplier<ChannelInitializer<Channel>> getServerChannelInitializerHolder() {

        final ReflectionObject reflectServer = new ReflectionObject(server);
        final Object connectionManager = reflectServer.readObject(0, CONNECTION_MANAGER_CLASS);
        final ReflectionObject reflectConnectionManager = new ReflectionObject(connectionManager);
        return (Supplier<ChannelInitializer<Channel>>) reflectConnectionManager.readObject(0, SERVER_INITIALIZER_HOLDER_CLASS);

    }

}
