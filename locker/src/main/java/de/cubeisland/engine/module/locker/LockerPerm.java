/**
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
package de.cubeisland.engine.module.locker;

import de.cubeisland.engine.module.locker.commands.LockerCommands;
import de.cubeisland.engine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class LockerPerm extends PermissionContainer<Locker>
{
    public LockerPerm(Locker module, LockerCommands mainCmd)
    {
        super(module);
    }

    // TODO invert deny permissions
    public final PermissionDescription DENY_CONTAINER = register("deny.container", "", null);
    public final PermissionDescription DENY_DOOR = register("deny.door", "", null);
    public final PermissionDescription DENY_ENTITY = register("deny.entity", "", null);
    public final PermissionDescription DENY_HANGING = register("deny.hanging", "", null);

    public final PermissionDescription SHOW_OWNER = register("show-owner", "", null);
    public final PermissionDescription BREAK_OTHER = register("break-other", "", null);
    public final PermissionDescription ACCESS_OTHER = register("access-other", "", null);
    public final PermissionDescription EXPAND_OTHER = register("break-other", "", null);

    public final PermissionDescription PREVENT_NOTIFY = register("prevent-notify", "", null);

    private final PermissionDescription COMMAND = register("command", "", null);

    public final PermissionDescription CMD_REMOVE_OTHER = register("locker.remove.other", "", COMMAND);
    public final PermissionDescription CMD_KEY_OTHER = register("locker.key.other", "", COMMAND);
    public final PermissionDescription CMD_MODIFY_OTHER = register("locker.modify.other", "", COMMAND);
    public final PermissionDescription CMD_GIVE_OTHER = register("locker.give.other", "", COMMAND);

    public final PermissionDescription CMD_INFO_OTHER = register("locker.info.other", "", COMMAND);
    public final PermissionDescription CMD_INFO_SHOW_OWNER =  register("locker.info.show-owner", "", null);

    public final PermissionDescription PROTECT = registerS("protect", "", null,
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

    public final PermissionDescription MODERATOR = register("moderator", "", null, PROTECT, SHOW_OWNER, CMD_INFO_OTHER, ACCESS_OTHER, CMD_REMOVE_OTHER);
}
