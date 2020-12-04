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
package org.cubeengine.module.locker.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.LockerPerm;
import org.cubeengine.module.locker.data.LockerManager;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.transaction.BlockTransaction;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.animal.horse.HorseEntity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.TransferInventoryEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Singleton
public class LockerLockedListener
{
    private LockerManager lockerManager;
    private I18n i18n;
    private LockerPerm perms;
    private Locker module;

    @Inject
    public LockerLockedListener(LockerManager lockerManager, I18n i18n, LockerPerm perms, Locker module)
    {
        this.lockerManager = lockerManager;
        this.i18n = i18n;
        this.perms = perms;
        this.module = module;
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final Optional<ServerLocation> serverLoc = event.getBlock().getLocation();
        final DataHolder.Mutable blockEntityOrLocation = lockerManager.getDataHolderAtLoc(serverLoc.orElse(null));
        if (this.handleLockedInteraction(player, blockEntityOrLocation, true))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onInteractEntity(InteractEntityEvent event, @Root ServerPlayer player)
    {
        // TODO Living handled by AttackEntityEvent instead?
        if (this.handleLockedInteraction(player, event.getEntity(), event instanceof InteractEntityEvent.Secondary))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onAttackEntity(AttackEntityEvent event, @First EntityDamageSource source)
    {
        ServerPlayer player = findDamageSource(source);
        if (this.handleLockedInteraction(player, event.getEntity(), false))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onRedstone(NotifyNeighborBlockEvent event)
    {
        final BlockSnapshot snap = event.getContext().get(EventContextKeys.NEIGHBOR_NOTIFY_SOURCE).orElse(null);
        if (snap == null)
        {
            return;
        }
        final ServerLocation loc = snap.getLocation().get();
        // TODO maybe the blockstate could have the data copied over?
        event.filterDirections(direction -> !lockerManager.handleRedstoneInteract(loc.relativeTo(direction)));
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        final Set<Vector3i> checkedPos = new HashSet<>();
        for (BlockTransaction transaction : event.getTransactions())
        {
            if (transaction.getOperation().equals(Operations.BREAK.get()))
            {
                if (lockerManager.handleBlockBreak(transaction.getOriginal(), player, true, checkedPos))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    public void onBlockPiston(ChangeBlockEvent.Pre event)
    {
        // TODO piston events needed?
    }


    public void onNotifyPiston(NotifyNeighborBlockEvent event, @Root LocatableBlock block)
    {
        // TODO check if piston move would break protected block?
        // check for 12 iterations in direction of piston
        // stop when block is not pushed by piston
        // dont forget e.g. slime blocks for retraction
    }

    // TODO entity death?

    @Listener
    public void onHopper(TransferInventoryEvent.Pre event)
    {
        if (lockerManager.handleHopperInteract(event.getSourceInventory(), event.getTargetInventory()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onBlockBreakByBlock(ChangeBlockEvent.All event, @First BlockSnapshot maybeFire)
    {
        if (maybeFire.getState().getType().isAnyOf(BlockTypes.FIRE)) // other types? maybe water/lava
        {
            final Set<Vector3i> checkedPos = new HashSet<>();
            // TODO missing original data
            for (BlockTransaction transaction : event.getTransactions())
            {
                if (transaction.getOperation().equals(Operations.BREAK.get())
                    && lockerManager.handleBlockBreak(transaction.getOriginal(), null, true, checkedPos))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }


    @Listener
    public void onExplosion(ExplosionEvent.Detonate event)
    {
        Set<Vector3i> checked = new HashSet<>();
        // TODO missing original data
        event.filterAffectedLocations(loc -> !lockerManager.handleBlockBreak(loc.createSnapshot(), null, true, checked));
        event.filterEntities(entity -> !lockerManager.handleEntityDamage(entity, null));
    }

    public ServerPlayer findDamageSource(EntityDamageSource source)
    {
        ServerPlayer player = null;
        if (source.getSource() instanceof ServerPlayer)
        {
            player = (ServerPlayer) source.getSource();
        }
        else if (source.getSource() instanceof Projectile)
        {
            final ProjectileSource projectileSource = ((Projectile)source.getSource()).shooter().get();
            if (projectileSource instanceof ServerPlayer)
            {
                player = ((ServerPlayer)projectileSource);
            }
        }
        return player;
    }

    public boolean handleLockedInteraction(ServerPlayer player, DataHolder.Mutable dataHolder, boolean secondary)
    {
        if (dataHolder != null)
        {
            final boolean isInventory = secondary && !(dataHolder instanceof HorseEntity && !player.get(Keys.IS_SNEAKING).orElse(false)); // Horse Inventory only opens when sneaking
            if (dataHolder instanceof Carrier && isInventory) // Is it an inventory?
            {
                return lockerManager.handleInventoryOpen(dataHolder, player, ((Carrier)dataHolder).getInventory());
            }
            else if (dataHolder instanceof Entity)
            {
                if (secondary)
                {
                    return lockerManager.handleEntityInteract(dataHolder, player);
                }
                return lockerManager.handleEntityDamage(dataHolder, player);
            }
            else
            {
                return lockerManager.handleBlockInteract(dataHolder, player);
            }
        }
        return false;
    }

}
