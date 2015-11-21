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
import java.util.Optional;
import java.util.Set;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.DamageableData;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.MoveBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierBuilder;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.TameEntityEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
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
    private Game game;
    private final Locker module;

    public LockerListener(Locker module, LockManager manager, UserManager um, I18n i18n, Game game)
    {
        this.module = module;
        this.manager = manager;
        this.um = um;
        this.i18n = i18n;
        this.game = game;
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
        if (block.getTileEntity().orElse(null) instanceof Carrier)
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
    public void onInventoryOpen(InteractInventoryEvent.Open event)
    {
        Optional<Player> playerCause = event.getCause().first(Player.class);
        if (!playerCause.isPresent() || !(event.getTargetInventory() instanceof CarriedInventory)
         || !((CarriedInventory) event.getTargetInventory()).getCarrier().isPresent())
        {
            return;
        }
        Object carrier = ((CarriedInventory) event.getTargetInventory()).getCarrier().get();
        Location loc = null;
        if (carrier instanceof TileEntityCarrier)
        {
            loc = ((TileEntityCarrier) carrier).getLocation();
        }
        else if (carrier instanceof Entity)
        {
            loc = ((Entity) carrier).getLocation();
        }
        Lock lock = this.manager.getLockOfInventory(((CarriedInventory) event.getTargetInventory()));
        if (lock == null) return;
        lock.handleInventoryOpen(event, event.getTargetInventory(), loc, playerCause.get());
    }

    @Listener
    public void onEntityDamageEntity(DamageEntityEvent event)
    {
        if (!this.module.getConfig().protectEntityFromDamage) return;
        Entity entity = event.getTargetEntity();
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        Optional<Player> playerCause = event.getCause().first(Player.class);
        if (playerCause.isPresent())
        {
            lock.handleEntityDamage(playerCause.get());
            event.setBaseDamage(0); // TODO is this canceling all damage?
            event.setDamage(DamageModifierBuilder.builder().type(DamageModifierTypes.ARMOR).cause(Cause.of(module)).build(), i -> 0d); // TODO or do i need this?
        }
        // else other source
        if (module.getConfig().protectEntityFromEnvironementalDamage)
        {
            event.setBaseDamage(0); // TODO is this canceling all damage?
            event.setDamage(DamageModifierBuilder.builder().type(DamageModifierTypes.ARMOR).cause(Cause.of(module)).build(), i -> 0d); // TODO or do i need this?
        }
    }

    @Listener
    public void onEntityDeath(DamageEntityEvent event)
    {
        // TODO cancelling here ONLY cancels the MessageSink part
        Optional<Player> playerCause = event.getCause().first(Player.class);
        Entity entity = event.getTargetEntity();
        if (entity.supports(Keys.VEHICLE))
        {
            if (!this.module.getConfig().protectVehicleFromBreak) return;

            Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
            if (lock == null) return;
            if (playerCause.isPresent())
            {
                Player player = playerCause.get();

                if (lock.isOwner(player))
                {
                    lock.handleEntityDeletion(player);
                    return;
                }
            }

            if (module.getConfig().protectVehicleFromEnvironmental)
            {
                event.setBaseDamage(0);
                // TODO cancel? event.setCancelled(true);
            }
            event.setBaseDamage(0);
            // TODO cancel? event.setCancelled(true);
            return;
        }
        if (entity instanceof Hanging) // leash / itemframe / image
        {
            if (playerCause.isPresent())
            {
                Player player = playerCause.get();

                Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
                if (player.hasPermission(module.perms().DENY_HANGING.getId()))
                {
                    event.setBaseDamage(0);
                    // TODO cancel? event.setCancelled(true);
                    return;
                }
                if (lock == null) return;
                if (lock.handleEntityDamage(player))
                {
                    lock.delete(player);
                }
                else
                {
                    event.setBaseDamage(0);
                    // TODO cancel?  event.setCancelled(true);
                }
            }
            return;
        }

        // no need to check if allowed to kill as this would have caused an DamageEvent before / this is only to cleanup database
        Lock lock = this.manager.getLockForEntityUID(entity.getUniqueId());
        if (lock == null) return;
        Optional<DamageableData> damageData = entity.get(DamageableData.class);
        Player user = null;
        if (damageData.isPresent())
        {
            Living living = damageData.get().lastAttacker().get().orElse(null);
            if (living instanceof Player)
            {
                user = ((Player)living);
            }
        }
        lock.handleEntityDeletion(user);
    }

    @Listener(order = Order.EARLY)
    public void onPlace(ChangeBlockEvent.Place event)
    {
        Optional<Player> playerCause = event.getCause().first(Player.class);
        if (!playerCause.isPresent())
        {
            return;
        }
        if (playerCause.get().get(Keys.IS_SNEAKING).orElse(false))
        {
            return;
        }
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            BlockSnapshot placed = trans.getFinal();
            BlockType type = placed.getState().getType();

            if (type == CHEST || type == TRAPPED_CHEST)
            {
                if (onPlaceChest(event, placed.getLocation().get(), playerCause.get()))
                {
                    return;
                }
            }
            else if (isDoor(type))
            {
                if (onPlaceDoor(event, placed, playerCause.get()))
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
                    this.manager.createLock(placed.getLocation().get(), playerCause.get(), blockprotection.autoProtectType, null, false);
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

    @Listener
    public void onBlockRedstone(ChangeBlockEvent.Modify event)
    {
        // TODO only redstone modifications
        if (!this.module.getConfig().protectFromRedstone) return;

        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            Location<World> location = trans.getDefault().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null && lock.hasFlag(BLOCK_REDSTONE)
                && trans.getDefault().getState().get(POWERED).get())
            {
                trans.setCustom(trans.getDefault().with(POWERED, false).get());
            }
        }
    }

    @Listener
    public void onBlockPistonExtend(MoveBlockEvent event)
    {
        // TODO piston moves
        if (!this.module.getConfig().protectFromPistonMove) return;
        Cause cause = event.getCause();
        Optional<Player> playerCause = cause.first(Player.class);
        if (playerCause.isPresent())
        {
            event.getTransactions().stream()
                 .filter(b -> manager.getLockAtLocation(b.getOriginal().getLocation().get(), null) != null)
                 .forEach(b -> b.setValid(false));
        }
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event)
    {
        if (!this.module.getConfig().protectFromBlockBreak) return;
        Optional<Player> playerCause = event.getCause().first(Player.class);
        if (!playerCause.isPresent())
        {
            return;
        }
        for (Transaction<BlockSnapshot> trans : event.getTransactions())
        {
            Location<World> location = trans.getOriginal().getLocation().get();
            Lock lock = manager.getLockAtLocation(location, null);
            if (lock != null)
            {
                lock.handleBlockBreak(event, playerCause.get());
            }

            for (Location<World> block : BlockUtil.getDetachableBlocks(location))
            {
                lock = this.manager.getLockAtLocation(block, playerCause.get());
                if (lock != null)
                {
                    lock.handleBlockBreak(event, playerCause.get());
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
                            lock.handleBlockBreak(event, playerCause.get());
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
    public void onBlockBurn(ChangeBlockEvent.Break event) // TODO burning cause?
    {
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

    /* TODO
    @Listener
    public void onHopperItemMove(InteractInventoryEvent event) // TODO InventoryMovements
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
    public void onWaterLavaFlow(ChangeBlockEvent.Fluid event)
    {
        if (!this.module.getConfig().protectBlocksFromWaterLava)
        {
            return;
        }

        event.getTransactions().stream()
             .filter(b -> BlockUtil.isNonFluidProofBlock(b.getOriginal().getState().getType()))
             .forEach(b -> {
                 Lock lock = this.manager.getLockAtLocation(b.getOriginal().getLocation().get(), null);
                 if (lock != null)
                 {
                     event.setCancelled(true);
                 }
             });
    }

    @Listener
    public void onTame(TameEntityEvent event)
    {
        Entity entity = event.getTargetEntity();
        for (ConfigWorld world : module.getConfig().disableAutoProtect)
        {
            if (entity.getWorld().equals(world.getWorld()))
            {
                return;
            }
        }
        Optional<Player> playerCause = event.getCause().first(Player.class);
        if (playerCause.isPresent())
        {
            this.module.getConfig().entityProtections.stream()
                 .filter(entityProtection -> entityProtection.isType(entity.getType()) && entityProtection.autoProtect)
                 .forEach(entityProtection -> {
                     if (this.manager.getLockForEntityUID(entity.getUniqueId()) == null)
                     {
                         this.manager.createLock(entity, playerCause.get(), entityProtection.autoProtectType, null, false);
                     }
                 });
        }
    }
}
