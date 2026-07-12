/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2026 GTeamX (GTeam) and it's contributors
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class APIManager {

    private final Set<CompletableFuture<?>> pendingFutures = ConcurrentHashMap.newKeySet();

    private final AsyncHttpClient httpClient;
    private final Map<String, CacheEntry> ipCache = new ConcurrentHashMap<>();
    private final long cacheTime;
    private final String baseUrl;
    private boolean healthStatus = false;

    private final CorePlugin corePlugin;
    private final Jankson jankson;

    public APIManager(final CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
        this.cacheTime = TimeUnit.MINUTES.toMillis(this.corePlugin.getConfigManager().getConfig().getApiCacheTime());
        this.httpClient = Dsl.asyncHttpClient(Dsl.config()
                .setConnectTimeout(3000)
                .setRequestTimeout(3000)
                .setReadTimeout(3000)
                .build());
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

            if (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage()) {
                this.ipCache.put(ipAddress, new CacheEntry(result));
            }

            return result;

        });

    }

    public boolean isIpCached(final String ipAddress) {
        final CacheEntry entry = this.ipCache.get(ipAddress);
        return entry != null && entry.isExpired(this.cacheTime);
    }

    public boolean isIpCachedBlocked(final String ipAddress) {
        return isIpCached(ipAddress) && this.ipCache.get(ipAddress).isBlocked();
    }

    public void checkIp(final String ipAddress) {
        fetchFromApi(ipAddress).thenApply(result -> {

            if (this.corePlugin.getConfigManager().getConfig().isAllowApiUsage()) {
                this.ipCache.put(ipAddress, new CacheEntry(result));
            }

            return null;

        });
    }

    private CompletableFuture<Boolean> fetchFromApi(final String ipAddress) {

        if (!this.corePlugin.getConfigManager().getConfig().isAllowApiUsage()) {
            return CompletableFuture.completedFuture(false); // Not blocked.
        }

        try {

            final InetAddress inetAddress = InetAddress.getByName(ipAddress);
            if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
                return CompletableFuture.completedFuture(false); // Not blocked.
            }

        } catch (final UnknownHostException e) {

            CorePlugin.getLogger().severe("Couldn't parse IP address. Is the API properly configured? See error: " + e.getMessage());
            this.healthStatus = false;

            // Not blocked.
            return CompletableFuture.completedFuture(false);

        }

        final CompletableFuture<Boolean> future = this.httpClient.prepareGet(this.baseUrl + ipAddress)
                .setHeader("User-Agent", "CoralGate-Plugin/" + this.corePlugin.getPlatformProperties().getProperty("platform-version") + "/" + this.corePlugin.getPlatformProperties().getProperty("platform-name") + " (Minecraft Server)")
                .execute()
                .toCompletableFuture()
                .thenApply(this::parseBlockedResponse)
                .exceptionally(e -> {
                    CorePlugin.getLogger().severe("Couldn't reach API. Is it down? See error: " + e.getMessage());
                    this.healthStatus = false;
                    return false; // Not blocked.
                });

        // Keep track of requests to cleanly clear them on shutdown.
        this.pendingFutures.add(future);
        future.whenComplete((res, exception) -> this.pendingFutures.remove(future));

        return future;

    }

    private boolean parseBlockedResponse(final Response response) {

        final String expectedField = this.corePlugin.getConfigManager().getConfig().getApiTriggerField();

        try {

            // Load JSON response from API.
            final JsonObject json = this.jankson.load(response.getResponseBody());

            // Get expected field.
            final JsonElement field = json.get(expectedField);

            if (field == null) {

                CorePlugin.getLogger().severe("Couldn't find field '" + expectedField + "' in API's JSON response. Did the API change? No error to display.");
                this.healthStatus = false;

                return false; // Not blocked.

            }

            // Compare values.
            String actualValue = field.toJson(false, false);
            final String expectedValue = this.corePlugin.getConfigManager().getConfig().getExpectedTriggerFieldValue();

            // Jankson usually wraps strings in quotes, we strip them for a clean comparison.
            actualValue = actualValue.replace("\"", "");

            this.healthStatus = true;

            return Objects.equals(actualValue, expectedValue); // Blocked.

        } catch (final Exception e) {

            CorePlugin.getLogger().severe("Couldn't parse '" + expectedField + "' status from API. Did the API change? See error: " + e.getMessage());
            this.healthStatus = false;

            return false; // Not blocked.

        }

    }

    public CompletableFuture<Boolean> checkHealth() {

        final String healthUrl = this.baseUrl + "0.0.0.0";

        return this.httpClient.prepareGet(healthUrl)
                .setHeader("User-Agent", "CoralGate-Plugin/" + this.corePlugin.getPlatformProperties().getProperty("platform-version") + "/" + this.corePlugin.getPlatformProperties().getProperty("platform-name") + "/health (Minecraft Server)")
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {

                    try {

                        // Load the JSON response.
                        final JsonObject json = this.jankson.load(response.getResponseBody());

                        // Extract the "health" field.
                        final JsonElement healthField = json.get("health");

                        if (healthField == null) {
                            return false;
                        }

                        // Clean the value and compare to "OK".
                        this.healthStatus = "OK".equalsIgnoreCase(healthField.toJson(false, false).replace("\"", ""));
                        return this.healthStatus;

                    } catch (final Exception e) {

                        CorePlugin.getLogger().warning("Health check failed to parse: " + e.getMessage());
                        return false;

                    }

                })
                .exceptionally(e -> {

                    CorePlugin.getLogger().severe("API Health check request failed: " + e.getMessage());
                    return false;

                });

    }

    public void shutdown() {

        // Forcefully cancel any HTTP callbacks still hanging around.
        for (final CompletableFuture<?> forFuture : this.pendingFutures) {

            if (!forFuture.isDone()) {
                forFuture.cancel(true);
            }

        }

        this.pendingFutures.clear();

        try {

            if (!this.httpClient.isClosed()) {
                this.httpClient.close();
            }

        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't close AsyncHttpClient. See error: " + e.getMessage());
        }

    }

    public boolean isHealthy() {
        return this.healthStatus;
    }

}
