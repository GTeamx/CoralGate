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

import cloud.gteam.coralgate.bridge.PlatformBridge;
import cloud.gteam.coralgate.processor.NetworkProcessor;

import java.util.logging.Logger;

public class PluginCore {

    private static final Logger logger = Logger.getLogger("CoralGate");

    private boolean onlineMode;
    private PlatformBridge platformBridge;

    private NetworkProcessor networkProcessor;

    public void onEnable(final boolean onlineMode) {

        logger.info("Startup sequence of CoralGate...");

        this.onlineMode = onlineMode;

        logger.info("Initializing NetworkProcessor...");

        networkProcessor = new NetworkProcessor(this);

        logger.info("NetworkProcessor initialized!");

        logger.info("CoralGate is ready to use!");

    }

    public static Logger getLogger() {
        return logger;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public PlatformBridge getPlatformBridge() {
        return this.platformBridge;
    }

    public void setPlatformBridge(final PlatformBridge platformBridge) {
        this.platformBridge = platformBridge;
        logger.info("New PlatformBridge initialized: " + platformBridge.getClass().getSimpleName());
    }

    public NetworkProcessor getNetworkProcessor() {
        return this.networkProcessor;
    }

}
