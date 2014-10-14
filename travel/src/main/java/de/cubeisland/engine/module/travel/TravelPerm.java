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

import de.cubeisland.engine.core.command.property.PermissionProvider;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.permission.PermissionContainer;
import de.cubeisland.engine.module.travel.home.HomeCommand;
import de.cubeisland.engine.module.travel.warp.WarpCommand;

import static de.cubeisland.engine.core.permission.PermDefault.TRUE;

public class TravelPerm extends PermissionContainer<Travel>
{
    public TravelPerm(Travel module, HomeCommand homeCmd, WarpCommand warpCmd)
    {
        super(module);
        HOME_TP_OTHER = homeCmd.getCommand("tp").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_SET_MORE = homeCmd.getCommand("set").getDescriptor().valueFor(PermissionProvider.class).child("more");
        HOME_MOVE_OTHER = homeCmd.getCommand("move").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_REMOVE_OTHER = homeCmd.getCommand("remove").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_RENAME_OTHER = homeCmd.getCommand("rename").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_LIST_OTHER = homeCmd.getCommand("list").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_PRIVATE_OTHER = homeCmd.getCommand("private").getDescriptor().valueFor(PermissionProvider.class).child("other");
        HOME_PUBLIC_OTHER = homeCmd.getCommand("public").getDescriptor().valueFor(PermissionProvider.class).child("other");

        WARP_TP_OTHER = warpCmd.getCommand("tp").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_MOVE_OTHER = warpCmd.getCommand("move").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_REMOVE_OTHER = warpCmd.getCommand("remove").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_RENAME_OTHER = warpCmd.getCommand("rename").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_LIST_OTHER = warpCmd.getCommand("list").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_PRIVATE_OTHER = warpCmd.getCommand("private").getDescriptor().valueFor(PermissionProvider.class).child("other");
        WARP_PUBLIC_OTHER = warpCmd.getCommand("public").getDescriptor().valueFor(PermissionProvider.class).child("other");

        HOME_USER.attach(homeCmd.getCommand("tp").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("set").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("move").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("remove").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("rename").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("list").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("private").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("greeting").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("ilist").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("invite").getDescriptor().valueFor(PermissionProvider.class),
                         homeCmd.getCommand("uninvite").getDescriptor().valueFor(PermissionProvider.class));

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
