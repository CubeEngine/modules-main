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
package org.cubeengine.module.protector.listener;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.util.Tristate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveListener
{
    private RegionManager manager;

    public Map<MoveType, Permission> permissions = new HashMap<>();

    public MoveListener(RegionManager manager, Permission base, PermissionManager pm)
    {
        this.manager = manager;
        // TODO description
        permissions.put(MoveType.MOVE, pm.register(MoveListener.class, "bypass.move.move", "", base));
        permissions.put(MoveType.EXIT, pm.register(MoveListener.class, "bypass.move.exit", "", base));
        permissions.put(MoveType.ENTER, pm.register(MoveListener.class, "bypass.move.enter", "", base));
        permissions.put(MoveType.TELEPORT, pm.register(MoveListener.class, "bypass.move.teleport", "", base));
    }

    @Listener
    public void onMove(MoveEntityEvent event, @Root Player player)
    {
        List<Region> from = manager.getRegionsAt(event.getFromTransform().getLocation());
        List<Region> to = manager.getRegionsAt(event.getFromTransform().getLocation());
        if (from.isEmpty() && to.isEmpty())
        {
            return;
        }

        if (event instanceof MoveEntityEvent.Teleport)
        {
            if (checkMove(event, player, from, to, MoveType.TELEPORT, false))
            {
                return; // Teleport out denied
            }

            if (checkMove(event, player, to, from, MoveType.TELEPORT, false))
            {
                return; // Teleport in denied
            }
        }
        else
        {
            if (checkMove(event, player, from, to, MoveType.MOVE, false))
            {
                return; // Move in from denied
            }

            if (checkMove(event, player, from, to, MoveType.EXIT, true))
            {
                return; // Move out of from denied
            }

            if (checkMove(event, player, to, from, MoveType.ENTER, true))
            {
                return; // Move into to denied
            }
        }
    }

    private boolean checkMove(MoveEntityEvent event, @Root Player player, List<Region> source, List<Region> dest, MoveType type, boolean contain)
    {
        Permission perm = this.permissions.get(type);
        if (!player.hasPermission(perm.getId()))
        {
            Tristate allow = Tristate.UNDEFINED;
            for (Region region : source)
            {
                allow = allow.and(region.getSettings().move.getOrDefault(type, Tristate.UNDEFINED));
                if (allow != Tristate.UNDEFINED)
                {
                    if (allow == Tristate.FALSE)
                    {
                        if (!contain || !dest.contains(region))
                        {
                            event.setCancelled(true);
                            return true;
                        }
                    }
                    if (!contain)
                    {
                        break;
                    }
                }
            }
        }
        return false;
    }

    public enum MoveType
    {
        MOVE, ENTER, EXIT, TELEPORT
    }

}
