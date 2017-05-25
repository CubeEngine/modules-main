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
package org.cubeengine.module.teleport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.data.property.block.MatterProperty.Matter.SOLID;
import static org.spongepowered.api.item.ItemTypes.COMPASS;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;

public class TeleportListener
{
    private final Teleport module;
    private I18n i18n;

    private Map<UUID, Transform<World>> deathLocations = new HashMap<>();
    private Map<UUID, Transform<World>> lastLocations = new HashMap<>();
    private Map<UUID, UUID> requestCancelTasks = new HashMap<>();
    private Map<UUID, UUID> tpToRequests = new HashMap<>();
    private Map<UUID, UUID> tpFromRequests = new HashMap<>();

    public TeleportListener(Teleport module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Listener
    public void onTeleport(MoveEntityEvent.Teleport event)
    {
        if (event.getTargetEntity() instanceof Player)
        {
            lastLocations.put(event.getTargetEntity().getUniqueId(), event.getFromTransform());
        }
    }

    @Listener
    public void onDeath(DestructEntityEvent event)
    {
        if (!(event.getTargetEntity() instanceof Player))
        {
            return;
        }
        if (((Player)event.getTargetEntity()).hasPermission(module.perms().COMMAND_BACK_ONDEATH.getId()))
        {
            deathLocations.put(event.getTargetEntity().getUniqueId(), event.getTargetEntity().getTransform());
        }
    }

    @Listener
    // TODO @Include(value = {InteractBlockEvent.Primary.class, InteractEntityEvent.Primary.class})
    public void onPrimary(InteractEvent event, @First Player player)
    {
        if (!(event instanceof InteractBlockEvent.Primary))
        {
            // TODO remove when include works as intended
            return;
        }
        if (player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem).orElse(null) != COMPASS
                || !player.hasPermission(module.perms().COMPASS_JUMPTO_LEFT.getId()))
        {
            return;
        }

        Location<World> loc = LocationUtil.getBlockInSight(player);
        if (loc == null)
        {
            return;
        }
        player.setLocation(LocationUtil.getLocationUp(loc).add(0.5, 0, 0.5));
        i18n.sendTranslated(ACTION_BAR, player, NEUTRAL, "Poof!");
        event.setCancelled(true);
    }

    @Listener
    // TODO @Include(value = {InteractBlockEvent.Secondary.class, InteractEntityEvent.Secondary.class})
    public void onSecondary(InteractEvent event, @First Player player)
    {
        if (!(event instanceof InteractBlockEvent.Secondary))
        {
            // TODO remove when include works as intended
            return;
        }
        if (player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem).orElse(null) != COMPASS
                || !player.hasPermission(module.perms().COMPASS_JUMPTO_RIGHT.getId()))
        {
            return;
        }

        Optional<Location<World>> end = LocationUtil.getBlockBehindWall(player, module.getConfig().navigation.thru.maxRange, module.getConfig().navigation.thru.maxWallThickness);
        if (!end.isPresent())
        {
            i18n.sendTranslated(player, NEGATIVE, "Nothing to pass through!");
            return;
        }

        player.setLocation(end.get().add(0.5, 0, 0.5));
        i18n.sendTranslated(player, NEUTRAL, "You passed through a wall");
        event.setCancelled(true);
    }

    public Transform<World> getDeathLocation(Player player)
    {
        return deathLocations.get(player.getUniqueId());
    }

    public void setDeathLocation(Player player, Transform<World> loc)
    {
        if (loc == null)
        {
            deathLocations.remove(player.getUniqueId());
        }
        else
        {
            deathLocations.put(player.getUniqueId(), loc);
        }
    }

    public Transform<World> getLastLocation(Player player)
    {
        return lastLocations.get(player.getUniqueId());
    }

    public void removeRequestTask(Player player)
    {
        requestCancelTasks.remove(player.getUniqueId());
    }

    public void setToRequest(Player player, Player target)
    {
        tpToRequests.put(player.getUniqueId(), target.getUniqueId());
    }

    public void removeFromRequest(Player player)
    {
        tpFromRequests.remove(player.getUniqueId());
    }

    public void removeToRequest(Player player)
    {
        tpToRequests.remove(player.getUniqueId());
    }

    public UUID getRequestTask(Player player)
    {
        return requestCancelTasks.get(player.getUniqueId());
    }

    public void setRequestTask(Player player, UUID taskID)
    {
        requestCancelTasks.put(player.getUniqueId(), taskID);
    }

    public void setFromRequest(Player player, Player target)
    {
        tpFromRequests.put(player.getUniqueId(), target.getUniqueId());
    }

    public UUID getToRequest(Player player)
    {
        return tpToRequests.get(player.getUniqueId());
    }

    public UUID getFromRequest(Player player)
    {
        return tpFromRequests.get(player.getUniqueId());
    }
}
