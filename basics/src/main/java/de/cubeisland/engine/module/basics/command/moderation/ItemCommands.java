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
package de.cubeisland.engine.module.basics.command.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.command.parameter.FixedValues;
import de.cubeisland.engine.core.command.readers.EnchantmentReader;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.parameter.TooFewArgumentsException;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.result.paginated.PaginatedResult;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static org.bukkit.Material.AIR;
import static org.bukkit.Material.SKULL_ITEM;

/**
 * item-related commands
 * <p>/itemdb
 * <p>/rename
 * <p>/headchange
 * <p>/unlimited
 * <p>/enchant
 * <p>/give
 * <p>/item
 * <p>/more
 * <p>/repair
 * <p>/stack
 */
public class ItemCommands
{
    private final Basics module;

    public ItemCommands(Basics module)
    {
        this.module = module;
    }

    @Command(desc = "Looks up an item for you!")
    @Params(positional = @Param(req = OPTIONAL, label = "item"))
    public PaginatedResult itemDB(CommandContext context)
    {
        if (context.hasPositional(0))
        {
            TreeSet<Entry<ItemStack, Double>> itemSet = Match.material().itemStackList(context.getString(0));
            if (itemSet != null && itemSet.size() > 0)
            {
                List<String> lines = new ArrayList<>();

                lines.add(context.getSource().getTranslation(POSITIVE, "Best Matched {input#item} ({integer#id}:{short#data}) for {input}", Match.material().getNameFor(itemSet.first().getKey()), itemSet.first().getKey().getType().getId(), itemSet.first().getKey().getDurability(), context.get(
                    0)));
                itemSet.remove(itemSet.first());
                for (Entry<ItemStack, Double> item : itemSet) {
                    lines.add(context.getSource().getTranslation(POSITIVE, "Matched {input#item} ({integer#id}:{short#data}) for {input}",
                                                                 Match.material().getNameFor(item.getKey()),
                                                                 item.getKey().getType().getId(),
                                                                 item.getKey().getDurability(), context.get(0)));
                }
                return new PaginatedResult(context, lines);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Could not find any item named {input}!", context.get(0));
            }
        }
        else if (context.isSource(User.class))
        {
            User sender = (User)context.getSource();
            if (sender.getItemInHand().getType() == AIR)
            {
                context.sendTranslated(NEUTRAL, "You hold nothing in your hands!");
                return null;
            }
            ItemStack item = sender.getItemInHand();
            String found = Match.material().getNameFor(item);
            if (found == null)
            {
                context.sendTranslated(NEGATIVE, "Itemname unknown! Itemdata: {integer#id}:{short#data}", item.getType().getId(), item.getDurability());
            }
            else
            {
                context.sendTranslated(POSITIVE, "The Item in your hand is: {input#item} ({integer#id}:{short#data})", found, item.getType().getId(), item.getDurability());
            }
        }
        else
        {
            throw new TooFewArgumentsException();
        }
        return null;
    }

