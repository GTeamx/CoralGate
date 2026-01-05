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

package cloud.gteam.coralgate.commands.permissions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.Lamp;
import revxrsal.commands.annotation.list.AnnotationList;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.command.CommandPermission;

public class PermissionFactory implements CommandPermission.Factory<CommandActor> {

    private final PermissionChecker permissionChecker;

    public PermissionFactory(final PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public @Nullable CommandPermission<CommandActor> create(final @NotNull AnnotationList annotations, final @NotNull Lamp<CommandActor> lamp) {

        final cloud.gteam.coralgate.commands.permissions.CommandPermission ann = annotations.get(cloud.gteam.coralgate.commands.permissions.CommandPermission.class);

        if (ann == null) return null;

        return (actor) -> permissionChecker.hasPermission(actor, ann.value());

    }

}
