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
package org.cubeengine.module.locker.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.module.locker.PluginLocker;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;

public interface LockerItems
{
    static void registerRecipes(RegisterDataPackValueEvent event)
    {
        final ItemStack lockerBook = ItemStack.of(ItemTypes.ENCHANTED_BOOK);
        lockerBook.offer(LockerData.MODE, LockerMode.INFO_CREATE.name());
        lockerBook.offer(Keys.CUSTOM_NAME, Component.text("Locker ").append(ItemTypes.BOOK.get().asComponent()).color(NamedTextColor.DARK_PURPLE));
        final RecipeRegistration lockerBookRecipe = CraftingRecipe.shapedBuilder()
            .aisle("kkk", "kbk", "kkk")
            .where('k', Ingredient.of(ItemTypes.TRIPWIRE_HOOK))
            .where('b', Ingredient.of(ItemTypes.BOOK))
            .result(lockerBook)
            .key(ResourceKey.of(PluginLocker.LOCKER_ID, "lockerbook"))
            .build();
        event.register(lockerBookRecipe);
    }
}
