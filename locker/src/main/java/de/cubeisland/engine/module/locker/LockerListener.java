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
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Piston;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.manipulator.block.OpenData;
import org.spongepowered.api.data.manipulator.entity.DamageableData;
import org.spongepowered.api.data.manipulator.entity.SneakingData;
import org.spongepowered.api.data.manipulator.entity.VehicleData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.block.BlockBurnEvent;
import org.spongepowered.api.event.block.BlockMoveEvent;
import org.spongepowered.api.event.block.BlockRedstoneUpdateEvent;
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
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.BlockUtil.CARDINAL_DIRECTIONS;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.locker.storage.ProtectionFlag.*;
import static org.spongepowered.api.block.BlockTypes.CHEST;
import static org.spongepowered.api.block.BlockTypes.TRAPPED_CHEST;
import static org.spongepowered.api.entity.EntityInteractionTypes.USE;
import static org.spongepowered.api.entity.EntityTypes.HORSE;

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
        if (event.getEntity().isCompatible(VehicleData.class))
        {
            if (!this.module.getConfig().protectVehicleFromBreak) return;

            Lock lock = this.manager.getLockForEntityUID(event.getEntity().getUniqueId());
            if (lock == null) return;
            Optional<Cause> cause = event.getCause();
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

        // no need to check if allowed to kill as this would have caused an DamageEvent before / this is only to cleanup database
        Lock lock = this.manager.getLockForEntityUID(event.getEntity().getUniqueId());
        if (lock == null) return;
        Optional<DamageableData> damageData = event.getEntity().getData(DamageableData.class);
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
        if (placed.getType() == CHEST || placed.getType() == TRAPPED_CHEST)
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
                return;
            }
        }
        else if (placed.getType() == BlockTypes.WOODEN_DOOR || placed.getType() == BlockTypes.IRON_DOOR)
        {
            Location loc = location;
            Location relativeLoc;
            for (Direction blockFace : CARDINAL_DIRECTIONS)
            {
                if (placed.getType() == placed.getRelative(blockFace).getType())
                {
                    relativeLoc = placed.getRelative(blockFace);
                    Lock lock = this.manager.getLockAtLocation(relativeLoc, null, false, false);
                    if (lock != null)
                    {
                        if (!lock.validateTypeAt(relativeLoc))
                        {
                            user.sendTranslated(NEUTRAL, "Nearby BlockProtection is not valid!");
                            lock.delete(user);
                        }
                        else
                        {
                            if (!(relativeLoc.getBlock().getState().getData() instanceof Door)) return; // door is above
                            Door botRelative = (Door)relativeLoc.getBlock().getState().getData();
                            if (botRelative.isTopHalf()) return; // door is below
                            Door botDoor = (Door)placed.getState().getData();
                            Door topDoor = new Door(placed.getType(), BlockUtil.getTopDoorDataOnPlace(placed.getType(), loc, event.getPlayer()));
                            Door topRelative = (Door)relativeLoc.getBlock().getRelative(BlockFace.UP).getState().getData();
                            if (botDoor.getFacing() == botRelative.getFacing())// Doors are facing the same direction
                            {
                                if (topDoor.getData() != topRelative.getData()) // This is a doubleDoor!
                                {
                                    if (lock.isOwner(user) || lock.hasAdmin(user) || module.perms().EXPAND_OTHER.isAuthorized(user))
                                    {
                                        this.manager.extendLock(lock, loc); // bot half
                                        this.manager.extendLock(lock, loc.clone().add(0, 1, 0)); // top half
                                        user.sendTranslated(POSITIVE, "Protection expanded!");
                                    }
                                    else
                                    {
                                        event.setCancelled(true);
                                        user.sendTranslated(NEGATIVE, "The nearby door is protected by someone else!");
                                    }
                                }
                            }
                            // else do not expand protection
                        }
                        return;
                    }
                }
            }
        }
        for (ConfigWorld world : module.getConfig().disableAutoProtect)
        {
            if (location.getWorld().equals(world.getWorld()))
            {
                return ;
            }
        }
        for (BlockLockerConfiguration blockprotection : this.module.getConfig().blockprotections)
        {
            if (blockprotection.isType(placed.getType()))
            {
                if (!blockprotection.autoProtect) return;
                this.manager.createLock(placed.get(), location, user, blockprotection.autoProtectType, null, false);
                return;
            }
        }
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
        Lock lock = this.manager.getLockAtLocation(event.getBlock(), user);
        if (lock != null)
        {
            lock.handleBlockBreak(event, user);
            return;
        }
        for (Location block : BlockUtil.getDetachableBlocks(event.getBlock()))
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
            chunks.add(event.getBlock().getChunk());
            chunks.add(event.getBlock().getRelative(Direction.NORTH).getChunk());
            chunks.add(event.getBlock().getRelative(Direction.EAST).getChunk());
            chunks.add(event.getBlock().getRelative(Direction.SOUTH).getChunk());
            chunks.add(event.getBlock().getRelative(Direction.WEST).getChunk());
            Set<Hanging> hangings = new HashSet<>();
            for (Chunk chunk : chunks)
            {
                for (Entity entity : chunk.getEntities())
                {
                    if (entity instanceof Hanging)
                    {
                        hangings.add((Hanging)entity);
                    }
                }
            }
            Location entityLoc = new Location(null,0,0,0);
            for (Hanging hanging : hangings)
            {
                if (hanging.getLocation(entityLoc).getBlock().getRelative(hanging.getAttachedFace()).equals(event.getBlock()))
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
        Inventory inventory = event.getSource();
        Lock lock = this.manager.getLockOfInventory(inventory, null);
        if (lock != null)
        {
            InventoryHolder dest = event.getDestination().getHolder();
            if (((dest instanceof Hopper || dest instanceof Dropper) && !lock.hasFlag(HOPPER_OUT))
             || (dest instanceof HopperMinecart && !lock.hasFlag(HOPPER_MINECART_OUT)))
            {
                event.setCancelled(true);
            }
        }
        if (event.isCancelled()) return;
        inventory = event.getDestination();
        lock = this.manager.getLockOfInventory(inventory, null);
        if (lock != null)
        {
            InventoryHolder source = event.getSource().getHolder();
            if (((source instanceof Hopper || source instanceof Dropper) && !lock.hasFlag(HOPPER_IN))
                || (source instanceof HopperMinecart && !lock.hasFlag(HOPPER_MINECART_IN)))
            {
                event.setCancelled(true);
            }
        }
    }

    @Subscribe
    public void onWaterLavaFlow(BlockFromToEvent event)
    {
        if (this.module.getConfig().protectBlocksFromWaterLava && BlockUtil.isNonFluidProofBlock(event.getToBlock().getType()))
        {
            Lock lock = this.manager.getLockAtLocation(event.getToBlock().getLocation(), null);
            if (lock != null)
            {
                event.setCancelled(true);
            }
        }
    }

    @Subscribe
    public void onHangingBreak(HangingBreakEvent event) // leash / itemframe / image
    {
        if (event.getCause() == RemoveCause.ENTITY && event instanceof HangingBreakByEntityEvent)
        {
            if (((HangingBreakByEntityEvent)event).getRemover() instanceof Player)
            {
                Lock lock = this.manager.getLockForEntityUID(event.getEntity().getUniqueId());
                User user = um.getExactUser((((HangingBreakByEntityEvent)event).getRemover()).getUniqueId());
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
        }
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
