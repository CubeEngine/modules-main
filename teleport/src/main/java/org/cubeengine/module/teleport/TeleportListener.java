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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.module.teleport.permission.TeleportPerm;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.item.ItemTypes.COMPASS;

@Singleton
public class TeleportListener
{
    private final Teleport module;
    private I18n i18n;
    private TeleportPerm perms;

    private Map<UUID, ServerLocation> deathLocations = new HashMap<>();
    private Map<UUID, ServerLocation> lastLocations = new HashMap<>();
    private Map<UUID, ScheduledTask> requestCancelTasks = new HashMap<>();
    private Map<UUID, UUID> tpToRequests = new HashMap<>();
    private Map<UUID, UUID> tpFromRequests = new HashMap<>();

    private Map<UUID, Long> compassJumpCooldown = new HashMap<>();

    @Inject
    public TeleportListener(Teleport module, I18n i18n, TeleportPerm perms)
    {
        this.module = module;
        this.i18n = i18n;
        this.perms = perms;
    }

    @Listener
    public void onTeleport(MoveEntityEvent event, @Getter("getEntity") ServerPlayer player)
    {
        if (event.getContext().get(EventContextKeys.MOVEMENT_TYPE)
                          .map(mt -> mt.equals(MovementTypes.COMMAND.get()) || mt.equals(MovementTypes.PLUGIN.get())).orElse(false))
        {
            final ServerWorld world = event instanceof ChangeEntityWorldEvent ? ((ChangeEntityWorldEvent)event).getOriginalWorld() : player.getWorld();
            lastLocations.put(player.getUniqueId(), world.getLocation(event.getOriginalPosition()));
            this.setDeathLocation(player, null);
        }
    }

    @Listener
    public void onDeath(DestructEntityEvent.Death event)
    {
        if (!(event.getEntity() instanceof ServerPlayer))
        {
            return;
        }
        if (((ServerPlayer)event.getEntity()).hasPermission(perms.COMMAND_BACK_ONDEATH.getId()))
        {
            deathLocations.put(event.getEntity().getUniqueId(), event.getEntity().getServerLocation());
        }
    }

    @Listener
    public void onPrimary(InteractItemEvent.Primary event, @First ServerPlayer player)
    {
        this.onPrimary0(player);
    }

    @Listener
    public void onPrimary(InteractBlockEvent.Primary.Start event, @First ServerPlayer player)
    {
        if (this.onPrimary0(player)) {
            event.setCancelled(true);
        }
    }

    public boolean onPrimary0(@First ServerPlayer player)
    {
        if (!player.getItemInHand(HandTypes.MAIN_HAND).getType().isAnyOf(COMPASS) || !player.hasPermission(perms.COMPASS_JUMPTO_LEFT.getId()))
        {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (this.compassJumpCooldown.getOrDefault(player.getUniqueId(), now) > now)
        {
            return true;
        }
        this.compassJumpCooldown.put(player.getUniqueId(), now + 150); // 150ms cooldown
        ServerLocation loc = LocationUtil.getBlockInSight(player);
        if (loc == null)
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "No block in sight");
            return true;
        }
        player.setLocation(LocationUtil.getLocationUp(loc).add(0.5, 0, 0.5));
        player.offer(Keys.VELOCITY, Vector3d.ZERO);
        player.offer(Keys.FALL_DISTANCE, 0d);
        i18n.send(ACTION_BAR, player, NEUTRAL, "Poof!");
        return true;
    }

    @Listener
    public void onSecondary(InteractItemEvent.Secondary event, @First ServerPlayer player)
    {
        if (!player.getItemInHand(HandTypes.MAIN_HAND).getType().isAnyOf(COMPASS)
                || !player.hasPermission(perms.COMPASS_JUMPTO_RIGHT.getId()))
        {
            return;
        }

        Optional<ServerLocation> end = LocationUtil.getBlockBehindWall(player, module.getConfig().navigation.thru.maxRange, module.getConfig().navigation.thru.maxWallThickness);
        if (!end.isPresent())
        {
            i18n.send(player, NEGATIVE, "Nothing to pass through!");
            return;
        }

        player.setLocation(end.get().add(0.5, 0, 0.5));
        player.offer(Keys.VELOCITY, Vector3d.ZERO);
        player.offer(Keys.FALL_DISTANCE, 0d);
        i18n.send(ACTION_BAR, player, NEUTRAL, "You passed through a wall");
        event.setCancelled(true);
    }

    public ServerLocation getDeathLocation(Player player)
    {
        return deathLocations.get(player.getUniqueId());
    }

    public void setDeathLocation(ServerPlayer player, ServerLocation loc)
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

    public ServerLocation getLastLocation(Player player)
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

    public ScheduledTask getRequestTask(Player player)
    {
        return requestCancelTasks.get(player.getUniqueId());
    }

    public void setRequestTask(Player player, ScheduledTask task)
    {
        requestCancelTasks.put(player.getUniqueId(), task);
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
