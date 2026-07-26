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

package cloud.gteam.coralgate;

import cloud.gteam.coralgate.api.APIManager;
import cloud.gteam.coralgate.config.ConfigManager;
import cloud.gteam.coralgate.injector.NettyResponder;
import cloud.gteam.coralgate.update.UpdateChecker;
import cloud.gteam.coralgate.utils.ConfigUtils;
import com.github.retrooper.packetevents.PacketEvents;

import java.io.File;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class CorePlugin {

    private static Logger logger;

    private boolean testMode;

    private boolean onlineMode;
    private int compressionThreshold;

    private Properties platformProperties;

    private NettyResponder nettyResponder;

    private ConfigManager configManager;
    private APIManager apiManager;
    private UpdateChecker updateChecker;

    public void onEnable(final Logger logger, final File dataFolder, final boolean onlineMode, final String configFileName, final Properties platformProperties, final NettyResponder nettyResponder) {

        CorePlugin.logger = logger;

        logger.info("Startup sequence of CoralGate...");

        this.platformProperties = platformProperties;

        logger.info("Loading platform '" + platformProperties.getProperty("platform-name") + "' version '" + platformProperties.getProperty("platform-version") + "', implemented against core version '" + platformProperties.getProperty("core-version") + "'...");

        this.nettyResponder = nettyResponder;

        final String peCoreVersion = platformProperties.getProperty("packetevents-version");
        final String peServerVersion = PacketEvents.getAPI().getVersion().toString();

        // packetevents versions do not match.
        if (!peCoreVersion.equals(peServerVersion)) {
            logger.warning("packetevents version mismatch! You are using version '" + peServerVersion + "' but core module uses '" + peCoreVersion + "'! You may experience issues or bugs. Update CoralGate and packetevents to fix this issue.");
        } else {
            logger.info("Using packetevents version '" + peCoreVersion + "'...");
        }

        this.testMode = new File(dataFolder, "test.mode").exists();

        this.configManager = new ConfigManager(dataFolder, "config.yml");
        this.configManager.load();

        // Get latest config file version.
        final String latestConfigVersion = this.configManager.getLatestConfigVersion();

        // Compare to internal configuration version to see if it's outdated.
        if (!Objects.equals(latestConfigVersion, this.configManager.getConfig().getConfigVersion())) {
            logger.warning("Please consider upgrading your configuration file to the latest version: '" + latestConfigVersion + "'.");
        }

        logger.info("Using configuration file version '" + this.configManager.getConfig().getConfigVersion() + "'.");

        if (this.configManager.getConfig().isAllowApiUsage()) {

            this.apiManager = new APIManager(this);

            logger.info("Loading API version '" + this.getConfigManager().getConfig().getApiVersion() + "', implemented against host '" + this.getConfigManager().getConfig().getApiHost() + "'...");

            if (this.configManager.getConfig().isApiHealthCheck()) {

                this.apiManager.checkHealth().thenAccept(isHealthy -> {

                    if (isHealthy) {
                        CorePlugin.getLogger().info("API connection is healthy!");
                    } else {
                        CorePlugin.getLogger().warning("API returned unhealthy status. Is it down ? No error to display.");
                    }

                });

            } else {
                logger.info("API health check skipped.");
            }

        } else {
            logger.info("API loading skipped (disabled by config).");
        }

        logger.info("Reading online-mode and compression threshold...");

        this.onlineMode = onlineMode;

        this.compressionThreshold = ConfigUtils.getCompressionThreshold(configFileName);

        logger.info("Loading update checker...");

        this.updateChecker = new UpdateChecker(this);

        Executors.newSingleThreadScheduledExecutor().schedule(() -> this.updateChecker.checkForUpdates(), 3, java.util.concurrent.TimeUnit.SECONDS);

        logger.info("CoralGate is ready to use!");

    }

    public void onDisable() {

        if (this.apiManager != null) {
            this.apiManager.shutdown();
        }

        if (this.updateChecker != null) {
            this.updateChecker.shutdown();
        }

    }

    public static Logger getLogger() {
        return logger;
    }

    public boolean isTestMode() {
        return this.testMode;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }

    public Properties getPlatformProperties() {
        return this.platformProperties;
    }

    public NettyResponder getNettyResponder() {
        return this.nettyResponder;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public APIManager getApiManager() {
        return this.apiManager;
    }

    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }

}
