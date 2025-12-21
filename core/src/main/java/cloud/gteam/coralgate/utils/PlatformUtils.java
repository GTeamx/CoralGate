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

package cloud.gteam.coralgate.utils;

import cloud.gteam.coralgate.CorePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PlatformUtils {

    public static Properties loadProperties(final Class<?> pluginClass) {

        final Properties props = new Properties();

        try (final InputStream is = pluginClass.getClassLoader().getResourceAsStream("platform.properties")) {
            props.load(is);
        } catch (final IOException e) {
            CorePlugin.getLogger().severe("Couldn't fetch 'platform.properties' file. CoralGate might not work as expected. See error: " + e.getMessage());
        }

        return props;

    }

}
