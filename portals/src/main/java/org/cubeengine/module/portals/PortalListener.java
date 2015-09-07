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
package org.cubeengine.module.portals;

import java.util.List;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class PortalListener
{
    private final Portals module;
    private final UserManager um;

    public PortalListener(Portals module, UserManager um)
    {
        this.module = module;
        this.um = um;
    }

    @Listener
    public void onTeleport(DisplaceEntityEvent.Teleport.TargetPlayer event)
    {
        List<Portal> portals = this.module.getPortalsInChunk(event.getToTransform().getLocation());
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            if (portal.has(event.getToTransform().getLocation()))
            {
                User user = um.getExactUser(event.getTargetEntity().getUniqueId());
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

    @Listener
    public void onEntityTeleport(DisplaceEntityEvent.Teleport event)
    {
        if (event.getTargetEntity() instanceof Player)
        {
            return;
        }
        List<Portal> portals = module.getPortalsInChunk(event.getFromTransform().getLocation());
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            List<Entity> entities = module.getEntitiesInPortal(portal);
            if (portal.has(event.getToTransform().getLocation()))
            {
                entities.add(event.getTargetEntity());
                return;
            }
            else
            {
                entities.remove(event.getTargetEntity());
            }
        }
    }

    @Listener
    public void onMove(DisplaceEntityEvent.Move.TargetPlayer event)
    {
        if (event.getFromTransform().getExtent() != event.getToTransform().getExtent()
            || event.getFromTransform().getLocation().getBlockX() == event.getToTransform().getLocation().getBlockX()
            && event.getFromTransform().getLocation().getBlockY() == event.getToTransform().getLocation().getBlockY()
            && event.getFromTransform().getLocation().getBlockZ() == event.getToTransform().getLocation().getBlockZ())
        {
            return;
        }
        List<Portal> portals = module.getPortalsInChunk(event.getToTransform().getLocation());
        User user = um.getExactUser(event.getTargetEntity().getUniqueId());
        PortalsAttachment attachment = user.attachOrGet(PortalsAttachment.class, module);
        if (portals != null)
        {
            for (Portal portal : portals)
            {
                if (portal.has(event.getToTransform().getLocation()))
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
                        portal.teleport(event.getTargetEntity());
                    }
                    return;
                }
                // else ignore
            }
        }
        attachment.setInPortal(false);
    }
}
