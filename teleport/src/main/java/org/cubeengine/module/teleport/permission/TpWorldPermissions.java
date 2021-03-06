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
package org.cubeengine.module.teleport.permission;

import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.teleport.Teleport;

/**
 * Dynamically registered Permissions for each world.
 */
@SuppressWarnings("all")
@Singleton
public class TpWorldPermissions extends PermissionContainer
{
    private final Map<String, Permission> permissions = new HashMap<>();
    private TeleportPerm perm;
    private PermissionManager pm;

    @Inject
    public TpWorldPermissions(TeleportPerm perm, PermissionManager pm)
    {
        super(pm, Teleport.class);
        this.perm = perm;
        this.pm = pm;
    }

    private Permission initWorldPermission(String world)
    {
        Permission worldPerm = register("command.tpworld." + world, "", null);
        permissions.put(world, worldPerm);
        return worldPerm;
    }

    public Permission getPermission(String world)
    {
        Permission perm = permissions.get(world);
        if (perm == null)
        {
            perm = initWorldPermission(world);
        }
        return perm;
    }
}
