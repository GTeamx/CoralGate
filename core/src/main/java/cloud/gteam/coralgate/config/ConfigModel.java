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

package cloud.gteam.coralgate.config;

public class ConfigModel {

    private String version;

    private final Prefixes prefixes = new Prefixes();

    private static class Prefixes {

        private String normal;
        private String warning;
        private String error;

    }

    private final APISettings apiSettings = new APISettings();

    private static class APISettings {

        private boolean allowUsage = true;
        private boolean healthCheck = true;
        private long cacheTime = 10;
        private String host = "https://api.gteam.cloud/coralgate/";
        private String version = "v2";
        private String triggerField = "blocked_status";
        private String triggerFieldValue = "true";

    }

    public void setConfigVersion(final String version) {
        this.version = version;
    }

    public String getConfigVersion() {
        return this.version;
    }

    public void setNormalPrefix(final String prefix) {
        this.prefixes.normal = prefix;
    }

    public String getNormalPrefix() {
        return this.prefixes.normal;
    }

    public void setWarningPrefix(final String prefix) {
        this.prefixes.warning = prefix;
    }

    public String getWarningPrefix() {
        return this.prefixes.warning;
    }

    public void setErrorPrefix(final String prefix) {
        this.prefixes.error = prefix;
    }

    public String getErrorPrefix() {
        return this.prefixes.error;
    }

    public void setAllowApiUsage(final boolean allowApiUsage) {
        this.apiSettings.allowUsage = allowApiUsage;
    }

    public boolean isAllowApiUsage() {
        return this.apiSettings.allowUsage;
    }

    public void setApiHealthCheck(final boolean apiHealthCheck) {
        this.apiSettings.healthCheck = apiHealthCheck;
    }

    public boolean isApiHealthCheck() {
        return this.apiSettings.healthCheck;
    }

    public void setApiCacheTime(final long apiCacheTime) {
        this.apiSettings.cacheTime = apiCacheTime;
    }

    public long getApiCacheTime() {
        return this.apiSettings.cacheTime;
    }

    public void setApiHost(final String apiHost) {
        this.apiSettings.host = apiHost;
    }

    public String getApiHost() {
        return this.apiSettings.host;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiSettings.version = apiVersion;
    }

    public String getApiVersion() {
        return this.apiSettings.version;
    }

    public void setApiTriggerField(final String triggerField) {
        this.apiSettings.triggerField = triggerField;
    }

    public String getApiTriggerField() {
        return this.apiSettings.triggerField;
    }

    public void setApiTriggerFieldValue(final String triggerFieldValue) {
        this.apiSettings.triggerFieldValue = triggerFieldValue;
    }

    public String getExpectedTriggerFieldValue() {
        return this.apiSettings.triggerFieldValue;
    }

}
