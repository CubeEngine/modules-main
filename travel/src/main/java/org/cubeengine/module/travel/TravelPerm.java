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
package org.cubeengine.module.travel;

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

public class TravelPerm extends PermissionContainer<Travel>
{
    public TravelPerm(Travel module)
    {
        super(module);
    }

    private final PermissionDescription COMMAND = register("command", "Base Commands Permission", null);

    public final PermissionDescription HOME_USER = register("home-user", "Home Permission Group for normal users", null);
    /* TODO
    HOME_USER.attach(homeCmd.getPermission("tp"), homeCmd.getPermission("set"), homeCmd.getPermission("move"),
                         homeCmd.getPermission("remove"), homeCmd.getPermission("rename"), homeCmd.getPermission(
            "list"), homeCmd.getPermission("private"), homeCmd.getPermission("greeting"), homeCmd.getPermission(
            "ilist"), homeCmd.getPermission("invite"), homeCmd.getPermission("uninvite"));
     */

    public final PermissionDescription HOME_TP_OTHER = register("home.tp.other", "", COMMAND);
    public final PermissionDescription HOME_SET_MORE = register("home.set.more", "", COMMAND);
    public final PermissionDescription HOME_MOVE_OTHER = register("home.move.other", "", COMMAND);
    public final PermissionDescription HOME_REMOVE_OTHER = register("home.remove.other", "", COMMAND);
    public final PermissionDescription HOME_RENAME_OTHER = register("home.rename.other", "", COMMAND);
    public final PermissionDescription HOME_LIST_OTHER = register("home.list.other", "", COMMAND);

    public final PermissionDescription WARP_TP_OTHER = register("warp.tp.other", "", COMMAND);
    public final PermissionDescription WARP_MOVE_OTHER = register("warp.move.other", "", COMMAND);
    public final PermissionDescription WARP_REMOVE_OTHER = register("warp.remove.other", "", COMMAND);
    public final PermissionDescription WARP_RENAME_OTHER = register("warp.rename.other", "", COMMAND);
    public final PermissionDescription WARP_LIST_OTHER = register("warp.list.other", "", COMMAND);
}
