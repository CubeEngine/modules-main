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
package org.cubeengine.module.portals;

import java.util.List;

import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class PortalListener
{
    private final Portals module;
    private I18n i18n;


    public PortalListener(Portals module,I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Listener
    public void onTeleport(MoveEntityEvent.Teleport event)
    {
        if ((this.module.getConfig().disableVanillaPortals || this.module.getConfig().disabledVanillaPortalsInWorlds.getOrDefault(new ConfigWorld(event.getFromTransform().getExtent()), false))
                && event.getCause().getContext().get(EventContextKeys.TELEPORT_TYPE).orElse(null) == TeleportTypes.PORTAL)
        {
            event.setCancelled(true);
            return;
        }
        Transform<World> target = event.getToTransform();
        Entity player = event.getTargetEntity();
        if (player instanceof Player)
        {
            onTeleport(target, ((Player) player)); // TODO event listener parameter
        }
    }

    private void onTeleport(Transform<World> target, Player player)
    {
        List<Portal> portals = this.module.getPortalsInChunk(target.getLocation());
        if (portals == null)
        {
            return;
        }
        for (Portal portal : portals)
        {
            if (portal.has(target.getLocation()))
            {
                PortalsAttachment attachment = module.getPortalsAttachment(player.getUniqueId());
                attachment.setInPortal(true);
                if (attachment.isDebug())
                {
                    i18n.send(player, POSITIVE, "{text:[Portals] Debug\\::color=YELLOW} Teleported into portal: {name}", portal.getName());
                }
                return;
            }
            // else ignore
        }
    }

    @Listener
    public void onEntityTeleport(MoveEntityEvent.Teleport event)
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
    public void onMove(MoveEntityEvent event)
    {
        if (!(event.getTargetEntity() instanceof Player))
        {
            return;
        }
        Player player = ((Player) event.getTargetEntity());


        if (event.getFromTransform().getExtent() != event.getToTransform().getExtent()
            || (event.getFromTransform().getLocation().getBlockX() == event.getToTransform().getLocation().getBlockX()
            && event.getFromTransform().getLocation().getBlockY() == event.getToTransform().getLocation().getBlockY()
            && event.getFromTransform().getLocation().getBlockZ() == event.getToTransform().getLocation().getBlockZ()))
        {
            return;
        }
        List<Portal> portals = module.getPortalsInChunk(event.getToTransform().getLocation());
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
                            i18n.send(player, POSITIVE, "{text:[Portals] Debug} Move in portal: {name}", portal.getName());
                        }
                        else
                        {
                            i18n.send(player, POSITIVE, "{text:[Portals] Debug} Entered portal: {name}", portal.getName());
                            portal.showInfo(player);
                            attachment.setInPortal(true);
                        }
                    }
                    else if (!attachment.isInPortal())
                    {
                        portal.teleport(player);
                        onTeleport(player.getTransform(), player); // TODO remove when DisplaceEntityEvent.Teleport is implemented
                    }
                    return;
                }
                // else ignore
            }
        }
        attachment.setInPortal(false);
    }
}
