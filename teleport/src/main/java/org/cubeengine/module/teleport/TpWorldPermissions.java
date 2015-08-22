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
package org.cubeengine.module.teleport;

import java.util.HashMap;
import java.util.Map;
import org.cubeengine.service.permission.PermissionContainer;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.world.WorldManager;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.World;

/**
 * Dynamically registered Permissions for each world.
 */
@SuppressWarnings("all")
public class TpWorldPermissions extends PermissionContainer<Teleport>
{
    private final Map<String, PermissionDescription> permissions = new HashMap<>();
    private TeleportPerm perm;
    private PermissionManager pm;

    public TpWorldPermissions(Teleport module, TeleportPerm perm, WorldManager wm, PermissionManager pm)
    {
        super(module);
        this.perm = perm;
        this.pm = pm;
        for (final World world : wm.getWorlds())
        {
            initWorldPermission(world.getName());
        }
    }

    private PermissionDescription initWorldPermission(String world)
    {
        PermissionDescription worldPerm = register("tpworld." + world, "", perm.COMMAND);
        permissions.put(world, worldPerm);
        return worldPerm;
    }

    public PermissionDescription getPermission(String world)
    {
        PermissionDescription perm = permissions.get(world);
        if (perm == null)
        {
            perm = initWorldPermission(world);
        }
        return perm;
    }
}
