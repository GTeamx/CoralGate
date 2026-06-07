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

package cloud.gteam.coralgate.update;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import cloud.gteam.coralgate.CorePlugin;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final AsyncHttpClient httpClient;
    private final CorePlugin corePlugin;
    private final Jankson jankson;

    private String latestVersion = null;

    public UpdateChecker(final CorePlugin corePlugin) {
        this.corePlugin = corePlugin;
        this.jankson = Jankson.builder().build();
        this.httpClient = Dsl.asyncHttpClient(Dsl.config()
                .setConnectTimeout(3000)
                .setRequestTimeout(3000)
                .build());
    }

    public CompletableFuture<Boolean> isUpToDate() {

        final String currentVersion = this.corePlugin.getPlatformProperties().getProperty("platform-version");

        return this.httpClient.prepareGet("https://api.github.com/repos/GTeamX/CoralGate/releases/latest")
                .setHeader("User-Agent", "CoralGate-UpdateChecker/" + currentVersion)
                .setHeader("Accept", "application/vnd.github+json")
                .execute()
                .toCompletableFuture()
                .thenApply(response -> {

                    try {

                        final JsonObject json = this.jankson.load(response.getResponseBody());

                        // Assume not up to date if API fails.
                        if (json.get("tag_name") == null) {
                            return false;
                        }

                        this.latestVersion = Objects.requireNonNull(json.get("tag_name")).toJson(false, false).replace("\"", "");

                        return this.latestVersion.equalsIgnoreCase(currentVersion);

                    } catch (final Exception e) {

                        CorePlugin.getLogger().warning("Failed to parse update check. See error: " + e.getMessage());
                        return false;

                    }

                })
                .exceptionally(e -> {

                    CorePlugin.getLogger().warning("Update check failed. See error: " + e.getMessage());
                    return false;

                });
    }

    public void shutdown() {

        try {
            if (!this.httpClient.isClosed()) this.httpClient.close();
        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Error closing UpdateChecker client: " + e.getMessage());
        }

    }

    public String getLatestVersion() {
        return this.latestVersion;
    }

}
