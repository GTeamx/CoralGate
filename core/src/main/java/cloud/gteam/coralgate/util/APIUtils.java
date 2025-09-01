/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2025 GTeamX (GTeam) and it's contributors
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

package cloud.gteam.coralgate.util;

import cloud.gteam.coralgate.PluginCore;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class APIUtils {

    private static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
    private static final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(10);

    public static CompletableFuture<Boolean> isIpBlocked(final String ipAddress) {

        final CachedResult cached = cache.get(ipAddress);

        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.blocked);
        }

        return fetchBlockedStatus(ipAddress).thenApply(blocked -> {

            cache.put(ipAddress, new CachedResult(blocked));
            return blocked;

        });

    }

    private static CompletableFuture<Boolean> fetchBlockedStatus(final String ipAddress) {
        return asyncHttpClient.prepareGet("https://api.gteam.cloud/coralgate/v1/" + ipAddress)
                .setHeader("User-Agent", "CoralGate-Minecraft")
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {

                    try {

                        final String body = response.getResponseBody();
                        final Any json = JsonIterator.deserialize(body);
                        return json.get("blocked").toBoolean();

                    } catch (final Exception e) {

                        PluginCore.getLogger().severe("Couldn't parse 'blocked' status from CoralGate's API. Did the API change? See error: " + e.getMessage());
                        return false;

                    }

                })
                .exceptionally(e -> {

                    PluginCore.getLogger().severe("Couldn't reach CoralGate's API. Is it down? See error: " + e.getMessage());
                    return false;

                });

    }

    public static void reportIp(final String ipAddress) {

        asyncHttpClient.prepareGet("https://api.gteam.cloud/coralgate/v1/" + ipAddress)
                .setHeader("User-Agent", "CoralGate-Minecraft-Plugin")
                .execute()
                .toCompletableFuture()
                .exceptionally(e -> {

                    PluginCore.getLogger().severe("Couldn't reach CoralGate's API. Is it down? See error: " + e.getMessage());
                    return null;

                });

    }


    private static class CachedResult {

        private final boolean blocked;
        private final long timestamp;

        public CachedResult(final boolean blocked) {
            this.blocked = blocked;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }

    }

}
