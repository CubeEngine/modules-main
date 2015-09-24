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

import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Optional;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTransaction;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.DamageableData;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.BreakBlockEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.MoveBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.PlaceBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.TameEntityEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.cubeengine.module.core.util.BlockUtil.CARDINAL_DIRECTIONS;
import static org.cubeengine.module.core.util.BlockUtil.getChunk;
import static org.cubeengine.module.locker.storage.ProtectionFlag.BLOCK_REDSTONE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.data.key.Keys.POWERED;
import static org.spongepowered.api.data.type.PortionTypes.TOP;
import static org.spongepowered.api.entity.EntityTypes.HORSE;
import static org.spongepowered.api.util.Direction.*;

public class LockerListener
{
    private final LockManager manager;
    private UserManager um;
    private I18n i18n;
    private final Locker module;

    public LockerListener(Locker module, LockManager manager, UserManager um, I18n i18n)
    {
        this.module = module;
        this.manager = manager;
        this.um = um;
        this.i18n = i18n;
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent.Secondary event)
    {
        if (!this.module.getConfig().protectBlockFromRClick) return;

        Optional<Player> player = event.getCause().first(Player.class);
        if (!player.isPresent())
        {
            return;
        }
        Location<World> block = event.getTargetBlock().getLocation().get();
        Lock lock = this.manager.getLockAtLocation(block, player.get());
        if (block.getTileEntity().orNull() instanceof Carrier)
        {
            if (!player.get().hasPermission(module.perms().DENY_CONTAINER.getId()))
            {
                i18n.sendTranslated(player.get(), NEGATIVE, "Strong magic prevents you from accessing any inventory!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;
            lock.handleInventoryOpen(event, null, null, player.get());
        }
        else if (block.supports(Keys.OPEN))
        {
            if (!player.get().hasPermission(module.perms().DENY_DOOR.getId()))
            {
                i18n.sendTranslated(player.get(), NEGATIVE, "Strong magic prevents you from accessing any door!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;
            lock.handleBlockDoorUse(event, player.get(), block);
        }
        else if (lock != null)// other interact e.g. repeater
        {
            lock.handleBlockInteract(event, player.get());
        }
    }

    @Listener
    public void onPlayerInteractEntity(InteractEntityEvent event)
    {
        if (!this.module.getConfig().protectEntityFromRClick) return;
        Entity entity = event.getTargetEntity();
        Optional<Player> player = event.getCause().first(Player.class);
        if (!player.get().hasPermission(module.perms().DENY_ENTITY.getId()))
        {
            i18n.sendTranslated(player.get(), NEGATIVE, "Strong magic prevents you from reaching this entity!");
            event.setCancelled(true);
            return;
        }
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        if (entity instanceof Carrier || (entity.getType() == HORSE && player.get().get(Keys.IS_SNEAKING).get()))
        {
            lock.handleInventoryOpen(event, null, null, player.get());
        }
        else
        {
            lock.handleEntityInteract(event, player.get());
        }
    }

    @Listener
    public void onInventoryOpen(ViewerOpenContainerEvent event)
    {
        if (!(event.getViewer() instanceof Player) || !(event.getContainer() instanceof CarriedInventory))
        {
            return;
        }
        CarriedInventory<?> carried = event.getContainer().<CarriedInventory>query(CarriedInventory.class);
        if (carried == null)
        {
            return;
        }
        Location loc = null;
        Optional<? extends Carrier> carrier = carried.getCarrier();
        if (carrier.isPresent())
        {
            if (carrier instanceof TileEntityCarrier)
            {
                loc = ((TileEntityCarrier)carrier).getLocation();
            }
            else if (carrier instanceof Entity)
            {
                loc = ((Entity)carrier).getLocation();
            }
        }
        Lock lock = this.manager.getLockOfInventory(carried);
        if (lock == null) return;
        MultilingualPlayer user = um.getExactUser(event.getViewer().getUniqueId());
        lock.handleInventoryOpen(event, event.getContainer(), loc, user);
    }

    @Listener
    public void onEntityDamageEntity(DamageEntityEvent event)
    {
        if (!this.module.getConfig().protectEntityFromDamage) return;
        Entity entity = event.getTargetEntity();
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        Cause cause = event.getCause();
        // TODO update to this when done https://github.com/SpongePowered/SpongeAPI/pull/712
        if (cause.isPresent() && cause.get().getCause() instanceof Player)
        {
            MultilingualPlayer user = um.getMultilingualPlayer(((Player)cause.get().getCause()).getUniqueId());
            lock.handleEntityDamage(event, user);
        }
        // else other source
        if (module.getConfig().protectEntityFromEnvironementalDamage)
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onEntityDeath(DestructEntityEvent event)
    {
        Optional<Player> playerCause = event.getCause().getFirst(Player.class);
        Entity entity = event.getTargetEntity();
        if (entity.supports(Keys.VEHICLE))
        {
            if (!this.module.getConfig().protectVehicleFromBreak) return;

            Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
            if (lock == null) return;


            if (playerCause.isPresent())
            {
                MultilingualPlayer user = um.getMultilingualPlayer(playerCause.get().getUniqueId());
                if (lock.isOwner(user))
                {
                    lock.handleEntityDeletion(user);
                    return;
                }
            }

            if (module.getConfig().protectVehicleFromEnvironmental)
            {
                event.setCancelled(true);
            }
            event.setCancelled(true);
            return;
        }
        if (entity instanceof Hanging) // leash / itemframe / image
        {
            if (playerCause.isPresent())
            {
                Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
                MultilingualPlayer user = um.getMultilingualPlayer(playerCause.get().getUniqueId());
                if (user.hasPermission(module.perms().DENY_HANGING.getId()))
                {
                    event.setCancelled(true);
                    return;
                }
                if (lock == null) return;
                if (lock.handleEntityDamage(event, user))
                {
                    lock.delete(user);
                }
            }
            return;
        }

        // no need to check if allowed to kill as this would have caused an DamageEvent before / this is only to cleanup database
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        Optional<DamageableData> damageData = entity.get(DamageableData.class);
        MultilingualPlayer user = null;
        if (damageData.isPresent())
        {
            Living living = damageData.get().lastAttacker().get().orNull();
            if (living instanceof Player)
            {
                user = um.getMultilingualPlayer(living.getUniqueId());
            }
        }
        lock.handleEntityDeletion(user);
    }

    @Listener(order = Order.EARLY)
    public void onPlace(PlaceBlockEvent.SourcePlayer event)
    {
        MultilingualPlayer user = um.getExactUser(event.getSourceEntity().getUniqueId());
        for (BlockTransaction trans : event.getTransactions())
        {
            BlockSnapshot placed = trans.getFinalReplacement();
            BlockType type = placed.getState().getType();

            if (type == CHEST || type == TRAPPED_CHEST)
            {
                if (onPlaceChest(event, placed.getLocation().get(), user))
                {
                    return;
                }
            }
            else if (isDoor(type))
            {
                if (onPlaceDoor(event, placed, user))
                {
                    return;
                }
            }
            if (module.getConfig().disableAutoProtect.stream().map(ConfigWorld::getWorld).collect(toSet()).contains(
                placed.getLocation().get().getExtent()))
            {
                return;
            }
            for (BlockLockerConfiguration blockprotection : this.module.getConfig().blockprotections)
            {
                if (blockprotection.isType(type))
                {
                    if (!blockprotection.autoProtect) return;
                    this.manager.createLock(type, placed.getLocation().get(), user, blockprotection.autoProtectType, null, false);
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

    private boolean onPlaceDoor(SourcePlayer event, BlockSnapshot placed, MultilingualPlayer user)
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

        if (!relative.get(Keys.DIRECTION).get().equals(direction) || relative.get(Keys.HINGE_POSITION) == hinge)
        {
            return false; // Not a doubledoor
        }
        Lock lock = this.manager.getLockAtLocation(relative, null, false, false);
        if (lock != null)
        {
            if (!lock.validateTypeAt(relative))
            {
                user.sendTranslated(NEUTRAL, "Nearby BlockProtection is not valid!");
                lock.delete(user);
                return true;
            }

            if (placed.get(Keys.PORTION_TYPE) == TOP)
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
                user.sendTranslated(POSITIVE, "Protection expanded!");
            }
            else
            {
                event.setCancelled(true);
                user.sendTranslated(NEGATIVE, "The nearby door is protected by someone else!");
            }
            return true;
        }
        return false;
    }

    private boolean onPlaceChest(PlaceBlockEvent.SourcePlayer event, Location placed, MultilingualPlayer user)
    {
        Location relativeLoc;
        for (Direction direction : CARDINAL_DIRECTIONS)
        {
            if (placed.getBlockType() != placed.getRelative(direction).getBlockType()) // bindable chest
            {
                continue;
            }
            relativeLoc = placed.getRelative(direction);
            Lock lock = this.manager.getLockAtLocation(relativeLoc, user, false, false);
            if (lock == null)
            {
                continue;
            }
            if (!lock.validateTypeAt(relativeLoc))
            {
                user.sendTranslated(NEUTRAL, "Nearby BlockProtection is not valid!");
                lock.delete(user);
            }
            else if (lock.isOwner(user) || lock.hasAdmin(user) || user.hasPermission(module.perms().EXPAND_OTHER.getId()))
            {
                this.manager.extendLock(lock, placed);
                user.sendTranslated(POSITIVE, "Protection expanded!");
            }
            else
            {
                event.setCancelled(true);
                user.sendTranslated(NEGATIVE, "The nearby chest is protected by someone else!");
            }
            return true;
        }
        return false;
    }

    @Listener
    public void onBlockRedstone(NotifyNeighborBlockEvent.Power.SourceBlock event)
    {
        if (!this.module.getConfig().protectFromRedstone) return;

        for (BlockTransaction trans : event.getTransactions())
        {
            Location<World> location = trans.getDefaultReplacement().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null && lock.hasFlag(BLOCK_REDSTONE)
                && trans.getDefaultReplacement().getState().get(POWERED).get())
            {
                trans.setCustomReplacement(trans.getDefaultReplacement().with(POWERED, false).get());
            }
        }
    }

    @Listener
    public void onBlockPistonExtend(ChangeBlockEvent.SourceBlock event)
    {
        if (!this.module.getConfig().protectFromPistonMove) return;
        Cause cause = event.getCause();
        Optional<Player> playerCause = cause.getFirst(Player.class);
        if (playerCause.isPresent())
        {
            event.getTransactions().stream()
                 .filter(b -> manager.getLockAtLocation(b.getOriginal().getLocation().get(), null) != null)
                 .forEach(b -> b.setIsValid(false));
        }
    }

    @Listener
    public void onBlockBreak(BreakBlockEvent.SourcePlayer event)
    {
        if (!this.module.getConfig().protectFromBlockBreak) return;
        MultilingualPlayer user = um.getExactUser(event.getSourceEntity().getUniqueId());
        for (BlockTransaction trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null)
            {
                lock.handleBlockBreak(event, user);
            }

            for (Location block : BlockUtil.getDetachableBlocks(location))
            {
                lock = this.manager.getLockAtLocation(block, user);
                if (lock != null)
                {
                    lock.handleBlockBreak(event, user);
                    return;
                }
            }

            // Search for Detachable Entities
            if (module.getConfig().protectsDetachableEntities())
            {
                Set<Chunk> chunks = new HashSet<>();
                chunks.add(getChunk(location));
                chunks.add(getChunk(location.getRelative(NORTH)));
                chunks.add(getChunk(location.getRelative(EAST)));
                chunks.add(getChunk(location.getRelative(SOUTH)));
                chunks.add(getChunk(location.getRelative(WEST)));
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
                            lock.handleBlockBreak(event, user);
                        }
                    }
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onBlockExplode(WorldExplosionEvent.Detonate event)
    {
        if (!this.module.getConfig().protectBlockFromExplosion) return;
        for (BlockTransaction trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            if (manager.getLockAtLocation(location, null) != null)
            {
                trans.setIsValid(false);
            }
        }

        event.filterEntities(input -> manager.getLockForEntityUID(input.getUniqueId()) != null);
    }

    @Listener
    public void onBlockBurn(BreakBlockEvent.SourceBlock event) // TODO burning cause?
    {
        if (!this.module.getConfig().protectBlockFromFire) return;
        for (BlockTransaction trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null)
            {
                trans.setIsValid(false);
                continue;
            }
            for (Location block : BlockUtil.getDetachableBlocks(location))
            {
                lock = this.manager.getLockAtLocation(block, null);
                if (lock != null)
                {
                    trans.setIsValid(false);
                    break;
                }
            }
        }
    }

    // TODO InventoryMovements
    @Listener
    public void onHopperItemMove(InventoryMoveItemEvent event)
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

    @Listener
    public void onWaterLavaFlow(MoveBlockEvent event)
    {
        if (!this.module.getConfig().protectBlocksFromWaterLava)
        {
            return;
        }
        event.getLocations().stream().filter(b -> BlockUtil.isNonFluidProofBlock(b.getBlockType())).forEach(b -> {
            Lock lock = this.manager.getLockAtLocation(b, null);
            if (lock != null)
            {
                event.setCancelled(true);
            }
        });
    }

    @Listener
    public void onTame(TameEntityEvent.SourcePlayer event)
    {
        for (ConfigWorld world : module.getConfig().disableAutoProtect)
        {
            if (event.getEntity().getWorld().equals(world.getWorld()))
            {
                return;
            }
        }
        this.module.getConfig().entityProtections.stream()
         .filter(entityProtection -> entityProtection.isType(event.getEntity().getType())
             && entityProtection.autoProtect)
         .forEach(entityProtection -> {
             MultilingualPlayer user = um.getExactUser((event.getTamer()).getUniqueId());
             if (this.manager.getLockForEntityUID(event.getEntity().getUniqueId()) == null)
             {
                 this.manager.createLock(event.getEntity(), user, entityProtection.autoProtectType, null, false);
             }
         });
    }
}