    @Command(desc = "Changes the display name of the item in your hand.")
    @Params(positional = {@Param(label = "name"),
              @Param(req = OPTIONAL, label = "lore...", greed = INFINITE)})
    public void rename(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            ItemStack item = sender.getItemInHand();
            if (item == null || item.getType() == AIR)
            {
                context.sendTranslated(NEGATIVE, "You need to hold an item to rename in your hand!");
                return;
            }
            ItemMeta meta = item.getItemMeta();
            String name = ChatFormat.parseFormats(context.getString(0));
            meta.setDisplayName(name);
            ArrayList<String> list = new ArrayList<>();
            for (int i = 1; i < context.getPositionalCount(); ++i)
            {
                list.add(ChatFormat.parseFormats(context.getString(i)));
            }
            meta.setLore(list);
            item.setItemMeta(meta);
            context.sendTranslated(POSITIVE, "You now hold {input#name} in your hands!", name);
            return;
        }
        context.sendTranslated(NEGATIVE, "Trying to give your {text:toys} a name?");
    }

    @Command(alias = "skullchange", desc = "Changes a skull to a players skin.")
    @Params(positional = @Param(req = OPTIONAL, label = "name"))
    @Restricted(value = User.class, msg = "This will you only give headaches!")
    public void headchange(CommandContext context)
    {
        User sender = (User)context.getSource();
        String name = context.get(0);
        if (sender.getItemInHand().getType() == SKULL_ITEM)
        {
            sender.getItemInHand().setDurability((short)3);
            SkullMeta meta = ((SkullMeta)sender.getItemInHand().getItemMeta());
            meta.setOwner(name);
            sender.getItemInHand().setItemMeta(meta);
            context.sendTranslated(POSITIVE, "You now hold {user}'s head in your hands!", name);
            return;
        }
        context.sendTranslated(NEGATIVE, "You are not holding a head.");
    }

    public enum OnOff implements FixedValues
    {
        ON(true), OFF(false);
        public final boolean value;

        OnOff(boolean value)
        {
            this.value = value;
        }

        @Override
        public String getName()
        {
            return this.name().toLowerCase();
        }
    }

    @Command(desc = "Grants unlimited items")
    @Params(positional = @Param(req = OPTIONAL, names = {"on","off"}))
    @Restricted(User.class)
    public void unlimited(CommandContext context, @Optional OnOff unlimited)
    {
        User sender = (User)context.getSource();
        boolean setTo = unlimited != null ? unlimited.value : !sender.get(BasicsAttachment.class).hasUnlimitedItems();
        if (setTo)
        {
            context.sendTranslated(POSITIVE, "You now have unlimited items to build!");
        }
        else
        {
            context.sendTranslated(NEUTRAL, "You no longer have unlimited items to build!");
        }
        sender.get(BasicsAttachment.class).setUnlimitedItems(setTo);
    }

    @Command(desc = "Adds an Enchantment to the item in your hand")
    @Restricted(value = User.class, msg = "Want to be Harry Potter?")
    public void enchant(CommandContext context, @Default Enchantment enchantment, @Optional Integer level, @Flag boolean unsafe)
    {
        User sender = (User)context.getSource();
        ItemStack item = sender.getItemInHand();
        if (item.getType() == AIR)
        {
            context.sendTranslated(NEUTRAL, "{text:ProTip}: You cannot enchant your fists!");
            return;
        }

        level = level == null ? enchantment.getMaxLevel() : level;
        if (level <= 0)
        {
            context.sendTranslated(NEGATIVE, "The enchantment level has to be a number greater than 0!");
            return;
        }
        if (unsafe)
        {
            if (!module.perms().COMMAND_ENCHANT_UNSAFE.isAuthorized(sender))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to add unsafe enchantments!");
                return;
            }
            if (item.getItemMeta() instanceof EnchantmentStorageMeta)
            {
                EnchantmentStorageMeta itemMeta = (EnchantmentStorageMeta)item.getItemMeta();
                itemMeta.addStoredEnchant(enchantment, level, true);
                item.setItemMeta(itemMeta);
                return;
            }
            // TODO enchant item event when bukkit event is not only for enchanting via table #WaitForBukkit
            item.addUnsafeEnchantment(enchantment, level);
            context.sendTranslated(POSITIVE,
                                   "Added unsafe enchantment: {input#enchantment} {integer#level} to your item!",
                                   Match.enchant().nameFor(enchantment), level);
            return;
        }
        if (enchantment.canEnchantItem(item))
        {
            if (level >= enchantment.getStartLevel() && level <= enchantment.getMaxLevel())
            {
                item.addUnsafeEnchantment(enchantment, level);
                context.sendTranslated(POSITIVE,
                                       "Added enchantment: {input#enchantment} {integer#level} to your item!", Match.enchant().nameFor(enchantment), level);
                return;
            }
            context.sendTranslated(NEGATIVE, "This enchantment level is not allowed!");
            return;
        }
        String possibleEnchs = EnchantmentReader.getPossibleEnchantments(item);
        if (possibleEnchs != null)
        {
            context.sendTranslated(NEGATIVE, "This enchantment is not allowed for this item!", possibleEnchs);
            context.sendTranslated(NEUTRAL, "Try one of those instead:");
            context.sendMessage(possibleEnchs);
            return;
        }
        context.sendTranslated(NEGATIVE, "You can not enchant this item!");
    }

    @SuppressWarnings("deprecation")
    @Command(desc = "Gives the specified Item to a player")
    public void give(CommandContext context, User player, @Label("material[:data]") ItemStack item, @Optional Integer amount, @Flag boolean blacklist)
    {
        if (!blacklist && module.perms().ITEM_BLACKLIST.isAuthorized(context.getSource())
            && this.module.getConfiguration().commands.itemBlacklist.contains(item))
        {
            context.sendTranslated(NEGATIVE, "This item is blacklisted!");
            return;
        }
        amount = amount == null ? item.getMaxStackSize() : amount;
        if (amount <= 0)
        {
            context.sendTranslated(NEGATIVE, "The amount has to be a number greater than 0!");
            return;
        }
        item.setAmount(amount);
        player.getInventory().addItem(item);
        player.updateInventory();
        String matname = Match.material().getNameFor(item);
        context.sendTranslated(POSITIVE, "You gave {user} {amount} {input#item}!", player, amount, matname);
        player.sendTranslated(POSITIVE, "{user} just gave you {amount} {input#item}!", context.getSource().getName(), amount, matname);
    }

    @Command(alias = "i", desc = "Gives the specified Item to you")
    @SuppressWarnings("deprecation")
    public void item(CommandContext context, @Label("material[:data]") ItemStack item,
                     @Optional Integer amount,
                     @Named("ench") @Label("enchantment[:level]") String enchantmentString,
                     @Flag boolean blacklist)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            if (!blacklist && module.perms().ITEM_BLACKLIST.isAuthorized(sender)
                    && this.module.getConfiguration().commands.containsBlackListed(item))
            {
                context.sendTranslated(NEGATIVE, "This item is blacklisted!");
                return;
            }
            amount = amount == null ? item.getMaxStackSize() : amount;
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
                    if (module.perms().COMMAND_ITEM_ENCHANTMENTS.isAuthorized(sender))
                    {
                        if (module.perms().COMMAND_ITEM_ENCHANTMENTS_UNSAFE.isAuthorized(sender))
                        {
                            Match.enchant().applyMatchedEnchantment(item, ench, enchLvl, true);
                        }
                        else
                        {
                            Match.enchant().applyMatchedEnchantment(item, ench, enchLvl, false);
                        }
                    }
                }
            }
            item.setAmount(amount);
            sender.getInventory().addItem(item);
            sender.updateInventory();
            sender.sendTranslated(NEUTRAL, "Received: {amount} {input#item}", amount, Match.material().getNameFor(item));
            return;
        }
        context.sendTranslated(NEUTRAL, "Did you try to use {text:/give} on your new I-Tem?");
    }

    @Command(desc = "Refills the stack in hand")
    public void more(CommandContext context, @Optional Integer amount, @Flag boolean all) // TODO staticvalues staticValues = "*",
    {
        if (!context.isSource(User.class))
        {
            context.sendTranslated(NEGATIVE, "You can't get enough of it, can you?");
            return;
        }
        User sender = (User)context.getSource();
        if (all)
        {
            for (ItemStack item : sender.getInventory().getContents())
            {
                if (item.getType() != AIR)
                {
                    item.setAmount(64);
                }
            }
            sender.sendTranslated(POSITIVE, "Refilled all stacks!");
            return;
        }
        amount = amount == null ? 1 : amount;
        if (amount < 1)
        {
            context.sendTranslated(NEGATIVE, "Invalid amount {input#amount}", amount);
            return;
        }
        if (sender.getItemInHand() == null || sender.getItemInHand().getType() == AIR)
        {
            context.sendTranslated(NEUTRAL, "More nothing is still nothing!");
            return;
        }
        sender.getItemInHand().setAmount(64);
        if (amount == 1)
        {
            sender.sendTranslated(POSITIVE, "Refilled stack in hand!");
            return;
        }
        for (int i = 1; i < amount; ++i)
        {
            sender.getInventory().addItem(sender.getItemInHand());
        }
        sender.sendTranslated(POSITIVE, "Refilled {amount} stacks in hand!", amount);
    }

    @Command(desc = "Repairs your items")
    @Restricted(value = User.class, msg = "If you do this you'll loose your warranty!")
    public void repair(CommandContext context, @Flag boolean all)
    {
        User sender = (User)context.getSource();
        if (all)
        {
            List<ItemStack> list = new ArrayList<>();
            list.addAll(Arrays.asList(sender.getInventory().getArmorContents()));
            list.addAll(Arrays.asList(sender.getInventory().getContents()));
            int repaired = 0;
            for (ItemStack item : list)
            {
                if (Match.material().repairable(item))
                {
                    item.setDurability((short)0);
                    repaired++;
                }
            }
            if (repaired == 0)
            {
                sender.sendTranslated(NEUTRAL, "No items to repair!");
                return;
            }
            sender.sendTranslated(POSITIVE, "Repaired {amount} items!", repaired);
            return;
        }
        ItemStack item = sender.getItemInHand();
        if (Match.material().repairable(item))
        {
            if (item.getDurability() == 0)
            {
                sender.sendTranslated(NEUTRAL, "No need to repair this!");
                return;
            }
            item.setDurability((short)0);
            sender.sendTranslated(POSITIVE, "Item repaired!");
            return;
        }
        sender.sendTranslated(NEUTRAL, "Item cannot be repaired!");
    }

    @Command(desc = "Stacks your items up to 64")
    @Restricted(value = User.class, msg = "No stacking for you.")
    public void stack(CommandContext context)
    {
        User user = (User)context.getSource();
        boolean allow64 = module.perms().COMMAND_STACK_FULLSTACK.isAuthorized(user);
        ItemStack[] items = user.getInventory().getContents();
        int size = items.length;
        boolean changed = false;
        for (int i = 0; i < size; i++)
        {
            ItemStack item = items[i];
            // no null / infinite or unstackable items (if not allowed)
            if (item == null || item.getAmount() <= 0 || (!allow64 && item.getMaxStackSize() == 1))
            {
                continue;
            }
            int max = allow64 ? 64 : item.getMaxStackSize();
            if (item.getAmount() < max)
            {
                int needed = max - item.getAmount();
                for (int j = i + 1; j < size; j++) // search for same item
                {
                    ItemStack item2 = items[j];
                    // no null / infinite or unstackable items (if not allowed)
                    if (item2 == null || item2.getAmount() <= 0 || (!allow64 && item.getMaxStackSize() == 1))
                    {
                        continue;
                    }
                    // compare
                    if (item.isSimilar(item2))
                    {
                        if (item2.getAmount() > needed) // not enough place -> fill up stack
                        {
                            item.setAmount(max);
                            item2.setAmount(item2.getAmount() - needed);
                            break;
                        }
                        // enough place -> add to stack
                        {
                            items[j] = null;
                            item.setAmount(item.getAmount() + item2.getAmount());
                            needed = max - item.getAmount();
                        }
                        changed = true;
                    }
                }
            }
        }
        if (changed)
        {
            user.getInventory().setContents(items);
            user.sendTranslated(POSITIVE, "Items stacked together!");
            return;
        }
        user.sendTranslated(NEUTRAL, "Nothing to stack!");
    }
}
