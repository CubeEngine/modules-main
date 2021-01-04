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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.registry.RegistryTypes;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * <p>/rename
 * <p>/headchange
 * <p>/enchant
 * <p>/repair
 */
@Singleton
public class ItemModifyCommands extends PermissionContainer
{
    private I18n i18n;

    @Inject
    public ItemModifyCommands(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(desc = "Changes the display name of the item in your hand.")
    @Restricted(msg = "Trying to give your {text:toys} a name?")
    public void rename(ServerPlayer context, String name, @Option @Greedy List<String> lore)
    {
        if (context.getItemInHand(HandTypes.MAIN_HAND).isEmpty())
        {
            i18n.send(context, NEGATIVE, "You need to hold an item to rename in your hand!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);

        item.offer(Keys.CUSTOM_NAME, LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        if (lore != null)
        {
            List<Component> list = new ArrayList<>();
            for (String line : lore)
            {
                list.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            item.offer(Keys.LORE, list);
        }
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You now hold {input#name} in your hands!", name);
    }

    @Command(desc = "Changes the lore of the item in your hand.")
    @Restricted
    public void lore(ServerPlayer context, @Greedy List<String> lore)
    {
        if (context.getItemInHand(HandTypes.MAIN_HAND).isEmpty())
        {
            i18n.send(context, NEGATIVE, "You need to hold an item to change the lore of in your hand!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);

        List<Component> list = new ArrayList<>();
        for (String line : lore)
        {
            list.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
        item.offer(Keys.LORE, list);
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You changed the lore.");
    }


    @Command(alias = "skullchange", desc = "Changes a skull to a players skin.")
    @Restricted(msg = "This will you only give headaches!")
    public void headchange(ServerPlayer context, @Option String name) throws ExecutionException, InterruptedException
    {
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (item.isEmpty() || item.getType() != ItemTypes.PLAYER_HEAD.get())
        {
            i18n.send(context, NEGATIVE, "You are not holding a head.");
            return;
        }

        item.offer(Keys.GAME_PROFILE, Sponge.getServer().getGameProfileManager().getProfile(name).get());

        context.setItemInHand(HandTypes.MAIN_HAND, item);
        i18n.send(context, POSITIVE, "You now hold {user}'s head in your hands!", name);
    }

    @Command(desc = "Adds an Enchantment to the item in your hand")
    @Restricted(msg = "Want to be Harry Potter?")
    public void enchant(ServerPlayer context, EnchantmentType enchantment, @Option Integer level,
                        @ParameterPermission @Flag boolean unsafe) // TODO are param permissions working????
    {
        if (context.getItemInHand(HandTypes.MAIN_HAND).isEmpty())
        {
            i18n.send(context, NEUTRAL, "{text:ProTip}: You cannot enchant your fists!");
            return;
        }
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);

        level = level == null ? enchantment.getMaximumLevel() : level;
        if (level <= 0)
        {
            i18n.send(context, NEGATIVE, "The enchantment level has to be a number greater than 0!");
            return;
        }
        Enchantment ench = Enchantment.builder().type(enchantment).level(level).build();

        if (unsafe)
        {
            if (item.get(Keys.STORED_ENCHANTMENTS).isPresent())
            {
                List<Enchantment> list = item.get(Keys.STORED_ENCHANTMENTS).get();
                list.add(ench);
                item.offer(Keys.STORED_ENCHANTMENTS, list);
                return;
            }

            List<Enchantment> list = item.getOrElse(Keys.APPLIED_ENCHANTMENTS, new ArrayList<>());
            list.add(ench);
            item.offer(Keys.APPLIED_ENCHANTMENTS, list);
            context.setItemInHand(HandTypes.MAIN_HAND, item);
            i18n.send(context, POSITIVE,
                                   "Added unsafe enchantment: {text#enchantment} {integer#level} to your item!",
                                   enchantment.asComponent(), level);
            return;
        }

        if (enchantment.canBeAppliedToStack(item))
        {
            if (level >= enchantment.getMinimumLevel() && level <= enchantment.getMaximumLevel())
            {
                List<Enchantment> list = item.getOrElse(Keys.APPLIED_ENCHANTMENTS, new ArrayList<>());
                list.add(ench);
                item.offer(Keys.APPLIED_ENCHANTMENTS, list);
                context.setItemInHand(HandTypes.MAIN_HAND, item);
                i18n.send(context, POSITIVE, "Added enchantment: {text#enchantment} {integer#level} to your item!",
                                    enchantment.asComponent(), level);  // TODO getTranslation
                return;
            }
            i18n.send(context, NEGATIVE, "This enchantment level is not allowed!");
            return;
        }
        final List<Component> possibleEnchantments = Sponge.getGame().registries().registry(RegistryTypes.ENCHANTMENT_TYPE).streamEntries()
                                                           .filter(e -> e.value().canBeAppliedToStack(item))
                                                           .map(enchantmentType -> enchantmentType.value().asComponent().color(NamedTextColor.YELLOW)
                                                                                      .hoverEvent(HoverEvent.showText(Component.text(enchantmentType.key().asString(), NamedTextColor.YELLOW))))
                                                           .collect(Collectors.toList());
        if (!possibleEnchantments.isEmpty())
        {
            i18n.send(context, NEGATIVE, "This enchantment is not allowed for this item!");
            i18n.send(context, NEUTRAL, "Try one of those instead:");
            context.sendMessage(Identity.nil(), Component.join(Component.text(", ", NamedTextColor.WHITE), possibleEnchantments));
            return;
        }
        i18n.send(context, NEGATIVE, "You can not enchant this item!");
    }

    @Command(desc = "Toggles the visibility of enchantments")
    @Restricted
    public void hideEnchantments(ServerPlayer context, @Option Boolean hide)
    {
        final ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (item.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No item in hand!");
            return;
        }
        hide = hide == null || hide;
        item.offer(Keys.HIDE_ENCHANTMENTS, hide);
        context.setItemInHand(HandTypes.MAIN_HAND, item);
        if (hide)
        {
            i18n.send(context, POSITIVE, "Enchantments are hidden on this item.");
            return;
        }
        i18n.send(context, POSITIVE, "Enchantments are visible on this item.");
    }

    @Command(desc = "Repairs your items")
    @Restricted(msg = "If you do this you'll loose your warranty!")
    public void repair(ServerPlayer context, @Flag boolean all)
    {
        if (all)
        {
            int repaired = 0;
            for (Slot slot : context.getInventory().slots())
            {
                if (!slot.peek().isEmpty())
                {
                    ItemStack item = slot.peek();
                    if (item.supports(Keys.ITEM_DURABILITY))
                    {
                        Integer max = item.get(Keys.MAX_DURABILITY).get();
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
        ItemStack item = context.getItemInHand(HandTypes.MAIN_HAND);
        if (item.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No item in hand!");
            return;
        }
        if (item.supports(Keys.ITEM_DURABILITY))
        {
            Integer max = item.get(Keys.ITEM_DURABILITY).get();
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
