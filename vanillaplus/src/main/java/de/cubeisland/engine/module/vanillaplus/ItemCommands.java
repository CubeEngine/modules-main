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
package de.cubeisland.engine.module.vanillaplus;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parameter.FixedValues;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.matcher.EnchantMatcher;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.paginate.PaginatedResult;
import de.cubeisland.engine.module.service.user.User;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.data.manipulator.SkullData;
import org.spongepowered.api.data.manipulator.item.DurabilityData;
import org.spongepowered.api.data.manipulator.item.LoreData;
import org.spongepowered.api.data.property.UseLimitProperty;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.service.command.readers.EnchantmentReader.getPossibleEnchantments;
import static org.spongepowered.api.item.ItemTypes.SKULL;
import static org.spongepowered.api.item.inventory.ItemStackComparators.ITEM_DATA;
import static org.spongepowered.api.item.inventory.ItemStackComparators.TYPE;

/**
 * item-related commands
 * <p>/give
 * <p>/item
 * <p>/more
 * <p>/stack
 */
public class ItemCommands
{
    private final VanillaPlus module;
    private MaterialMatcher materialMatcher;
    private EnchantMatcher enchantMatcher;
    private Game game;

    public ItemCommands(VanillaPlus module, MaterialMatcher materialMatcher, EnchantMatcher enchantMatcher, Game game)
    {
        this.module = module;
        this.materialMatcher = materialMatcher;
        this.enchantMatcher = enchantMatcher;
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    @Command(desc = "Gives the specified Item to a player")
    public void give(CommandSender context, User player, @Label("material[:data]") ItemStack item, @Optional Integer amount, @Flag boolean blacklist)
    {
        if (!blacklist && module.perms().ITEM_BLACKLIST.isAuthorized(context)
            && this.module.getConfiguration().commands.itemBlacklist.contains(item)) // TODO
        {
            context.sendTranslated(NEGATIVE, "This item is blacklisted!");
            return;
        }
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            context.sendTranslated(NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }
        item.setQuantity(amount);
        if (player.getInventory().offer(item))
        {
            String matname = materialMatcher.getNameFor(item);
            context.sendTranslated(POSITIVE, "You gave {user} {amount} {input#item}!", player, amount, matname);
            player.sendTranslated(POSITIVE, "{user} just gave you {amount} {input#item}!", context.getName(), amount, matname);
            return;
        }
        player.sendTranslated(NEGATIVE, "{user} had no place for the item.");
    }

    @Command(alias = "i", desc = "Gives the specified Item to you")
    @Restricted(value = User.class, msg = "Did you try to use {text:/give} on your new I-Tem?")
    @SuppressWarnings("deprecation")
    public void item(User context, @Label("material[:data]") ItemStack item,
                     @Optional Integer amount,
                     @Named("ench") @Label("enchantment[:level]") String enchantmentString,
                     @Flag boolean blacklist)
    {
        if (!blacklist && module.perms().ITEM_BLACKLIST.isAuthorized(context) && this.module.getConfiguration().commands.containsBlackListed(item.getItem()))
        {
            context.sendTranslated(NEGATIVE, "This item is blacklisted!");
            return;
        }
        amount = amount == null ? item.getMaxStackQuantity() : amount;
        if (amount <= 0)
        {
            context.sendTranslated(NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }

        if (enchantmentString != null)
        {
            String[] enchs = StringUtils.explode(",", enchantmentString);
            for (String ench : enchs)
            {
                int enchLvl = 0;
                if (ench.contains(":"))
                {
                    enchLvl = Integer.parseInt(ench.substring(ench.indexOf(":") + 1, ench.length()));
                    ench = ench.substring(0, ench.indexOf(":"));
                }
                if (module.perms().COMMAND_ITEM_ENCHANTMENTS.isAuthorized(context))
                {
                    if (module.perms().COMMAND_ITEM_ENCHANTMENTS_UNSAFE.isAuthorized(context))
                    {
                        enchantMatcher.applyMatchedEnchantment(item, ench, enchLvl, true);
                    }
                    else
                    {
                        enchantMatcher.applyMatchedEnchantment(item, ench, enchLvl, false);
                    }
                }
            }
        }
        item.setQuantity(amount);
        context.getInventory().offer(item);
        context.sendTranslated(NEUTRAL, "Received: {amount} {input#item}", amount, materialMatcher.getNameFor(item));
    }

    @Command(desc = "Refills the stack in hand")
    @Restricted(value = User.class, msg = "You can't get enough of it, can you?")
    public void more(User context, @Optional Integer amount, @Flag boolean all) // TODO staticvalues staticValues = "*",
    {
        if (all)
        {
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    item.setQuantity(64);
                }
            }
            context.sendTranslated(POSITIVE, "Refilled all stacks!");
            return;
        }
        amount = amount == null ? 1 : amount;
        if (amount < 1)
        {
            context.sendTranslated(NEGATIVE, "Invalid amount {input#amount}", amount);
            return;
        }

        if (!context.getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "More nothing is still nothing!");
            return;
        }
        context.getItemInHand().get().setQuantity(64);
        if (amount == 1)
        {
            context.sendTranslated(POSITIVE, "Refilled stack in hand!");
            return;
        }
        for (int i = 1; i < amount; ++i)
        {
            context.getInventory().offer(context.getItemInHand().get());
        }
        context.sendTranslated(POSITIVE, "Refilled {amount} stacks in hand!", amount);
    }



    @Command(desc = "Stacks your items up to 64")
    @Restricted(value = User.class, msg = "No stacking for you.")
    public void stack(User context)
    {
        boolean allow64 = module.perms().COMMAND_STACK_FULLSTACK.isAuthorized(context);
        ItemStack[] items = new ItemStack[context.getInventory().capacity()];
        int slotIndex = 0;
        for (Inventory slot : context.getInventory().slots())
        {
            items[slotIndex] = slot.peek().orNull();
        }

        int size = items.length;
        boolean changed = false;
        for (int i = 0; i < size; i++)
        {
            ItemStack item = items[i];
            // no null / infinite or unstackable items (if not allowed)
            if (item == null || item.getQuantity() <= 0 || (!allow64 && item.getMaxStackQuantity() == 1))
            {
                continue;
            }
            int max = allow64 ? 64 : item.getMaxStackQuantity();
            if (item.getQuantity() < max)
            {
                int needed = max - item.getQuantity();
                for (int j = i + 1; j < size; j++) // search for same item
                {
                    ItemStack item2 = items[j];
                    // no null / infinite or unstackable items (if not allowed)
                    if (item2 == null || item2.getQuantity() <= 0 || (!allow64 && item.getMaxStackQuantity() == 1))
                    {
                        continue;
                    }
                    // compare
                    if (TYPE.compare(item, item2) == 0 && ITEM_DATA.compare(item, item2) == 0)
                    {
                        if (item2.getQuantity() > needed) // not enough place -> fill up stack
                        {
                            item.setQuantity(max);
                            item2.setQuantity(item2.getQuantity() - needed);
                            break;
                        }
                        // enough place -> add to stack
                        {
                            items[j] = null;
                            item.setQuantity(item.getQuantity() + item2.getQuantity());
                            needed = max - item.getQuantity();
                        }
                        changed = true;
                    }
                }
            }
        }
        if (changed)
        {
            int i = 0;
            for (Inventory slot : context.getInventory().slots())
            {
                slot.set(items[i++]);
            }
            context.sendTranslated(POSITIVE, "Items stacked together!");
            return;
        }
        context.sendTranslated(NEUTRAL, "Nothing to stack!");
    }
}
