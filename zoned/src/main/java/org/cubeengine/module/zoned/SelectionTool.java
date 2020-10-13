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
package org.cubeengine.module.zoned;

import static org.spongepowered.api.item.ItemTypes.COAL;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Arrays;

public class SelectionTool
{

    public static boolean inHand(Player player)
    {
        final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        return isTool(itemInHand);
    }

    public static boolean isTool(ItemStack stack)
    {
        return stack.get(ZonedData.ZONE_TYPE).isPresent();
    }

    public static ItemStack newTool(ServerPlayer player)
    {
        final ItemStack newTool = ItemStack.of(COAL);
        newTool.offer(ZonedData.ZONE_TYPE, "default");
        newTool.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.BINDING_CURSE, 1)));
        newTool.offer(Keys.HIDE_ENCHANTMENTS, true);
        newTool.offer(Keys.DISPLAY_NAME, Component.text("Selector-Tool", NamedTextColor.BLUE));
        newTool.offer(Keys.LORE, Arrays.asList(Component.text("created by ").append(Component.text(player.getName()))));
        return newTool;
    }
}
