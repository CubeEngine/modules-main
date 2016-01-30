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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.ArrayList;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.service.user.User;
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

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.command.readers.EnchantmentReader.getPossibleEnchantments;
import static org.spongepowered.api.item.ItemTypes.SKULL;

/**
 * <p>/rename
 * <p>/headchange
 * <p>/enchant
 * <p>/repair
 */
public class ItemModifyCommands
{
    @Command(desc = "Changes the display name of the item in your hand.")
    @Restricted(value = User.class, msg = "Trying to give your {text:toys} a name?")
    public void rename(User context, String name, @Optional @Greed(INFINITE) String... lore)
    {
        // TODO lore cmd
        if (!context.getItemInHand().isPresent())
        {
            context.sendTranslated(NEGATIVE, "You need to hold an item to rename in your hand!");
            return;
        }
        ItemStack item = context.getItemInHand().get();

        @SuppressWarnings("deprecation") DisplayNameData data = item.getOrCreate(DisplayNameData.class).get();
        data.setDisplayName(Texts.fromLegacy(name, '&'));
        item.offer(data);

        if (lore != null)
        {
            LoreData loreData = item.getOrCreate(LoreData.class).get();
            ArrayList<Text> list = new ArrayList<>();
            for (String line : lore)
            {
                list.add(Texts.fromLegacy(line, '&'));
            }
            loreData.set(list);
            item.offer(loreData);
        }
        context.sendTranslated(POSITIVE, "You now hold {input#name} in your hands!", name);
    }

    @Command(alias = "skullchange", desc = "Changes a skull to a players skin.")
    @Restricted(value = User.class, msg = "This will you only give headaches!")
    public void headchange(User context, @Optional String name)
    {
        ItemStack item = context.getItemInHand().orNull();
        if (item == null || item.getItem() != SKULL)
        {
            context.sendTranslated(NEGATIVE, "You are not holding a head.");
            return;
        }
        SkullData data = item.getOrCreate(SkullData.class).get();
        data.setValue(SkullTypes.PLAYER);
        item.offer(data);

        // TODO actually set head type
        context.sendTranslated(POSITIVE, "You now hold {user}'s head in your hands!", name);
    }

    @Command(desc = "Adds an Enchantment to the item in your hand")
    @Restricted(value = User.class, msg = "Want to be Harry Potter?")
    public void enchant(User context, @Default Enchantment enchantment, @Optional Integer level, @Flag boolean unsafe)
    {
        if (!context.getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "{text:ProTip}: You cannot enchant your fists!");
            return;
        }
        ItemStack item = context.getItemInHand().get();

        level = level == null ? enchantment.getMaximumLevel() : level;
        if (level <= 0)
        {
            context.sendTranslated(NEGATIVE, "The enchantment level has to be a number greater than 0!");
            return;
        }
        if (unsafe)
        {
            if (!module.perms().COMMAND_ENCHANT_UNSAFE.isAuthorized(context))
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
            // TODO enchant item event when sponge event is not only for enchanting via table #WaitForBukkit
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
                context.sendTranslated(POSITIVE, "Added enchantment: {input#enchantment} {integer#level} to your item!",
                                       Match.enchant().nameFor(enchantment), level);
                return;
            }
            context.sendTranslated(NEGATIVE, "This enchantment level is not allowed!");
            return;
        }
        String possibleEnchs = getPossibleEnchantments(game.getRegistry(), item);
        if (possibleEnchs != null)
        {
            context.sendTranslated(NEGATIVE, "This enchantment is not allowed for this item!", possibleEnchs);
            context.sendTranslated(NEUTRAL, "Try one of those instead:");
            context.sendMessage(possibleEnchs);
            return;
        }
        context.sendTranslated(NEGATIVE, "You can not enchant this item!");
    }

    @Command(desc = "Repairs your items")
    @Restricted(value = User.class, msg = "If you do this you'll loose your warranty!")
    public void repair(User context, @Flag boolean all)
    {
        if (all)
        {
            int repaired = 0;
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    if (item.isCompatible(DurabilityData.class))
                    {
                        Integer max = item.getProperty(UseLimitProperty.class).get().getValue();
                        item.offer(item.getOrCreate(DurabilityData.class).get().setDurability(max));
                        repaired++;
                    }
                }
            }
            if (repaired == 0)
            {
                context.sendTranslated(NEUTRAL, "No items to repair!");
                return;
            }
            context.sendTranslated(POSITIVE, "Repaired {amount} items!", repaired);
            return;
        }
        ItemStack item = context.getItemInHand().get();
        if (item.isCompatible(DurabilityData.class))
        {
            Integer max = item.getProperty(UseLimitProperty.class).get().getValue();
            if (item.getOrCreate(DurabilityData.class).get().getDurability() == max)
            {
                context.sendTranslated(NEUTRAL, "No need to repair this!");
                return;
            }
            item.offer(item.getOrCreate(DurabilityData.class).get().setDurability(max));
            context.sendTranslated(POSITIVE, "Item repaired!");
            return;
        }
        context.sendTranslated(NEUTRAL, "Item cannot be repaired!");
    }
}
