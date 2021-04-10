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
package org.cubeengine.module.portals;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;

public interface PortalsItems
{
    static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration> event)
    {
        {
            final RecipeRegistration recipe = CraftingRecipe.shapedBuilder()
                  .aisle(" c ", "cac", " l ")
                  .where('c', Ingredient.of(ItemTypes.COMPASS))
                  .where('a', Ingredient.of(ItemTypes.ARMOR_STAND))
                  .where('l', Ingredient.of(ItemTypes.LODESTONE))
                  .result(portalExit())
                  .key(ResourceKey.of(PluginPortals.PORTALS_ID, "portal_exit"))
                  .build();
            event.register(recipe);
        }
    }

    @NotNull
    static ItemStack portalExit()
    {
        final ItemStack newTool = ItemStack.of(ItemTypes.ARMOR_STAND);
        newTool.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)));
        newTool.offer(Keys.HIDE_ENCHANTMENTS, true);
        newTool.offer(Keys.CUSTOM_NAME, Component.text("Portal Exit", NamedTextColor.DARK_AQUA));
        newTool.offer(Keys.LORE, Arrays.asList(Component.text("To be used with a Saved Selection", NamedTextColor.GRAY)));
        newTool.offer(PortalsData.PORTAL, "exit");
        return newTool;
    }
}
