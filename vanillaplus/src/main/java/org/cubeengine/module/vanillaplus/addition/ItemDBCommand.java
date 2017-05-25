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
package org.cubeengine.module.vanillaplus.addition;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EnchantMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

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
public class ItemDBCommand
{
    private final VanillaPlus module;
    private MaterialMatcher materialMatcher;
    private EnchantMatcher enchantMatcher;
    private I18n i18n;

    public ItemDBCommand(VanillaPlus module, MaterialMatcher materialMatcher, EnchantMatcher enchantMatcher, I18n i18n)
    {
        this.module = module;
        this.materialMatcher = materialMatcher;
        this.enchantMatcher = enchantMatcher;
        this.i18n = i18n;
    }

    @Command(desc = "Looks up an item for you!")
    @SuppressWarnings("deprecation")
    public void itemDB(CommandSource context, @Optional String item)
    {
        if (item != null)
        {
            List<ItemStack> itemList = materialMatcher.itemStackList(item);
            if (itemList == null || itemList.size() <= 0)
            {
                i18n.sendTranslated(context, NEGATIVE, "Could not find any item named {input}!", item);
                return;
            }
            List<Text> lines = new ArrayList<>();
            ItemStack key = itemList.get(0);
            lines.add(i18n.getTranslation(context, POSITIVE, "Best Matched {input#item} {input#id} for {input}",
                                          materialMatcher.getNameFor(key), key.getItem().getId(), item));
            itemList.remove(0);
            for (ItemStack stack : itemList)
            {
                lines.add(i18n.getTranslation(context, POSITIVE, "Matched {input#item} {input#id} for {input}",
                                              materialMatcher.getNameFor(stack), stack.getItem().getId(), item));
            }
            Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder()
                .contents(lines).sendTo(context);
            return;
        }
        if (!(context instanceof Player))
        {
            throw new TooFewArgumentsException();
        }
        Player sender = (Player)context;
        if (!sender.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "You hold nothing in your hands!");
            return;
        }
        ItemStack aItem = sender.getItemInHand(HandTypes.MAIN_HAND).get();
        String found = materialMatcher.getNameFor(aItem);
        if (found == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Itemname unknown! Itemdata: {integer#id}", aItem.getItem().getId());
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The Item in your hand is: {input#item} ({integer#id})", found, aItem.getItem().getId());
    }
}
