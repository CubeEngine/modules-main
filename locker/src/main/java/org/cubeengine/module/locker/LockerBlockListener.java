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
package org.cubeengine.module.locker;

import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.cubeengine.module.core.util.BlockUtil.CARDINAL_DIRECTIONS;
import static org.cubeengine.module.core.util.BlockUtil.getChunk;
import static org.cubeengine.module.locker.storage.ProtectionFlag.BLOCK_REDSTONE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.data.property.block.MatterProperty.Matter.LIQUID;
import static org.spongepowered.api.data.property.block.MatterProperty.Matter.SOLID;
import static org.spongepowered.api.data.type.PortionTypes.TOP;
import static org.spongepowered.api.util.Direction.*;
import static org.spongepowered.api.util.Direction.SOUTH;
import static org.spongepowered.api.util.Direction.WEST;

public class LockerBlockListener
{
    private final Locker module;
    private final LockManager manager;
    private final I18n i18n;
    private final Game game;

    public LockerBlockListener(Locker locker, LockManager manager, I18n i18n, Game game)
    {
        this.module = locker;
        this.manager = manager;
        this.i18n = i18n;
        this.game = game;
    }

    @Listener(order = Order.EARLY)
    public void onPlace(ChangeBlockEvent.Place event, @First Player player)
    {
        if (player.get(Keys.IS_SNEAKING).orElse(false))
        {
            return;
        }
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            BlockSnapshot placed = trans.getFinal();
            BlockType type = placed.getState().getType();

            if (type == CHEST || type == TRAPPED_CHEST)
            {
                if (onPlaceChest(event, placed.getLocation().get(), player))
                {
                    return;
                }
            }
            else if (isDoor(type))
            {
                if (onPlaceDoor(event, placed, player))
                {
                    return;
                }
            }
            if (module.getConfig().disableAutoProtect.stream().map(ConfigWorld::getWorld).collect(toSet()).contains(
                    placed.getLocation().get().getExtent()))
            {
                return;
            }
            for (BlockLockConfig blockprotection : this.module.getConfig().blockprotections)
            {
                if (blockprotection.isType(type))
                {
                    if (!blockprotection.isAutoProtect()) return;
                    this.manager.createLock(placed.getLocation().get(), player, blockprotection.getAutoProtect(), null, false);
                    return;
                }
            }
        }
    }

    private boolean isDoor(BlockType type)
    {
        return type == WOODEN_DOOR || type == IRON_DOOR || type == SPRUCE_DOOR || type == BIRCH_DOOR
                || type == JUNGLE_DOOR || type == ACACIA_DOOR || type == DARK_OAK_DOOR;
    }

    private boolean onPlaceDoor(ChangeBlockEvent event, BlockSnapshot placed, Player user)
    {
        Location<World> location = placed.getLocation().get();
        BlockState doorState = placed.getState();
        Hinge hinge = doorState.get(Keys.HINGE_POSITION).get();
        Direction direction = doorState.get(Keys.DIRECTION).get();

        Location<World> relative = location.getRelative(BlockUtil.getOtherDoorDirection(direction, hinge));
        if (!isDoor(relative.getBlockType()))
        {
            return false; // Not a door
        }

        if (!relative.get(Keys.DIRECTION).get().equals(direction) || relative.get(Keys.HINGE_POSITION).get().equals(hinge))
        {
            return false; // Not a doubledoor
        }
        Lock lock = this.manager.getLockAtLocation(relative, null);
        if (lock != null)
        {
            if (!lock.validateTypeAt(relative))
            {
                i18n.sendTranslated(user, NEUTRAL, "Nearby BlockProtection is not valid!");
                lock.delete(user);
                return true;
            }

            if (placed.get(Keys.PORTION_TYPE).get().equals(TOP))
            {
                relative = location.getRelative(DOWN);
            }
            else
            {
                relative = location.getRelative(UP);
            }

            if (lock.isOwner(user) || lock.hasAdmin(user) || user.hasPermission(module.perms().EXPAND_OTHER.getId()))
            {
                this.manager.extendLock(lock, location);
                this.manager.extendLock(lock, relative);
                i18n.sendTranslated(user, POSITIVE, "Protection expanded!");
            }
            else
            {
                event.setCancelled(true);
                i18n.sendTranslated(user, NEGATIVE, "The nearby door is protected by someone else!");
            }
            return true;
        }
        return false;
    }

    private boolean onPlaceChest(ChangeBlockEvent.Place event, Location<World> placed, Player user)
    {
        Location<World> relativeLoc;
        for (Direction direction : CARDINAL_DIRECTIONS)
        {
            if (placed.getBlockType() != placed.getRelative(direction).getBlockType()) // bindable chest
            {
                continue;
            }
            relativeLoc = placed.getRelative(direction);
            Lock lock = this.manager.getLockAtLocation(relativeLoc, user);
            if (lock == null)
            {
                continue;
            }
            if (!lock.validateTypeAt(relativeLoc))
            {
                i18n.sendTranslated(user, NEUTRAL, "Nearby BlockProtection is not valid!");
                lock.delete(user);
            }
            else if (lock.isOwner(user) || lock.hasAdmin(user) || user.hasPermission(module.perms().EXPAND_OTHER.getId()))
            {
                this.manager.extendLock(lock, placed);
                i18n.sendTranslated(user, POSITIVE, "Protection expanded!");
            }
            else
            {
                event.setCancelled(true);
                i18n.sendTranslated(user, NEGATIVE, "The nearby chest is protected by someone else!");
            }
            return true;
        }
        return false;
    }

    /*  TODO use this instead of ugly mess below once available
    @Listener
    public void onBlockPistonExtend(MoveBlockEvent event, @First Player player)
    {
        if (!this.module.getConfig().protectFromPistonMove) return;
        for (Transaction<BlockSnapshot> transaction : event.getTransactions())
        {
            if (manager.getLockAtLocation(transaction.getOriginal().getLocation().get(), null) != null)
            {
                event.setCancelled(true);
                return;
            }
        }
    }
    */

    @Listener
    public void onNotifyPiston(NotifyNeighborBlockEvent event, @First BlockSnapshot block)
    {

        if (this.module.getConfig().protectFromPistonMove)
        {
            event.getNeighbors().entrySet().stream()
                    .filter(e -> PISTON.equals(e.getValue().getType()) || STICKY_PISTON.equals(e.getValue().getType()))
                    .forEach(entry ->
                    {
                        Location<World> piston = block.getLocation().get().getRelative(entry.getKey());
                        Direction direction = piston.get(Keys.DIRECTION).get();
                        boolean extend = !piston.get(Keys.EXTENDED).get();
                        if (!extend)
                        {
                            piston = piston.getRelative(direction); // piston head
                        }
                        if (hasLock(piston.getRelative(direction), direction, new HashSet<>(), extend, 0))
                        {
                            event.setCancelled(true);
                            // TODO filter once implemented event.filterDirections(d -> d == entry.getKey());
                        }
                    });
        }

        if (this.module.getConfig().protectFromRedstone)
        {
            for (Map.Entry<Direction, BlockState> entry : event.getNeighbors().entrySet())
            {
                Lock lock = manager.getLockAtLocation(block.getLocation().get().getRelative(entry.getKey()), null);
                if (lock != null && lock.hasFlag(BLOCK_REDSTONE) && entry.getValue().supports(Keys.POWERED))
                {
                    event.setCancelled(true);
                    // TODO filter once implemented event.filterDirections(d -> d == entry.getKey());
                    return;
                }
            }
        }
    }

    private boolean hasLock(Location<World> block, Direction direction, Set<Location<World>> locs, boolean extend, int iterations)
    {
        iterations++;
        if (!locs.add(block) || iterations > 12) // Add Block to set - if not added or iterations > 12 stop
        {
            return false;
        }
        if (manager.getLockAtLocation(block, null) != null) // Found Lock. Abort
        {
            return true;
        }

        if (block.getBlockType().getProperty(MatterProperty.class).get().getValue() != SOLID)
        {
            return false;
        }
        if (extend && hasLock(block.getRelative(direction), direction, locs, true, iterations))
        {
            return true;
        }
        if (block.getBlockType() == SLIME)
        {
            for (Direction dir : Direction.values())
            {
                if (dir.isCardinal() && hasLock(block.getRelative(dir), direction, locs, extend, iterations))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player player)
    {
        if (!this.module.getConfig().protectFromBlockBreak) return;
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null)
            {
                lock.handleBlockBreak(event, player);
            }

            for (Location<World> block : BlockUtil.getDetachableBlocks(location))
            {
                lock = this.manager.getLockAtLocation(block, player);
                if (lock != null)
                {
                    lock.handleBlockBreak(event, player);
                    return;
                }
            }

            // Search for Detachable Entities
            if (module.getConfig().protectsDetachableEntities())
            {
                Set<Chunk> chunks = new HashSet<>();
                chunks.add(getChunk(location, game).get());
                chunks.add(getChunk(location.getRelative(NORTH), game).get());
                chunks.add(getChunk(location.getRelative(EAST), game).get());
                chunks.add(getChunk(location.getRelative(SOUTH), game).get());
                chunks.add(getChunk(location.getRelative(WEST), game).get());
                Set<Hanging> hangings = new HashSet<>();
                for (Chunk chunk : chunks)
                {
                    hangings.addAll(chunk.getEntities().stream()
                            .filter(entity -> entity instanceof Hanging)
                            .map(entity -> (Hanging)entity).collect(toList()));
                }
                Location entityLoc;
                for (Hanging hanging : hangings)
                {
                    entityLoc = hanging.getLocation();
                    if (entityLoc.getRelative(hanging.getDirectionalData().direction().get()).equals(location))
                    {
                        lock = this.manager.getLockForEntityUID(hanging.getUniqueId());
                        if (lock != null)
                        {
                            lock.handleBlockBreak(event, player);
                        }
                    }
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onBlockExplode(ExplosionEvent.Detonate event)
    {
        if (!this.module.getConfig().protectBlockFromExplosion) return;
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            if (manager.getLockAtLocation(location, null) != null)
            {
                trans.setValid(false);
            }
        }

        event.filterEntities(input -> manager.getLockForEntityUID(input.getUniqueId()) != null);
    }

    @Listener
    public void onBlockBurn(ChangeBlockEvent.Break event)
    {
        Optional<BlockSnapshot> blockCause = event.getCause().first(BlockSnapshot.class);
        if (!blockCause.isPresent() || blockCause.get().getState().getType().equals(FIRE))
        {
            return;
        }
        if (!this.module.getConfig().protectBlockFromFire) return;
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null)
            {
                trans.setValid(false);
                continue;
            }
            for (Location<World> block : BlockUtil.getDetachableBlocks(location))
            {
                lock = this.manager.getLockAtLocation(block, null);
                if (lock != null)
                {
                    trans.setValid(false);
                    break;
                }
            }
        }
    }

    /* // TODO InventoryMovements
    @Listener
    public void onHopperItemMove(InteractInventoryEvent event)
    {
        if (this.module.getConfig().noProtectFromHopper) return;
        CarriedInventory inventory = event.getSource();
        Lock lock = this.manager.getLockOfInventory(inventory);
        if (lock != null)
        {
            Carrier dest = event.getDestination().getHolder();
            if (((dest instanceof Hopper || dest instanceof Dropper) && !lock.hasFlag(HOPPER_OUT))
             || (dest instanceof MinecartHopper && !lock.hasFlag(HOPPER_MINECART_OUT)))
            {
                event.setCancelled(true);
            }
        }
        if (event.isCancelled()) return;
        inventory = event.getDestination();
        lock = this.manager.getLockOfInventory(inventory);
        if (lock != null)
        {
            Carrier source = event.getSource().getHolder();
            if (((source instanceof Hopper || source instanceof Dropper) && !lock.hasFlag(HOPPER_IN))
                || (source instanceof MinecartHopper && !lock.hasFlag(HOPPER_MINECART_IN)))
            {
                event.setCancelled(true);
            }
        }
    }
    */

    @Listener
    public void onWaterLavaFlow(ChangeBlockEvent.Place event)
    {
        if (!this.module.getConfig().protectBlocksFromWaterLava)
        {
            return;
        }
        // do not allow change
        event.getTransactions().stream()
                .filter(trans -> trans.getFinal().getState().getType().getProperty(MatterProperty.class).get().getValue() == LIQUID)
                .forEach(trans -> {
                    Location<World> location = trans.getOriginal().getLocation().get();
                    if (manager.getLockAtLocation(location, null) != null)
                    {
                        trans.setCustom(trans.getOriginal()); // do not allow change
                    }
                });
    }
}