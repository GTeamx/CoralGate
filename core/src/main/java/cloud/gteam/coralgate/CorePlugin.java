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

package cloud.gteam.coralgate;

import cloud.gteam.coralgate.api.APIManager;
import cloud.gteam.coralgate.utils.ConfigUtils;

import java.util.Properties;
import java.util.logging.Logger;

public final class CorePlugin {

    private static Logger logger;

    private boolean onlineMode;
    private int compressionThreshold;

    private String platformName;
    private String platformVersion;
    private String apiVersion;

    private APIManager apiManager;

    public void onEnable(final Logger logger, final boolean onlineMode, final String configFileName, final Properties platformProperties) {

        CorePlugin.logger = logger;

        logger.info("Startup sequence of CoralGate...");

        this.platformName = platformProperties.getProperty("platform-name");
        this.platformVersion = platformProperties.getProperty("platform-version");
        this.apiVersion = platformProperties.getProperty("api-version");

        logger.info("Loading platform '" + this.platformName + "' version '" + this.platformVersion + "', implemented against core version '" + platformProperties.getProperty("core-version") + "'...");

        this.apiManager = new APIManager(this, this.apiVersion);

        logger.info("Using API version '" + this.apiVersion + "'.");

        this.onlineMode = onlineMode;

        this.compressionThreshold = ConfigUtils.getCompressionThreshold(configFileName);

        logger.info("CoralGate is ready to use!");

    }

    public void onDisable() {

        if (this.apiManager != null) this.apiManager.shutdown();

    }

    public static Logger getLogger() {
        return logger;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }

    public String getPlatformName() {
        return this.platformName;
    }

    public String getPlatformVersion() {
        return this.platformVersion;
    }

    public APIManager getApiManager() {
        return this.apiManager;
    }

}
