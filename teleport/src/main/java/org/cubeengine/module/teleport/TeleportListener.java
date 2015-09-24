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
import java.util.UUID;
import com.google.common.base.Optional;
import org.cubeengine.module.core.util.LocationUtil;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.item.ItemTypes.COMPASS;

public class TeleportListener
{
    private final Teleport module;
    private UserManager um;
    private I18n i18n;

    private Map<UUID, Transform<World>> deathLocations = new HashMap<>();
    private Map<UUID, Transform<World>> lastLocations = new HashMap<>();
    private Map<UUID, UUID> requestCancelTasks = new HashMap<>();
    private Map<UUID, UUID> tpToRequests = new HashMap<>();
    private Map<UUID, UUID> tpFromRequests = new HashMap<>();

    public TeleportListener(Teleport module, UserManager um, I18n i18n)
    {
        this.module = module;
        this.um = um;
        this.i18n = i18n;
    }

    @Listener
    public void onTeleport(DisplaceEntityEvent.Teleport.TargetPlayer event)
    {
        lastLocations.put(event.getTargetEntity().getUniqueId(), event.getFromTransform());
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
    public void onClick(InteractBlockEvent event)
    {
        Optional<Player> source = event.getCause().first(Player.class);
        if (!source.isPresent())
        {
            return;
        }
        Player player = source.get();
        if (source.get().getItemInHand().transform(ItemStack::getItem).orNull() != COMPASS)
        {
            return;
        }
        event.setCancelled(true);
        if (event instanceof InteractBlockEvent.Primary)
        {
            if (player.hasPermission(module.perms().COMPASS_JUMPTO_LEFT.getId()))
            {
                Location<World> loc;
                if (event.getTargetBlock().getState().getType().isSolidCube())
                {
                    loc = event.getTargetBlock().getLocation().get().add(0.5, 1, 0.5);
                }
                else
                {
                    Optional<BlockRayHit<World>> end = BlockRay.from(player).end();
                    if (!end.isPresent())
                    {
                        return;
                    }
                    loc = end.get().getLocation().add(0.5, 1, 0.5);
                }
                player.setLocation(loc);
                i18n.sendTranslated(player, NEUTRAL, "Poof!");
                event.setCancelled(true);
            }
        }
        else if (event instanceof InteractBlockEvent.Secondary)
        {
            if (player.hasPermission(module.perms().COMPASS_JUMPTO_RIGHT.getId()))
            {
                Location<World> loc = LocationUtil.getBlockBehindWall(player, this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
                if (loc == null)
                {
                    i18n.sendTranslated(player, NEGATIVE, "Nothing to pass through!");
                    return;
                }
                loc = loc.add(0, 1, 0);
                player.setLocation(loc);
                i18n.sendTranslated(player, NEUTRAL, "You passed through a wall");
                event.setCancelled(true);
            }
        }
        // TODO left click air is not handled OR is it?
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
