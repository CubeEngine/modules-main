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

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.command.parser.EnchantmentParser.getPossibleEnchantments;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.item.ItemTypes.SKULL;
import static org.spongepowered.api.text.serializer.TextSerializers.FORMATTING_CODE;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.data.property.item.UseLimitProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * <p>/rename
 * <p>/headchange
 * <p>/enchant
 * <p>/repair
 */
public class ItemModifyCommands extends PermissionContainer
{
    private I18n i18n;

    public ItemModifyCommands(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(desc = "Changes the display name of the item in your hand.")
    @Restricted(value = Player.class, msg = "Trying to give your {text:toys} a name?")
    public void rename(Player context, String name, @Optional @Greed(INFINITE) String... lore)
    {
        if (!context.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            i18n.send(context, NEGATIVE, "You need to hold an item to rename in your hand!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND).get();

        item.offer(Keys.DISPLAY_NAME, FORMATTING_CODE.deserialize(name));
        if (lore != null)
        {
            List<Text> list = new ArrayList<>();
            for (String line : lore)
            {
                list.add(FORMATTING_CODE.deserialize(line));
            }
            item.offer(Keys.ITEM_LORE, list);
        }
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You now hold {input#name} in your hands!", name);
    }

    @Command(desc = "Changes the lore of the item in your hand.")
    @Restricted(value = Player.class)
    public void lore(Player context, @Greed(INFINITE) String... lore)
    {
        if (!context.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            i18n.send(context, NEGATIVE, "You need to hold an item to change the lore of in your hand!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND).get();

        List<Text> list = new ArrayList<>();
        for (String line : lore)
        {
            list.add(FORMATTING_CODE.deserialize(line));
        }
        item.offer(Keys.ITEM_LORE, list);
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You changed the lore.");
    }


    @Command(alias = "skullchange", desc = "Changes a skull to a players skin.")
    @Restricted(value = Player.class, msg = "This will you only give headaches!")
    public void headchange(Player context, @Optional String name) throws ExecutionException, InterruptedException
    {
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        if (item == null || item.getItem() != SKULL)
        {
            i18n.send(context, NEGATIVE, "You are not holding a head.");
            return;
        }

        item.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        item.offer(Keys.REPRESENTED_PLAYER, Sponge.getServer().getGameProfileManager().get(name).get());

        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You now hold {user}'s head in your hands!", name);
    }

    @Command(desc = "Adds an Enchantment to the item in your hand")
    @Restricted(value = Player.class, msg = "Want to be Harry Potter?")
    public void enchant(Player context, @Default Enchantment enchantment, @Optional Integer level,
                        @ParameterPermission @Flag boolean unsafe) // TODO are param permissions working????
    {
        if (!context.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            i18n.send(context, NEUTRAL, "{text:ProTip}: You cannot enchant your fists!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND).get();

        level = level == null ? enchantment.getMaximumLevel() : level;
        if (level <= 0)
        {
            i18n.send(context, NEGATIVE, "The enchantment level has to be a number greater than 0!");
            return;
        }
        ItemEnchantment ench = new ItemEnchantment(enchantment, level);

        if (unsafe)
        {
            if (item.get(Keys.STORED_ENCHANTMENTS).isPresent())
            {
                List<ItemEnchantment> list = item.get(Keys.STORED_ENCHANTMENTS).get();
                list.add(ench);
                item.offer(Keys.STORED_ENCHANTMENTS, list);
                return;
            }

            List<ItemEnchantment> list = item.getOrElse(Keys.ITEM_ENCHANTMENTS, new ArrayList<>());
            list.add(ench);
            item.offer(Keys.ITEM_ENCHANTMENTS, list);
            context.setItemInHand(HandTypes.MAIN_HAND, item);
            i18n.send(context, POSITIVE,
                                   "Added unsafe enchantment: {input#enchantment} {integer#level} to your item!",
                                   enchantment.getName(), level); // TODO getTranslation
            return;
        }

        if (enchantment.canBeAppliedToStack(item))
        {
            if (level >= enchantment.getMinimumLevel() && level <= enchantment.getMaximumLevel())
            {
                List<ItemEnchantment> list = item.getOrElse(Keys.ITEM_ENCHANTMENTS, new ArrayList<>());
                list.add(ench);
                item.offer(Keys.ITEM_ENCHANTMENTS, list);
                context.setItemInHand(HandTypes.MAIN_HAND, item);
                i18n.send(context, POSITIVE, "Added enchantment: {input#enchantment} {integer#level} to your item!",
                                    enchantment.getName(), level);  // TODO getTranslation
                return;
            }
            i18n.send(context, NEGATIVE, "This enchantment level is not allowed!");
            return;
        }
        Text possibleEnchs = getPossibleEnchantments(item);
        if (possibleEnchs != null)
        {
            i18n.send(context, NEGATIVE, "This enchantment is not allowed for this item!", possibleEnchs);
            i18n.send(context, NEUTRAL, "Try one of those instead:");
            context.sendMessage(possibleEnchs);
            return;
        }
        i18n.send(context, NEGATIVE, "You can not enchant this item!");
    }

    @Command(desc = "Toggles the visibility of enchantments")
    @Restricted(value = Player.class)
    public void hideEnchantments(Player context, @Optional Boolean hide)
    {
        java.util.Optional<ItemStack> item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (!item.isPresent())
        {
            i18n.send(context, NEGATIVE, "No item in hand!");
            return;
        }
        hide = hide == null ? true : hide;
        item.get().offer(Keys.HIDE_ENCHANTMENTS, hide);
        context.setItemInHand(HandTypes.MAIN_HAND, item.get());
        if (hide)
        {
            i18n.send(context, POSITIVE, "Enchantments are hidden on this item.");
            return;
        }
        i18n.send(context, POSITIVE, "Enchantments are visible on this item.");
    }

    @Command(desc = "Repairs your items")
    @Restricted(value = Player.class, msg = "If you do this you'll loose your warranty!")
    public void repair(Player context, @Flag boolean all)
    {
        if (all)
        {
            int repaired = 0;
            for (Inventory slot : context.getInventory().slots())
            {
                if (slot.peek().isPresent())
                {
                    ItemStack item = slot.peek().get();
                    if (item.supports(DurabilityData.class)) // TODO mod-items that use Durability for different types
                    {
                        Integer max = item.getProperty(UseLimitProperty.class).get().getValue();
                        if (!max.equals(item.get(Keys.ITEM_DURABILITY).orElse(0)))
                        {
                            repaired++;
                        }
                        item.offer(Keys.ITEM_DURABILITY, max);
                    }
                    slot.set(item);
                }
            }
            if (repaired == 0)
            {
                i18n.send(context, NEUTRAL, "No items to repair!");
                return;
            }
            i18n.send(context, POSITIVE, "Repaired {amount} items!", repaired);
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        if (item == null)
        {
            i18n.send(context, NEGATIVE, "No item in hand!");
            return;
        }
        if (item.supports(DurabilityData.class))
        {
            Integer max = item.getProperty(UseLimitProperty.class).get().getValue();
            if (item.get(Keys.ITEM_DURABILITY).get().equals(max))
            {
                i18n.send(context, NEUTRAL, "No need to repair this!");
                return;
            }
            item.offer(Keys.ITEM_DURABILITY, max);
            context.setItemInHand(HandTypes.MAIN_HAND, item);
            i18n.send(context, POSITIVE, "Item repaired!");
            return;
        }
        i18n.send(context, NEUTRAL, "Item cannot be repaired!");
    }
}
