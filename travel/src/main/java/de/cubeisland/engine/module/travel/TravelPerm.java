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
package de.cubeisland.engine.module.travel;

import de.cubeisland.engine.service.permission.Permission;
import de.cubeisland.engine.service.permission.PermissionContainer;
import de.cubeisland.engine.module.travel.home.HomeCommand;
import de.cubeisland.engine.module.travel.warp.WarpCommand;

import static de.cubeisland.engine.service.permission.PermDefault.TRUE;

public class TravelPerm extends PermissionContainer<Travel>
{
    public TravelPerm(Travel module, HomeCommand homeCmd, WarpCommand warpCmd)
    {
        super(module);
        HOME_TP_OTHER = homeCmd.getPermission("tp").child("other");
        HOME_SET_MORE = homeCmd.getPermission("set").child("more");
        HOME_MOVE_OTHER = homeCmd.getPermission("move").child("other");
        HOME_REMOVE_OTHER = homeCmd.getPermission("remove").child("other");
        HOME_RENAME_OTHER = homeCmd.getPermission("rename").child("other");
        HOME_LIST_OTHER = homeCmd.getPermission("list").child("other");
        HOME_PRIVATE_OTHER = homeCmd.getPermission("private").child("other");
        HOME_PUBLIC_OTHER = homeCmd.getPermission("public").child("other");

        WARP_TP_OTHER = warpCmd.getPermission("tp").child("other");
        WARP_MOVE_OTHER = warpCmd.getPermission("move").child("other");
        WARP_REMOVE_OTHER = warpCmd.getPermission("remove").child("other");
        WARP_RENAME_OTHER = warpCmd.getPermission("rename").child("other");
        WARP_LIST_OTHER = warpCmd.getPermission("list").child("other");
        WARP_PRIVATE_OTHER = warpCmd.getPermission("private").child("other");
        WARP_PUBLIC_OTHER = warpCmd.getPermission("public").child("other");

        HOME_USER.attach(homeCmd.getPermission("tp"),
                         homeCmd.getPermission("set"),
                         homeCmd.getPermission("move"),
                         homeCmd.getPermission("remove"),
                         homeCmd.getPermission("rename"),
                         homeCmd.getPermission("list"),
                         homeCmd.getPermission("private"),
                         homeCmd.getPermission("greeting"),
                         homeCmd.getPermission("ilist"),
                         homeCmd.getPermission("invite"),
                         homeCmd.getPermission("uninvite"));

        this.registerAllPermissions();
    }

    public final Permission HOME_USER = getBasePerm().child("home-user", TRUE);

    public final Permission HOME_TP_OTHER;
    public final Permission HOME_SET_MORE;
    public final Permission HOME_MOVE_OTHER;
    public final Permission HOME_REMOVE_OTHER;
    public final Permission HOME_RENAME_OTHER;
    public final Permission HOME_LIST_OTHER;
    public final Permission HOME_PRIVATE_OTHER;
    public final Permission HOME_PUBLIC_OTHER;

    public final Permission WARP_TP_OTHER;
    public final Permission WARP_MOVE_OTHER;
    public final Permission WARP_REMOVE_OTHER;
    public final Permission WARP_RENAME_OTHER;
    public final Permission WARP_LIST_OTHER;
    public final Permission WARP_PRIVATE_OTHER;
    public final Permission WARP_PUBLIC_OTHER;
}
