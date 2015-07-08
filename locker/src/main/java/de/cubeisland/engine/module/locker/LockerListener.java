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
package de.cubeisland.engine.module.locker;

import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.core.util.BlockUtil;
import de.cubeisland.engine.module.locker.storage.Lock;
import de.cubeisland.engine.module.locker.storage.LockManager;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserManager;
import de.cubeisland.engine.service.world.ConfigWorld;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.Piston;
import org.spongepowered.api.block.tileentity.carrier.Dropper;
import org.spongepowered.api.block.tileentity.carrier.Hopper;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.manipulator.block.DirectionalData;
import org.spongepowered.api.data.manipulator.block.HingeData;
import org.spongepowered.api.data.manipulator.block.OpenData;
import org.spongepowered.api.data.manipulator.block.PortionData;
import org.spongepowered.api.data.manipulator.entity.DamageableData;
import org.spongepowered.api.data.manipulator.entity.SneakingData;
import org.spongepowered.api.data.manipulator.entity.VehicleData;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.vehicle.minecart.MinecartHopper;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.block.BlockBurnEvent;
import org.spongepowered.api.event.block.BlockMoveEvent;
import org.spongepowered.api.event.block.BlockRedstoneUpdateEvent;
import org.spongepowered.api.event.block.FluidSpreadEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.EntityDeathEvent;
import org.spongepowered.api.event.entity.EntityExplosionEvent;
import org.spongepowered.api.event.entity.EntityTameEvent;
import org.spongepowered.api.event.entity.living.LivingChangeHealthEvent;
import org.spongepowered.api.event.entity.player.PlayerBreakBlockEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractBlockEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEntityEvent;
import org.spongepowered.api.event.entity.player.PlayerPlaceBlockEvent;
import org.spongepowered.api.event.inventory.ContainerOpenEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.BlockUtil.CARDINAL_DIRECTIONS;
import static de.cubeisland.engine.module.core.util.BlockUtil.getChunk;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.*;
import static de.cubeisland.engine.module.locker.storage.ProtectionFlag.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.data.type.PortionTypes.TOP;
import static org.spongepowered.api.entity.EntityInteractionTypes.USE;
import static org.spongepowered.api.entity.EntityTypes.HORSE;
import static org.spongepowered.api.util.Direction.*;

public class LockerListener
{
    private final LockManager manager;
    private UserManager um;
    private final Locker module;

    public LockerListener(Locker module, LockManager manager, UserManager um)
    {
        this.module = module;
        this.manager = manager;
        this.um = um;
    }

