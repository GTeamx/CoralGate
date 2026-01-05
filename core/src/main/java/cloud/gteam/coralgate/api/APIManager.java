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

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import cloud.gteam.coralgate.CorePlugin;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class APIManager {

    private final AsyncHttpClient httpClient;
    private final Map<String, CacheEntry> ipCache = new ConcurrentHashMap<>();
    private final long cacheTime;
    private final String baseUrl;

    private final CorePlugin corePlugin;
    private final Jankson jankson;

    public APIManager(final CorePlugin corePlugin)
    {
        this.corePlugin = corePlugin;
        this.cacheTime = TimeUnit.MINUTES.toMillis(this.corePlugin.getConfigManager().getConfig().getApiCacheTime());
        this.httpClient = Dsl.asyncHttpClient();
        this.baseUrl = this.corePlugin.getConfigManager().getConfig().getApiHost() + this.corePlugin.getConfigManager().getConfig().getApiVersion() + "/";
        this.jankson = Jankson.builder().build();
    }

    public CompletableFuture<Boolean> isIpBlocked(final String ipAddress) {

        // Get IP from cache before fetching from API.
        final CacheEntry entry = this.ipCache.get(ipAddress);

        // Serve cache if available.
        if (entry != null && !entry.isExpired(this.cacheTime)) {
            return CompletableFuture.completedFuture(entry.isBlocked());
        }

        // Cache not available, fetch from API.
        return fetchFromApi(ipAddress).thenApply(result -> {
            this.ipCache.put(ipAddress, new CacheEntry(result));
            return result;
        });

    }

    public boolean isIpBlockedCache(final String ipAddress) {
        final CacheEntry entry = this.ipCache.get(ipAddress);
        return entry != null && !entry.isExpired(this.cacheTime) && !entry.isBlocked();
    }

    public void reportIp(final String ipAddress) {
        fetchFromApi(ipAddress);
    }

    private CompletableFuture<Boolean> fetchFromApi(final String ipAddress) {

        if (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage()) CompletableFuture.completedFuture(false);

        try {

            final InetAddress inetAddress = InetAddress.getByName(ipAddress);

            if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) return CompletableFuture.completedFuture(false);

        } catch (final UnknownHostException e) {
            CorePlugin.getLogger().severe("Couldn't parse IP address. Is the API properly configured? See error: " + e.getMessage());
        }

        return this.httpClient.prepareGet(this.baseUrl + ipAddress)
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

        final String expectedField = this.corePlugin.getConfigManager().getConfig().getExpectedTriggerField();

        try {

            // Load JSON response from API.
            final JsonObject json = this.jankson.load(response.getResponseBody());

            // Get expected field.
            final JsonElement field = json.get(expectedField);

            if (field == null) {
                CorePlugin.getLogger().severe("Couldn't find field '" + expectedField + "' in API's JSON response. Did the API change? No error to display.");
                return false;
            }

            // Compare values.
            String actualValue = field.toJson(false, false);
            final String expectedValue = this.corePlugin.getConfigManager().getConfig().getExpectedTriggerFieldValue();

            // Jankson usually wraps strings in quotes, we strip them for a clean comparison.
            actualValue = actualValue.replace("\"", "");

            return Objects.equals(actualValue, expectedValue);

        } catch (final Exception e) {
            CorePlugin.getLogger().severe("Couldn't parse '" + expectedField + "' status from CoralGate's API. Did the API change? See error: " + e.getMessage());
            return false;
        }

    }

    public void shutdown() {

        try {
            this.httpClient.close();
        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't close AsyncHttpClient. See error: " + e.getMessage());
        }

    }

}
