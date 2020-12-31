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

import java.util.Arrays;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.locker.data.LockerManager;
import org.cubeengine.module.locker.data.LockerData;
import org.cubeengine.module.locker.data.LockerMode;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataHolder.Mutable;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;

@Singleton
public class LockerBookListener
{
    private I18n i18n;
    private LockerManager lockerManager;

    @Inject
    public LockerBookListener(I18n i18n, LockerManager lockerManager)
    {
        this.i18n = i18n;
        this.lockerManager = lockerManager;
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        if (!event.getContext().get(EventContextKeys.USED_HAND).map(hand -> hand.equals(HandTypes.MAIN_HAND.get())).orElse(false))
        {
            return;
        }
        final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        final Optional<String> mode = itemInHand.get(LockerData.MODE);
        if (mode.isPresent())
        {
            final BlockEntity blockEntity = event.getBlock().getLocation().flatMap(Location::getBlockEntity).orElse(null);
            if (this.handleLockerBookInteraction(player, LockerMode.valueOf(mode.get()), blockEntity, itemInHand))
            {
                event.setCancelled(true);
            }
        }
    }


    @Listener
    public void onInteractEntity(InteractEntityEvent.Secondary event, @Root ServerPlayer player)
    {
        final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        final Optional<String> mode = itemInHand.get(LockerData.MODE);
        if (mode.isPresent() && event.getContext().get(EventContextKeys.USED_HAND).map(hand -> hand.equals(HandTypes.MAIN_HAND.get())).orElse(false))
        {
            if (this.handleLockerBookInteraction(player, LockerMode.valueOf(mode.get()), event.getEntity(), itemInHand))
            {
                event.setCancelled(true);
            }
        }
    }

    public boolean handleLockerBookInteraction(ServerPlayer player, LockerMode mode, Mutable dataHolder, ItemStack itemInHand)
    {
        if (player.get(Keys.IS_SNEAKING).orElse(false))
        {
            lockerManager.openBook(dataHolder, player);
            return true;
        }

        Integer lockFlags = null;
        if (dataHolder != null)
        {
            lockFlags = dataHolder.get(LockerData.FLAGS).orElse(null);
        }

        if (lockFlags == null)
        {
            switch (mode)
            {
                case INFO:
                case REMOVE:
                case UPDATE_ACCESS:
                    i18n.send(ChatType.ACTION_BAR, player, MessageType.NEUTRAL, "Target is not locked");
                    return true;
                case TRUST:
                    if (dataHolder instanceof ServerPlayer)
                    {
                        // TODO set global trust of player
                    }
                    else
                    {
                        // TODO open book to modify global trust
                    }
                    return false;
                case INFO_CREATE:
                    if (dataHolder == null)
                    {
                        i18n.send(ChatType.ACTION_BAR, player, MessageType.NEUTRAL, "Target is not lockable");
                        return true;
                    }
                    // TODO return for unprotectable stuff?
            }
        }

        switch (mode)
        {
            case INFO: // Fallthrough...
            case INFO_CREATE:
                if (lockFlags == null) {
                    lockerManager.createLock(dataHolder, player, itemInHand);
                    return true;
                }
                lockerManager.showLock(dataHolder, player);
                return true;
            case REMOVE:
                lockerManager.removeLock(dataHolder, player);
                return true;
            case UPDATE_FLAGS:
                lockerManager.updateLockFlags(dataHolder, player, itemInHand);
                return true;
            case UPDATE_ACCESS:
                lockerManager.updateLockAccess(dataHolder, player, itemInHand);
                return true;
        }

        return false;
    }



}