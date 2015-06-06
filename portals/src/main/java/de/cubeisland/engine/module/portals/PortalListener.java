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
package de.cubeisland.engine.module.portals;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.module.core.util.LocationUtil;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.EntityTeleportEvent;
import org.spongepowered.api.event.entity.player.PlayerMoveEvent;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

public class PortalListener
{
    private final Portals module;
    private final UserManager um;

    public PortalListener(Portals module, UserManager um)
    {
        this.module = module;
        this.um = um;
    }

    @Subscribe
    public void onTeleport(EntityTeleportEvent event)
    {
        if (!(event.getEntity() instanceof Player))
        {
            return;
        }
        List<Portal> portals = this.module.getPortalsInChunk(event.getNewLocation());
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            if (portal.has(event.getNewLocation()))
            {
                User user = um.getExactUser(event.getEntity().getUniqueId());
                PortalsAttachment attachment = user.attachOrGet(PortalsAttachment.class, module);
                attachment.setInPortal(true);
                if (attachment.isDebug())
                {
                    user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Teleported into portal: {name}", portal.getName());
                }
                return;
            }
            // else ignore
        }
    }

    @Subscribe
    public void onEntityTeleport(EntityTeleportEvent event)
    {
        List<Portal> portals = module.getPortalsInChunk(event.getOldLocation());
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            List<Entity> entities = module.getEntitiesInPortal(portal);
            if (portal.has(event.getNewLocation()))
            {
                entities.add(event.getEntity());
                return;
            }
            else
            {
                entities.remove(event.getEntity());
            }
        }
    }

    @Subscribe
    public void onMove(PlayerMoveEvent event)
    {
        if (event.getOldLocation().getExtent() != event.getNewLocation().getExtent()
            || event.getOldLocation().getBlockX() == event.getNewLocation().getBlockX()
            && event.getOldLocation().getBlockY() == event.getNewLocation().getBlockY()
            && event.getOldLocation().getBlockZ() == event.getNewLocation().getBlockZ())
        {
            return;
        }
        List<Portal> portals = module.getPortalsInChunk(event.getNewLocation());
        User user = um.getExactUser(event.getUser().getUniqueId());
        PortalsAttachment attachment = user.attachOrGet(PortalsAttachment.class, module);
        if (portals != null)
        {
            for (Portal portal : portals)
            {
                if (portal.has(event.getNewLocation()))
                {
                    if (attachment.isDebug())
                    {
                        if (attachment.isInPortal())
                        {
                            user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Move in portal: {name}", portal.getName());
                        }
                        else
                        {
                            user.sendTranslated(POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Entered portal: {name}", portal.getName());
                            portal.showInfo(user);
                            attachment.setInPortal(true);
                        }
                    }
                    else if (!attachment.isInPortal())
                    {
                        portal.teleport(event.getUser());
                    }
                    return;
                }
                // else ignore
            }
        }
        attachment.setInPortal(false);
    }
}
