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
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;

import java.io.File;

public class ConfigManager {

    private final File dataFolder;
    private final File configFile;
    private ConfigModel config;
    private final HoconConfigurationLoader hoconConfigurationLoader;

    public ConfigManager(final File dataFolder, final String configFileName) {
        this.dataFolder = dataFolder;
        this.configFile = new File(dataFolder, configFileName);
        this.hoconConfigurationLoader = HoconConfigurationLoader .builder()
                .path(this.configFile.toPath())
                .indent(2)
                .defaultOptions(options -> options
                        .shouldCopyDefaults(true)
                        .header("CoralGate - https://github.com/GTeamX/CoralGate\nCopyright (C) 2026 GTeamX.\nConfiguration file.")
                        .serializers(s -> s.registerAnnotatedObjects(ObjectMapper.factory()))
                )
                .build();
    }

    public void load() {

        // Load config file, create a new one if it doesn't exist.
        if (!this.configFile.exists()) {

            CorePlugin.getLogger().info("Couldn't find config.yml file. Generating default config...");

            this.config = new ConfigModel();
            save();

            return;

        }

        try {

            // Read config file into a node.
            final CommentedConfigurationNode node = this.hoconConfigurationLoader.load();

            // Map the node to POJO.
            this.config = node.get(ConfigModel.class);

            if (this.config == null) this.config = new ConfigModel();

            CorePlugin.getLogger().info("Successfully loaded config.yml!");

        } catch (final Exception e) {

            CorePlugin.getLogger().severe("Couldn't load config.yml. Did the file get corrupted? See error: " + e.getMessage());

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

            // Map the POJO back to a node.
            final CommentedConfigurationNode node = this.hoconConfigurationLoader.createNode(this.hoconConfigurationLoader.defaultOptions());
            node.set(ConfigModel.class, this.config);

            // Write node to file.
            this.hoconConfigurationLoader.save(node);

        } catch (final Exception e) {
            CorePlugin.getLogger().severe("Couldn't write data to config.yml. Is the directory read-only? See error: " + e.getMessage());
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
