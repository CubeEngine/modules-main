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
package org.cubeengine.module.travel;

import javax.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.PermissionDescription;

public class TravelPerm extends PermissionContainer
{
    @Inject
    public TravelPerm(PermissionManager pm)
    {
        super(pm, Travel.class);
    }

    public final Permission HOME_TP_OTHER = register("command.home.tp.other", "", null);
    public final Permission HOME_SET_MORE = register("command.home.set.more", "", null);
    public final Permission HOME_MOVE_OTHER = register("command.home.move.other", "", null);
    public final Permission HOME_REMOVE_OTHER = register("command.home.remove.other", "", null);
    public final Permission HOME_RENAME_OTHER = register("command.home.rename.other", "", null);
    public final Permission HOME_LIST_OTHER = register("command.home.list.other", "", null);

    public final Permission WARP_TP_OTHER = register("command.warp.tp.other", "", null);
    public final Permission WARP_MOVE_OTHER = register("command.warp.move.other", "", null);
    public final Permission WARP_REMOVE_OTHER = register("command.warp.remove.other", "", null);
    public final Permission WARP_RENAME_OTHER = register("command.warp.rename.other", "", null);
    public final Permission WARP_LIST_OTHER = register("command.warp.list.other", "", null);
}
