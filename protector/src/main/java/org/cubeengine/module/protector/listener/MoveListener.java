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

import java.util.List;

public class MoveListener
{
    private RegionManager manager;

    private Permission PERM_MOVE;
    private Permission PERM_EXIT;
    private Permission PERM_ENTER;
    private Permission PERM_TELEPORT;

    public MoveListener(RegionManager manager, Permission base, PermissionManager pm)
    {
        this.manager = manager;
        // TODO description
        PERM_MOVE = pm.register(MoveListener.class, "bypass.move.move", "", base);
        PERM_EXIT = pm.register(MoveListener.class, "bypass.exit.move", "", base);
        PERM_ENTER = pm.register(MoveListener.class, "bypass.enter.move", "", base);
        PERM_TELEPORT = pm.register(MoveListener.class, "bypass.teleport.move", "", base);
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
            if (checkMove(event, player, from, to, MoveType.TELEPORT, PERM_TELEPORT, false))
            {
                return; // Teleport out denied
            }

            if (checkMove(event, player, to, from, MoveType.TELEPORT, PERM_TELEPORT, false))
            {
                return; // Teleport in denied
            }
        }
        else
        {
            if (checkMove(event, player, from, to, MoveType.MOVE, PERM_MOVE, false))
            {
                return; // Move in from denied
            }

            if (checkMove(event, player, from, to, MoveType.EXIT, PERM_EXIT, true))
            {
                return; // Move out of from denied
            }

            if (checkMove(event, player, to, from, MoveType.ENTER, PERM_ENTER, true))
            {
                return; // Move into to denied
            }
        }
    }

    private boolean checkMove(MoveEntityEvent event, @Root Player player, List<Region> source, List<Region> dest, MoveType type, Permission perm, boolean contain)
    {
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
