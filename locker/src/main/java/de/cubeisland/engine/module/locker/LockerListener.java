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
import de.cubeisland.engine.module.core.util.BlockUtil;
import de.cubeisland.engine.module.locker.storage.Lock;
import de.cubeisland.engine.module.locker.storage.LockManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.block.BlockBreakEvent;
import org.spongepowered.api.event.block.BlockBurnEvent;
import org.spongepowered.api.event.block.BlockPlaceEvent;
import org.spongepowered.api.event.block.BlockRedstoneUpdateEvent;
import org.spongepowered.api.event.entity.EntityChangeHealthEvent;
import org.spongepowered.api.event.entity.EntityDeathEvent;
import org.spongepowered.api.event.entity.EntityExplosionEvent;
import org.spongepowered.api.event.entity.EntityTameEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEntityEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEvent;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.locker.storage.ProtectionFlag.*;

public class LockerListener
{
    private final LockManager manager;
    private final Locker module;

    public LockerListener(Locker module, LockManager manager)
    {
        this.module = module;
        this.manager = manager;
    }

    @Subscribe
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (!this.module.getConfig().protectBlockFromRClick) return;
        if (event.useInteractedBlock() == DENY) return;
        if (event.getAction() != RIGHT_CLICK_BLOCK) return;
        User user = um.getExactUser(event.getPlayer().getUniqueId());
        Location location = event.getClickedBlock().getLocation();
        Lock lock = this.manager.getLockAtLocation(location, user);
        if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof InventoryHolder)
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
        else if (event.getClickedBlock().getState().getData() instanceof Openable)
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
        if (event.isCancelled()) event.setUseInteractedBlock(DENY);
    }

    @Subscribe
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (!this.module.getConfig().protectEntityFromRClick) return;
        Entity entity = event.getRightClicked();
        User user = um.getExactUser(event.getUser().getUniqueId());
        if (module.perms().DENY_ENTITY.isAuthorized(user))
        {
            user.sendTranslated(NEGATIVE, "Strong magic prevents you from reaching this entity!");
            event.setCancelled(true);
            return;
        }
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        if (entity instanceof StorageMinecart
            || entity instanceof HopperMinecart
            || (entity.getType() == EntityType.HORSE && event.getPlayer().isSneaking()))
        {
            lock.handleInventoryOpen(event, null, null, user);
        }
        else
        {
            lock.handleEntityInteract(event, user);
        }
    }

    @Subscribe
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if (!(event.getPlayer() instanceof Player)) return;
        Lock lock = this.manager.getLockOfInventory(event.getInventory(), holderLoc);
        if (lock == null) return;
        User user = um.getExactUser(event.getPlayer().getUniqueId());
        lock.handleInventoryOpen(event, event.getInventory(), holderLoc, user);
    }

    public void onEntityDamageEntity(EntityDamageByEntityEvent event)
    {
        Entity entity = event.getEntity();
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        if (event.getDamager() instanceof Player)
        {
            User user = um.getExactUser(event.getDamager().getUniqueId());
            lock.handleEntityDamage(event, user);
            return;
        }
        else if (event.getDamager() instanceof TNTPrimed)
        {
            Entity source = ((TNTPrimed)event.getDamager()).getSource();
            if (source != null && source instanceof Player)
            {
                User user = um.getExactUser(source.getUniqueId());
                lock.handleEntityDamage(event, user);
                return;
            }
        }
        else if (event.getDamager() instanceof Projectile)
        {
            ProjectileSource shooter = ((Projectile)event.getDamager()).getShooter();
            if (shooter != null && shooter instanceof Player)
            {
                User user = um.getExactUser(((Player)shooter).getUniqueId());
                lock.handleEntityDamage(event, user);
                return;
            }
        }
        // else other source
        if (module.getConfig().protectEntityFromEnvironementalDamage)
        {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onEntityDamageEvent(EntityChangeHealthEvent event)
    {
        if (!this.module.getConfig().protectEntityFromDamage) return;
        if (event instanceof EntityDamageByEntityEvent)
        {
            this.onEntityDamageEntity((EntityDamageByEntityEvent)event);
        }
        else if (module.getConfig().protectEntityFromEnvironementalDamage)
        {
            Entity entity = event.getEntity();
            Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
            if (lock == null) return;
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onEntityDeath(EntityDeathEvent event)
    {
        // no need to check if allowed to kill as this would have caused an DamageEvent before / this is only to cleanup database
        Lock lock = this.manager.getLockForEntityUID(event.getEntity().getUniqueId());
        if (lock == null) return;
        EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        User user = null;
        if (lastDamage != null && lastDamage instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent)lastDamage).getDamager() instanceof Player)
        {
            user = um.getExactUser(((EntityDamageByEntityEvent)lastDamage).getDamager().getUniqueId());
        }
        lock.handleEntityDeletion(user);
    }

    @Subscribe
    public void onVehicleBreak(VehicleDestroyEvent event)
    {
        if (!this.module.getConfig().protectVehicleFromBreak) return;
        Lock lock = this.manager.getLockForEntityUID(event.getVehicle().getUniqueId());
        if (lock == null) return;
        if (event.getAttacker() == null)
        {
            if (module.getConfig().protectVehicleFromEnvironmental)
            {
                event.setCancelled(true);
            }
            return;
        }
        User user = um.getExactUser(event.getAttacker().getUniqueId());
        if (lock.isOwner(user))
        {
            lock.handleEntityDeletion(user);
            return;
        }
        event.setCancelled(true);
    }

    @Subscribe(order = Order.EARLY)
    public void onPlace(BlockPlaceEvent event)
    {
        if (!event.canBuild()) return;
        Block placed = event.getBlockPlaced();
        User user = um.getExactUser(event.getPlayer().getUniqueId());
        Location location = placed.getLocation();
        if (placed.getType() == BlockTypes.CHEST || placed.getType() == BlockTypes.TRAPPED_CHEST)
        {
            Location relativeLoc = new Location(null,0,0,0);
            for (BlockFace blockFace : BlockUtil.CARDINAL_DIRECTIONS)
            {
                if (placed.getType() == placed.getRelative(blockFace).getType()) // bindable chest
                {
                    placed.getRelative(blockFace).getLocation(relativeLoc);
                    Lock lock = this.manager.getLockAtLocation(relativeLoc, user, false, false);
                    if (lock != null)
                    {
                        if (!lock.validateTypeAt(relativeLoc))
                        {
                            user.sendTranslated(NEUTRAL, "Nearby BlockProtection is not valid!");
                            lock.delete(user);
                        }
                        else if (lock.isOwner(user) || lock.hasAdmin(user) || module.perms().EXPAND_OTHER.isAuthorized(user))
                        {
                            this.manager.extendLock(lock, event.getBlockPlaced().getLocation());
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
            }
        }
        else if (placed.getType() == BlockTypes.WOODEN_DOOR || placed.getType() == BlockTypes.IRON_DOOR)
        {
            Location loc = location;
            Location relativeLoc = new Location(null,0,0,0);
            for (BlockFace blockFace : BlockUtil.CARDINAL_DIRECTIONS)
            {
                if (placed.getType() == placed.getRelative(blockFace).getType())
                {
                    placed.getRelative(blockFace).getLocation(relativeLoc);
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
                this.manager.createLock(placed.getType(), location, user, blockprotection.autoProtectType, null, false);
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
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        if (!this.module.getConfig().protectFromPistonMove) return;
        Location location = event.getBlock().getLocation();
        for (Block block : event.getBlocks())
        {
            Lock lock = this.manager.getLockAtLocation(block.getLocation(location), null);
            if (lock != null)
            {
                event.setCancelled(true);
                return;
            }
        }
        Lock lock = this.manager.getLockAtLocation(location.getBlock().getRelative(event.getDirection()).getLocation(location), null);
        if (lock != null)
        {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        if (!this.module.getConfig().protectFromPistonMove) return;
        Lock lock = this.manager.getLockAtLocation(event.getRetractLocation(), null);
        if (lock != null)
        {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (!this.module.getConfig().protectFromBlockBreak) return;
        User user = um.getExactUser(event.getPlayer().getUniqueId());
        Lock lock = this.manager.getLockAtLocation(event.getBlock().getLocation(), user);
        if (lock != null)
        {
            lock.handleBlockBreak(event, user);
        }
        else
        {
            // Search for Detachable Blocks
            Location location = new Location(null,0,0,0);
            for (Block block : BlockUtil.getDetachableBlocks(event.getBlock()))
            {
                lock = this.manager.getLockAtLocation(block.getLocation(location), user);
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
                chunks.add(event.getBlock().getRelative(BlockFace.NORTH).getChunk());
                chunks.add(event.getBlock().getRelative(BlockFace.EAST).getChunk());
                chunks.add(event.getBlock().getRelative(BlockFace.SOUTH).getChunk());
                chunks.add(event.getBlock().getRelative(BlockFace.WEST).getChunk());
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
    }

    @Subscribe
    public void onBlockExplode(EntityExplosionEvent event)
    {
        if (!this.module.getConfig().protectBlockFromExplosion) return;
        Location location = new Location(null,0,0,0);
        for (Block block : event.blockList())
        {
            Lock lock = this.manager.getLockAtLocation(block.getLocation(location), null);
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
        Location location = event.getBlock().getLocation();
        Lock lock = this.manager.getLockAtLocation(location, null);
        if (lock != null)
        {
            event.setCancelled(true);
            return;
        }
        for (Block block : BlockUtil.getDetachableBlocks(event.getBlock()))
        {
            lock = this.manager.getLockAtLocation(block.getLocation(location), null);
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
        if (event.getOwner() instanceof Player)
        {
            for (ConfigWorld world : module.getConfig().disableAutoProtect)
            {
                if (event.getEntity().getWorld().equals(world.getWorld()))
                {
                    return;
                }
            }
            for (EntityLockerConfiguration entityProtection : this.module.getConfig().entityProtections)
            {
                if (entityProtection.isType(event.getEntityType()) && entityProtection.autoProtect)
                {
                    User user = um.getExactUser((event.getOwner()).getUniqueId());
                    if (this.manager.getLockForEntityUID(event.getEntity().getUniqueId()) == null)
                    {
                        this.manager.createLock(event.getEntity(), user, entityProtection.autoProtectType, null, false);
                    }
                }
            }
        }
    }
}
