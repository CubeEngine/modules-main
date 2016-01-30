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
package org.cubeengine.module.vanillaplus.addition;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.butler.parameter.FixedValues;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.module.core.util.matcher.EnchantMatcher;
import org.cubeengine.module.core.util.matcher.MaterialMatcher;
import org.cubeengine.service.command.CommandContext;
import de.cubeisland.engine.service.command.result.paginated.PaginatedResult;
import org.cubeengine.service.paginate.PaginatedResult;
import org.cubeengine.service.user.User;
import de.cubeisland.engine.module.core.util.matcher.Match;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.basics.BasicsAttachment;
import org.bukkit.enchantments.Enchantment;
import org.spongepowered.api.item.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.spongepowered.api.Game;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
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
public class ItemDBCommand
{
    private final Basics module;
    private MaterialMatcher materialMatcher;
    private EnchantMatcher enchantMatcher;
    private Game game;

    public ItemDBCommand(Basics module, MaterialMatcher materialMatcher, EnchantMatcher enchantMatcher, Game game)
    {
        this.module = module;
        this.materialMatcher = materialMatcher;
        this.enchantMatcher = enchantMatcher;
        this.game = game;
    }

    @Command(desc = "Looks up an item for you!")
    @SuppressWarnings("deprecation")
    public PaginatedResult itemDB(CommandContext context, @Optional String item)
    {
        if (item != null)
        {
            List<ItemStack> itemList = materialMatcher.itemStackList(item);
            if (itemList == null || itemList.size() <= 0)
            {
                context.sendTranslated(NEGATIVE, "Could not find any item named {input}!", item);
                return null;
            }
            List<Text> lines = new ArrayList<>();
            ItemStack key = itemList.get(0);
            lines.add(Texts.of(context.getSource().getTranslation(POSITIVE,
                                                                   "Best Matched {input#item} ({integer#id} for {input}",
                                                                   materialMatcher.getNameFor(key),
                                                                   key.getItem().getId(), item)));
            itemList.remove(0);
            for (ItemStack stack : itemList) {
                lines.add(Texts.of(context.getSource().getTranslation(POSITIVE,
                                                                      "Matched {input#item} ({integer#id} for {input}",
                                                                      materialMatcher.getNameFor(stack),
                                                                      stack.getItem().getId(), item)));
            }
            return new PaginatedResult(context, lines);
        }
        if (!context.isSource(User.class))
        {
            throw new TooFewArgumentsException();
        }
        User sender = (User)context.getSource();
        if (!sender.getItemInHand().isPresent())
        {
            context.sendTranslated(NEUTRAL, "You hold nothing in your hands!");
            return null;
        }
        ItemStack aItem = sender.getItemInHand().get();
        String found = materialMatcher.getNameFor(aItem);
        if (found == null)
        {
            context.sendTranslated(NEGATIVE, "Itemname unknown! Itemdata: {integer#id}",
                                   aItem.getItem().getId());
            return null;
        }
        context.sendTranslated(POSITIVE, "The Item in your hand is: {input#item} ({integer#id})",
                               found, aItem.getItem().getId());
        return null;
    }



}
