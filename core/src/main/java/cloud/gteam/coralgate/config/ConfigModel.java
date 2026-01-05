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

package cloud.gteam.coralgate.config;

import blue.endless.jankson.Comment;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class ConfigModel {

    @Comment("Do not change this!")
    private String config_version = "0.0.6";

    private Prefixes prefixes = new Prefixes();

    public static class Prefixes {

        @Comment("Prefix used for commands when everything is working.")
        private String normal_prefix = "§b§lCoralGate §7» §r";

        @Comment("Prefix used for commands when a warning occurs.")
        private String warning_prefix = "§6§lCoralGate §7» §r";

        @Comment("Prefix used for commands when an error occurs.")
        private String error_prefix = "§c§lCoralGate §7» §r";

    }

    @Comment("\n" + "You need to restart your server in order to fully apply changes to the API settings." + "\n")
    private APISettings api_settings = new APISettings();

    public static class APISettings {

        @Comment("\n" + "Allow or not the usage of the CoralGate API service." + "\n" +
                "Not using the API will result in higher chances of bots extracting information out of your server." + "\n" +
                "We therefore highly recommend you keep this enabled." + "\n" + "\n" +
                "Expected value: true.")
        private boolean allow_api_usage = true;

        @Comment("\n" + "How long the local memory cache holds the IP." + "\n" +
                "If you have a large amount of requests or prefer having latest API data, reduce this." + "\n" +
                "The time is defined in minutes." + "\n" + "\n" +
                "Expected value: 10")
        private long api_cache_time = 10;

        @Comment("\n" + "The host server responsible for API queries." + "\n" +
                "If you own or self-host an API that returns a specific JSON encoded field (which can be changed bellow), feel free to use it." + "\n" +
                "BEWARE: Putting other servers here might result in your data leaking or server being compromised, keeping the default server is recommended." + "\n" + "\n" +
                "Expected value: 'https://api.gteam.cloud/coralgate/'")
        private String api_host = "https://api.gteam.cloud/coralgate/";

        @Comment("\n" + "The API version used." + "\n" +
                "If you want to use newer development or older versions of the API." + "\n" +
                "Keeping this version to the latest is recommended. If you happen to use other versions, make sure the expected trigger is properly defined." + "\n" + "\n" +
                "Expected value: 'v2'")
        private String api_version = "v2";

        @Comment("\n" + "The field the API sends that contains the blocked status." + "\n" +
                "If you use special API versions or your own API, you may need to change this. Otherwise, don't." + "\n" + "\n" +
                "Expected value: 'blocked_status'")
        private String expected_trigger_field = "blocked_status";

        @Comment("\n" + "The value of the previous field required to declare an IP as blocked." + "\n" +
                "If you use special API versions or your own API, you may need to change this. Otherwise, don't." + "\n" + "\n" +
                "Expected value: 'true'")
        private String expected_trigger_field_value = "true";

    }

    @Comment("\n" + "You need to restart your server in order to fully apply changes to the filter settings." + "\n")
    private FilterSettings filter_settings = new FilterSettings();

    public static class FilterSettings {



    }

    public String getConfigVersion() {
        return this.config_version;
    }

    public String getNormalPrefix() {
        return this.prefixes.normal_prefix;
    }

    public String getWarningPrefix() {
        return this.prefixes.warning_prefix;
    }

    public String getErrorPrefix() {
        return this.prefixes.error_prefix;
    }

    public boolean isAllowApiUsage() {
        return this.api_settings.allow_api_usage;
    }

    public long getApiCacheTime() {
        return this.api_settings.api_cache_time;
    }

    public String getApiHost() {
        return this.api_settings.api_host;
    }

    public String getApiVersion() {
        return this.api_settings.api_version;
    }

    public String getExpectedTriggerField() {
        return this.api_settings.expected_trigger_field;
    }

    public String getExpectedTriggerFieldValue() {
        return this.api_settings.expected_trigger_field_value;
    }

}
