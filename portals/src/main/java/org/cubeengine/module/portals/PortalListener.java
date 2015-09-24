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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent.Teleport.TargetPlayer;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class PortalListener
{
    private final Portals module;
    private final UserManager um;
    private I18n i18n;


    public PortalListener(Portals module, UserManager um, I18n i18n)
    {
        this.module = module;
        this.um = um;
        this.i18n = i18n;
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
                PortalsAttachment attachment = module.getPortalsAttachment(event.getTargetEntity().getUniqueId());
                attachment.setInPortal(true);
                if (attachment.isDebug())
                {
                    i18n.sendTranslated(event.getTargetEntity(), POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Teleported into portal: {name}", portal.getName());
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
        Player player = event.getTargetEntity();
        PortalsAttachment attachment = module.getPortalsAttachment(player.getUniqueId());
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
                            i18n.sendTranslated(player, POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Move in portal: {name}", portal.getName());
                        }
                        else
                        {
                            i18n.sendTranslated(player, POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Entered portal: {name}", portal.getName());
                            portal.showInfo(player);
                            attachment.setInPortal(true);
                        }
                    }
                    else if (!attachment.isInPortal())
                    {
                        portal.teleport(player);
                    }
                    return;
                }
                // else ignore
            }
        }
        attachment.setInPortal(false);
    }
}
