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
    public LockerPerm(PermissionManager pm, LockerCommands mainCmd)
    {
        super(pm, Locker.class);
    }

    public final Permission ALLOW_CONTAINER = register("allow.container", "", null);
    public final Permission ALLOW_DOOR = register("allow.door", "", null);
    public final Permission ALLOW_ENTITY = register("allow.entity", "", null);
    public final Permission ALLOW_HANGING = register("allow.hanging", "", null);

    public final Permission SHOW_OWNER = register("show-owner", "", null);
    public final Permission BREAK_OTHER = register("break-other", "", null);
    public final Permission ACCESS_OTHER = register("access-other", "", null);
    public final Permission EXPAND_OTHER = register("expand-other", "", null);

    public final Permission PREVENT_NOTIFY = register("prevent-notify", "", null);

    private final Permission COMMAND = register("command", "", null);

    public final Permission CMD_REMOVE_OTHER = register("locker.remove.other", "", COMMAND);
    public final Permission CMD_KEY_OTHER = register("locker.key.other", "", COMMAND);
    public final Permission CMD_MODIFY_OTHER = register("locker.modify.other", "", COMMAND);
    public final Permission CMD_GIVE_OTHER = register("locker.give.other", "", COMMAND);

    public final Permission CMD_INFO_OTHER = register("locker.info.other", "", COMMAND);
    public final Permission CMD_INFO_SHOW_OWNER =  register("locker.info.show-owner", "", COMMAND);

    // TODO Locker grouping Perms on commands
    /*
    public final Permission PROTECT = registerS("protect", "", null,
                                                          "command.locker.info.use",
                                                          "command.locker.persist.use",
                                                          "command.locker.remove.use",
                                                          "command.locker.unlock.use",
                                                          "command.locker.modify.use",
                                                          "command.locker.key.use",
                                                          "command.locker.flag.use",
                                                          "command.locker.give.use",
                                                          "command.locker.create.private.use",
                                                          "command.locker.create.public.use",
                                                          "command.locker.create.donation.use",
                                                          "command.locker.create.free.use",
                                                          "command.locker.create.password.use",
                                                          "command.locker.create.guarded.use",
                                                          "command.locker.info.show-owner");


    public final Permission MODERATOR = register("moderator", "", null, PROTECT, SHOW_OWNER, CMD_INFO_OTHER, ACCESS_OTHER, CMD_REMOVE_OTHER);
    */
}
