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

package cloud.gteam.coralgate.api;

import cloud.gteam.coralgate.CorePlugin;
import com.jsoniter.JsonIterator;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class APIManager {

    private final AsyncHttpClient httpClient;
    private final Map<String, CacheEntry> ipCache = new ConcurrentHashMap<>();
    private final long cacheTime = TimeUnit.MINUTES.toMillis(10);
    private final String baseUrl;

    private final CorePlugin corePlugin;

    public APIManager(final CorePlugin corePlugin, final String apiVersion) {
        this.corePlugin = corePlugin;
        this.baseUrl = "https://api.gteam.cloud/coralgate/" + apiVersion + "/";
        this.httpClient = Dsl.asyncHttpClient();
    }

    public CompletableFuture<Boolean> isIpBlocked(final String ipAddress) {

        // Get IP from cache before fetching from API.
        final CacheEntry entry = ipCache.get(ipAddress);

        // Serve cache if available
        if (entry != null && !entry.isExpired(cacheTime)) {
            return CompletableFuture.completedFuture(entry.isBlocked());
        }

        // Cache not available, fetch from API
        return fetchFromApi(ipAddress).thenApply(result -> {
            ipCache.put(ipAddress, new CacheEntry(result));
            return result;
        });

    }

    public void reportIp(final String ipAddress) {
        fetchFromApi(ipAddress);
    }

    private CompletableFuture<Boolean> fetchFromApi(final String ipAddress) {
        return httpClient.prepareGet(baseUrl + ipAddress)
                .setHeader("User-Agent", "CoralGate-Plugin/" + this.corePlugin.getPlatformVersion() + "/" + this.corePlugin.getPlatformName() + " (Minecraft Server)")
                .execute()
                .toCompletableFuture()
                .thenApply(this::parseBlockedResponse)
                .exceptionally(e -> {
                    CorePlugin.getLogger().severe("Couldn't reach CoralGate's API. Is it down? See error: " + e.getMessage());
                    return false;
                });
    }

    private boolean parseBlockedResponse(final Response response) {

        try {
            return JsonIterator.deserialize(response.getResponseBody()).get("blocked_status").toBoolean();
        } catch (final Exception e) {
            CorePlugin.getLogger().warning("Couldn't parse 'blocked' status from CoralGate's API. Did the API change? See error: " + e.getMessage());
            return false;
        }

    }

    public void shutdown() {

        try {
            this.httpClient.close();
        } catch (final IOException e) {
            CorePlugin.getLogger().warning("Couldn't close AsyncHttpClient. See error: " + e.getMessage());
        }

    }

}