    @Subscribe
    public void onPlayerInteract(PlayerInteractBlockEvent event)
    {
        if (!this.module.getConfig().protectBlockFromRClick) return;
        if (event.getInteractionType() != USE) return;
        User user = um.getExactUser(event.getUser().getUniqueId());
        Location location = event.getBlock();
        Lock lock = this.manager.getLockAtLocation(location, user);
        Location block = event.getBlock();
        if (block.getTileEntity().orNull() instanceof Carrier)
        {
            if (module.perms().DENY_CONTAINER.isAuthorized(user))
            {
                user.sendTranslated(NEGATIVE, "Strong magic prevents you from accessing any inventory!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;
            lock.handleInventoryOpen(event, null, null, user);
        }
        else if (block.isCompatible(OpenData.class))
        {
            if (module.perms().DENY_DOOR.isAuthorized(user))
            {
                user.sendTranslated(NEGATIVE, "Strong magic prevents you from accessing any door!");
                event.setCancelled(true);
                return;
            }
            if (lock == null) return;
            lock.handleBlockDoorUse(event, user, location);
        }
        else if (lock != null)// other interact e.g. repeater
        {
            lock.handleBlockInteract(event, user);
        }
    }

    @Subscribe
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (!this.module.getConfig().protectEntityFromRClick) return;
        Entity entity = event.getTargetEntity();
        User user = um.getExactUser(event.getUser().getUniqueId());
        if (module.perms().DENY_ENTITY.isAuthorized(user))
        {
            user.sendTranslated(NEGATIVE, "Strong magic prevents you from reaching this entity!");
            event.setCancelled(true);
            return;
        }
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        if (entity instanceof Carrier || (entity.getType() == HORSE && event.getUser().getData(SneakingData.class).isPresent()))
        {
            lock.handleInventoryOpen(event, null, null, user);
        }
        else
        {
            lock.handleEntityInteract(event, user);
        }
    }

    @Subscribe
    public void onInventoryOpen(ContainerOpenEvent event)
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
                loc = ((TileEntityCarrier)carrier).getBlock();
            }
            else if (carrier instanceof Entity)
            {
                loc = ((Entity)carrier).getLocation();
            }
        }
        Lock lock = this.manager.getLockOfInventory(carried);
        if (lock == null) return;
        User user = um.getExactUser(event.getViewer().getUniqueId());
        lock.handleInventoryOpen(event, event.getContainer(), loc, user);
    }

    @Subscribe
    public void onEntityDamageEntity(LivingChangeHealthEvent event)
    {
        if (!this.module.getConfig().protectEntityFromDamage) return;
        Living entity = event.getEntity();
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        Optional<Cause> cause = event.getCause();
        // TODO update to this when done https://github.com/SpongePowered/SpongeAPI/pull/712
        if (cause.isPresent() && cause.get().getCause() instanceof Player)
        {
            User user = um.getExactUser(((Player)cause.get().getCause()).getUniqueId());
            lock.handleEntityDamage(event, user);
        }
        // else other source
        if (module.getConfig().protectEntityFromEnvironementalDamage)
        {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onEntityDeath(EntityDeathEvent event)
    {
        Optional<Cause> cause = event.getCause();
        Entity entity = event.getEntity();
        if (entity.isCompatible(VehicleData.class))
        {
            if (!this.module.getConfig().protectVehicleFromBreak) return;

            Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
            if (lock == null) return;

            if (cause.isPresent() && cause.get().getCause() instanceof Player)
            {
                User user = um.getExactUser(((Player)cause.get().getCause()).getUniqueId());
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
            if (cause.isPresent() && cause.get().getCause() instanceof Player)
            {
                Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
                User user = um.getExactUser(((Player)cause.get().getCause()).getUniqueId());
                if (module.perms().DENY_HANGING.isAuthorized(user))
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
        Optional<DamageableData> damageData = entity.getData(DamageableData.class);
        User user = null;
        if (damageData.isPresent())
        {
            Living living = damageData.get().getLastAttacker().orNull();
            if (living instanceof Player)
            {
                user = um.getExactUser(living.getUniqueId());
            }
        }
        lock.handleEntityDeletion(user);
    }

    @Subscribe(order = Order.EARLY)
    public void onPlace(PlayerPlaceBlockEvent event)
    {
        Location placed = event.getBlock();
        User user = um.getExactUser(event.getUser().getUniqueId());
        BlockType type = placed.getType();
        if (type == CHEST || type == TRAPPED_CHEST)
        {
            if (onPlaceChest(event, placed, user))
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
        if (module.getConfig().disableAutoProtect.stream().map(ConfigWorld::getWorld).collect(toSet()).contains(placed.getExtent()))
        {
            return;
        }
        for (BlockLockerConfiguration blockprotection : this.module.getConfig().blockprotections)
        {
            if (blockprotection.isType(type))
            {
                if (!blockprotection.autoProtect) return;
                this.manager.createLock(type, placed, user, blockprotection.autoProtectType, null, false);
                return;
            }
        }
    }

    private boolean isDoor(BlockType type)
    {
        return type == WOODEN_DOOR || type == IRON_DOOR || type == SPRUCE_DOOR || type == BIRCH_DOOR
            || type == JUNGLE_DOOR || type == ACACIA_DOOR || type == DARK_OAK_DOOR;
    }

    private boolean onPlaceDoor(PlayerPlaceBlockEvent event, Location placed, User user)
    {
        BlockState doorState = event.getReplacementBlock().getState();
        Hinge hinge = doorState.getManipulator(HingeData.class).get().getValue();
        Direction direction = doorState.getManipulator(DirectionalData.class).get().getValue();

        Location relative = placed.getRelative(BlockUtil.getOtherDoorDirection(direction, hinge));
        if (!isDoor(relative.getType()))
        {
            return false; // Not a door
        }
        if (!relative.getData(DirectionalData.class).get().getValue().equals(direction)
            || relative.getData(HingeData.class).get().getValue() == hinge)
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
            if (placed.getData(PortionData.class).get().getValue() == TOP)
            {
                relative = placed.getRelative(DOWN);
            }
            else
            {
                relative = placed.getRelative(UP);
            }

            if (lock.isOwner(user) || lock.hasAdmin(user) || module.perms().EXPAND_OTHER.isAuthorized(user))
            {
                this.manager.extendLock(lock, placed);
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

    private boolean onPlaceChest(PlayerPlaceBlockEvent event, Location placed, User user)
    {
        Location relativeLoc;
        for (Direction direction : CARDINAL_DIRECTIONS)
        {
            if (placed.getType() != placed.getRelative(direction).getType()) // bindable chest
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
            else if (lock.isOwner(user) || lock.hasAdmin(user) || module.perms().EXPAND_OTHER.isAuthorized(user))
            {
                this.manager.extendLock(lock, event.getBlock());
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

    @Subscribe
    public void onBlockRedstone(BlockRedstoneUpdateEvent event)
    {
        if (!this.module.getConfig().protectFromRedstone) return;
        Location block = event.getBlock();
        Lock lock = this.manager.getLockAtLocation(block, null);
        if (lock != null)
        {
            if (lock.hasFlag(BLOCK_REDSTONE))
            {
                event.setNewSignalStrength(event.getOldSignalStrength());
            }
        }
    }

    @Subscribe
    public void onBlockPistonExtend(BlockMoveEvent event)
    {
        if (!this.module.getConfig().protectFromPistonMove) return;
        Optional<Cause> cause = event.getCause();
        if (!cause.isPresent() || !(cause.get().getCause() instanceof Piston))
        {
            return;
        }
        for (Location block : event.getBlocks())
        {
            Lock lock = this.manager.getLockAtLocation(block, null);
            if (lock != null)
            {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Subscribe
    public void onBlockBreak(PlayerBreakBlockEvent event)
    {
        if (!this.module.getConfig().protectFromBlockBreak) return;
        User user = um.getExactUser(event.getUser().getUniqueId());
        Location location = event.getBlock();
        Lock lock = this.manager.getLockAtLocation(location, user);
        if (lock != null)
        {
            lock.handleBlockBreak(event, user);
            return;
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
                if (entityLoc.getRelative(hanging.getDirectionalData().getValue()).equals(location))
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

    @Subscribe
    public void onBlockExplode(EntityExplosionEvent event)
    {
        if (!this.module.getConfig().protectBlockFromExplosion) return;
        for (Location block : event.getBlocks())
        {
            Lock lock = this.manager.getLockAtLocation(block, null);
            if (lock != null)
            {
                event.setCancelled(true);
            }
        }
    }

    @Subscribe
    public void onBlockBurn(BlockBurnEvent event)
    {
        if (!this.module.getConfig().protectBlockFromFire) return;
        Location location = event.getBlock();
        Lock lock = this.manager.getLockAtLocation(location, null);
        if (lock != null)
        {
            event.setCancelled(true);
            return;
        }
        for (Location block : BlockUtil.getDetachableBlocks(event.getBlock()))
        {
            lock = this.manager.getLockAtLocation(block, null);
            if (lock != null)
            {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Subscribe
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

    @Subscribe
    public void onWaterLavaFlow(FluidSpreadEvent event)
    {
        if (!this.module.getConfig().protectBlocksFromWaterLava)
        {
            return;
        }
        event.getBlocks().stream().filter(b -> BlockUtil.isNonFluidProofBlock(b.getType())).forEach(b -> {
            Lock lock = this.manager.getLockAtLocation(b, null);
            if (lock != null)
            {
                event.setCancelled(true);
            }
        });
    }

    @Subscribe
    public void onTame(EntityTameEvent event)
    {
        if (!(event.getTamer() instanceof Player))
        {
            return;
        }
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
             User user = um.getExactUser((event.getTamer()).getUniqueId());
             if (this.manager.getLockForEntityUID(event.getEntity().getUniqueId()) == null)
             {
                 this.manager.createLock(event.getEntity(), user, entityProtection.autoProtectType, null, false);
             }
         });
    }
}
