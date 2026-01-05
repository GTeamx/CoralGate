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

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import cloud.gteam.coralgate.CorePlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ConfigManager {

    private final File dataFolder;
    private final File configFile;
    private ConfigModel config;
    private final Jankson jankson;

    public ConfigManager(final File dataFolder, final String configFileName) {
        this.dataFolder = dataFolder;
        this.configFile = new File(dataFolder, configFileName);
        this.jankson = Jankson.builder().build();
    }

    // Load config file, create a new one if it doesn't exist.
    public void load() {

        if (!this.configFile.exists()) {

            CorePlugin.getLogger().info("Couldn't find config.json file. Generating default config...");

            // Generate new config and save it.
            this.config = new ConfigModel();
            save();

            return;

        }

        try {

            // Read config file.
            final JsonObject jsonObject = this.jankson.load(configFile);
            this.config = this.jankson.fromJson(jsonObject, ConfigModel.class);
            CorePlugin.getLogger().info("Successfully loaded config.json!");

        } catch (final Exception e) {

            CorePlugin.getLogger().severe("Couldn't load config.json. Did the file get corrupted? See error: " + e.getMessage());

            // Fallback to avoid NullPointerException.
            this.config = new ConfigModel();

        }

    }

    public void save() {

        try {

            // Generate data folder if it doesn't exist.
            if (!this.configFile.getParentFile().exists()) {
                if (!this.configFile.getParentFile().mkdirs()) CorePlugin.getLogger().severe("Couldn't create data folders. Is the directory read-only? No error to display.");
            }

            final String json = this.jankson.toJson(this.config).toJson(true, true);

            // Append header.
            final String header = "//" + "\n" +
                    "// CoralGate - https://github.com/GTeamX/CoralGate" + "\n" +
                    "// Copyright (C) 2025 GTeamX." + "\n" +
                    "// Configuration file." + "\n" +
                    "//" + "\n" + "\n";

            // Write file and data.
            Files.write(this.configFile.toPath(), (header + json).getBytes(StandardCharsets.UTF_8));

        } catch (final Exception e) {
            CorePlugin.getLogger().severe("Couldn't write data to config.json. Is the directory read-only? See error: " + e.getMessage());
        }

    }

    public File getDataFolder() {
        return this.dataFolder;
    }

    public File getConfigFile() {
        return this.configFile;
    }

    public ConfigModel getConfig() {
        return this.config;
    }

}
