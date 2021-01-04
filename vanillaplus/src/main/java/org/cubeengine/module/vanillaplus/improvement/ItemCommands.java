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
package org.cubeengine.module.vanillaplus.improvement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult.Type;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * item-related commands
 * <p>/give
 * <p>/item
 * <p>/more
 * <p>/stack
 */
@Singleton
public class ItemCommands extends PermissionContainer
{
    private I18n i18n;

    @Inject
    public ItemCommands(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(desc = "Gives the specified Item to a player")
    public void give(CommandCause context, User player, ItemStackSnapshot itemstack, @Option Integer amount)
    {
        final ItemStack item = itemstack.createStack();
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            i18n.send(context, NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }
        item.setQuantity(amount);
        final Inventory inventory = getHotbarFirst(player.isOnline() ? player.getPlayer().get().getInventory() : player.getInventory());
        if (inventory.offer(item.copy()).getType() == Type.SUCCESS)
        {
            Component matname = item.get(Keys.DISPLAY_NAME).get().color(NamedTextColor.GOLD);
            i18n.send(context, POSITIVE, "You gave {user} {amount} {name#item}!", player, amount, matname);
            if (player.isOnline())
            {
                i18n.send(player.getPlayer().get(), POSITIVE, "{user} just gave you {amount} {name#item}!",
                          context.getSubject().getFriendlyIdentifier().orElse(context.getSubject().getIdentifier()), amount, matname);
            }
            return;
        }
        i18n.send(context, NEGATIVE, "{user} had no place for the item.");
    }

    @Command(alias = "i", desc = "Gives the specified Item to you")
    @Restricted(msg = "Did you try to use {text:/give} on your new I-Tem?")
    public void item(ServerPlayer context, ItemStackSnapshot itemstack, @Option Integer amount)
    {
        final ItemStack item = itemstack.createStack();
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            i18n.send(context, NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }
        item.setQuantity(amount);
        Inventory hotbarFirstInventory = getHotbarFirst(context.getInventory());
        if (!hotbarFirstInventory.offer(item.copy()).getRejectedItems().isEmpty())
        {
            i18n.send(context, NEGATIVE, "Not enough space for the item!");
            return;
        }
        i18n.send(context, NEUTRAL, "Received: {amount} {text#item}", amount, item.get(Keys.CUSTOM_NAME).get());
    }

    public Inventory getHotbarFirst(Inventory inventory)
    {
        return inventory.query(QueryTypes.PLAYER_PRIMARY_HOTBAR_FIRST.get().toQuery());
    }

    @Command(desc = "Refills the stack in hand")
    @Restricted(msg = "You can't get enough of it, can you?")
    public void more(ServerPlayer context, @Option Integer amount, @Flag boolean all)
    {
        if (all)
        {
            for (Slot slot : context.getInventory().slots())
            {
                if (!slot.peek().isEmpty())
                {
                    ItemStack item = slot.peek();
                    item.setQuantity(64);
                    slot.set(item);
                }
            }
            i18n.send(context, POSITIVE, "Refilled all stacks!");
            return;
        }
        amount = amount == null ? 1 : amount;
        if (amount < 1)
        {
            i18n.send(context, NEGATIVE, "Invalid amount {input#amount}", amount);
            return;
        }

        final ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (item.isEmpty())
        {
            i18n.send(context, NEUTRAL, "More nothing is still nothing!");
            return;
        }
        item.setQuantity(item.getMaxStackQuantity());
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        if (amount == 1)
        {
            i18n.send(context, POSITIVE, "Refilled stack in hand!");
            return;
        }
        for (int i = 1; i < amount; ++i)
        {
            context.getInventory().offer(item.copy());
        }
        i18n.send(context, POSITIVE, "Refilled {amount} stacks in hand!", amount);
    }

}
