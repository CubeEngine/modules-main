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
package de.cubeisland.engine.module.travel.warp;

import java.util.Locale;
import de.cubeisland.engine.service.permission.PermDefault;
import de.cubeisland.engine.service.permission.Permission;
import de.cubeisland.engine.service.permission.PermissionManager;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.WorldManager;
import de.cubeisland.engine.module.travel.TeleportPoint;
import de.cubeisland.engine.module.travel.Travel;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel;
import de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility;

import static de.cubeisland.engine.module.travel.storage.TableTeleportPoint.TABLE_TP_POINT;
import static de.cubeisland.engine.module.travel.storage.TeleportPointModel.Visibility.PUBLIC;

public class Warp extends TeleportPoint
{
    private PermissionManager pm;

    public Warp(TeleportPointModel teleportPoint, Travel module, PermissionManager pm, WorldManager wm, UserManager um)
    {
        super(teleportPoint, module, wm, um);
        this.pm = pm;
        if (teleportPoint.getValue(TABLE_TP_POINT.VISIBILITY) == PUBLIC.value)
        {
            this.permission = generatePublicPerm();
            return;
        }
        this.permission = null;
    }

    public void setVisibility(Visibility visibility)
    {
        super.setVisibility(visibility);
        model.updateAsync();
        if (visibility == PUBLIC)
        {
            this.permission = generatePublicPerm();
            this.iManager.removeInvites(this);
            return;
        }
        pm.removePermission(this.module, permission);
        this.permission = null;
    }

    @Override
    protected Permission generatePublicPerm()
    {
        Permission perm =  module.getProvided(Permission.class).childWildcard("warps").childWildcard("access").child(this.getName().toLowerCase(Locale.ENGLISH), PermDefault.TRUE);
        pm.registerPermission(module, perm);
        return perm;
    }
}
