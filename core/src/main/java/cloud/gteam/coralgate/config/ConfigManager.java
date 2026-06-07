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

import cloud.gteam.coralgate.CorePlugin;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigManager {

    private final String latestConfigVersion = "0.2.0";

    private final String configFileName;
    private final File configFile;
    private YamlDocument document;
    private ConfigModel config;

    public ConfigManager(final File dataFolder, final String configFileName) {
        this.configFileName = configFileName;
        this.configFile = new File(dataFolder, configFileName);
    }

    public void load() {

        try {

            // Grab the pristine template file out of your plugin's resources/jar.
            final InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(this.configFileName);

            if (defaultStream == null) {
                CorePlugin.getLogger().severe("Could not find default resource file: " + this.configFileName);
                return;
            }

            this.config = new ConfigModel();

            // Create, load, and automatically update the config file.
            this.document = YamlDocument.create(
                    this.configFile,
                    defaultStream,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.DEFAULT
            );

            mapFields();

            CorePlugin.getLogger().info("Successfully loaded " + this.configFileName + "!");

        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't load " + this.configFileName + ". Did the file get corrupted? See error: " + e.getMessage());
        }

    }

    private void mapFields() {

        if (this.document == null) return;

        this.config.setConfigVersion(this.document.getString("version", "0.2.0"));

        this.config.setNormalPrefix(this.document.getString("prefixes.normal", "§b§lCoralGate §7» §r"));
        this.config.setWarningPrefix(this.document.getString("prefixes.warning", "§6§lCoralGate §7» §r"));
        this.config.setErrorPrefix(this.document.getString("prefixes.error", "§c§lCoralGate §7» §r"));

        this.config.setAllowApiUsage(this.document.getBoolean("api-settings.allow-usage", true));
        this.config.setApiHealthCheck(this.document.getBoolean("api-settings.health-check", true));
        this.config.setApiCacheTime(this.document.getInt("api-settings.cache-time", 10));
        this.config.setApiHost(this.document.getString("api-settings.host", "https://api.gteam.cloud/coralgate/"));
        this.config.setApiVersion(this.document.getString("api-settings.version", "v2"));
        this.config.setApiTriggerField(this.document.getString("api-settings.trigger-field", "blocked_status"));
        this.config.setApiTriggerFieldValue(this.document.getString("api-settings.trigger-field-value", "true"));

    }

    public void save() {

        try {

            if (!this.configFile.getParentFile().exists()) {
                if (!this.configFile.getParentFile().mkdirs()) CorePlugin.getLogger().severe("Couldn't create data folders. Is the directory read-only? No error to display.");
            }

            if (this.document != null) this.document.save();

        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't write data to " + this.configFileName + ". Is the directory read-only? See error:" + e.getMessage());
        }

    }

    public String getLatestConfigVersion() {
        return this.latestConfigVersion;
    }

    public ConfigModel getConfig() {
        return this.config;
    }

}
