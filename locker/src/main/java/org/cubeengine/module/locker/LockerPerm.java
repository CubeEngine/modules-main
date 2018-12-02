/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.locker;

import javax.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.locker.commands.LockerCommands;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class LockerPerm extends PermissionContainer
{
    @Inject
    public LockerPerm(PermissionManager pm)
    {
        super(pm, Locker.class);
    }

    public final Permission ALLOW_CONTAINER = register("allow.container", "", null);
    public final Permission ALLOW_DOOR = register("allow.door", "", null);
    public final Permission ALLOW_LIVING_ENTITY = register("allow.living-entity", "Allows interaction with living entities", null);
    public final Permission ALLOW_ENTITY = register("allow.entity", "Allows interaction with non living entities", null);
    public final Permission ALLOW_HANGING = register("allow.hanging", "", null);

    public final Permission SHOW_OWNER = register("show-owner", "", null);
    public final Permission BREAK_OTHER = register("break-other", "", null);
    public final Permission ACCESS_OTHER = register("access-other", "", null);
    public final Permission EXPAND_OTHER = register("expand-other", "", null);

    public final Permission PREVENT_NOTIFY = register("prevent-notify", "", null);

    public final Permission CMD_REMOVE_OTHER = register("command.locker.remove.other", "", null);
    public final Permission CMD_KEY_OTHER = register("command.locker.key.other", "", null);
    public final Permission CMD_MODIFY_OTHER = register("command.locker.modify.other", "", null);
    public final Permission CMD_GIVE_OTHER = register("command.locker.give.other", "", null);

    public final Permission CMD_INFO_OTHER = register("command.locker.info.other", "", null);
    public final Permission CMD_INFO_SHOW_OWNER =  register("command.locker.info.show-owner", "", null);
}
