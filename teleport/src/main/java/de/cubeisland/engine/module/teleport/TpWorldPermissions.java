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
package de.cubeisland.engine.module.teleport;

import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionContainer;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.world.World;

/**
 * Dynamically registered Permissions for each world.
 */
@SuppressWarnings("all")
public class TpWorldPermissions extends PermissionContainer<Teleport>
{
    private final Permission COMMAND_TPWORLD;
    private final Map<String, Permission> permissions = new HashMap<>();
    private PermissionManager pm;

    public TpWorldPermissions(Teleport module, TeleportPerm perm, WorldManager wm, PermissionManager pm)
    {
        super(module);
        this.pm = pm;
        COMMAND_TPWORLD = perm.COMMAND.childWildcard("tpworld");
        for (final World world : wm.getWorlds())
        {
            initWorldPermission(world.getName());
        }
    }

    private Permission initWorldPermission(String world)
    {
        Permission perm = COMMAND_TPWORLD.child(world);
        permissions.put(world, perm);
        pm.registerPermission(module, perm);
        return perm;
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
