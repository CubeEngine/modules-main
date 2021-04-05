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

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.math.vector.Vector3d;

import static org.spongepowered.api.item.ItemTypes.COAL;

public interface ZonedItems
{

    static void registerRecipes(RegisterDataPackValueEvent<RecipeRegistration> event, ZonedListener listener)
    {
        final RecipeRegistration selectionToolRecipe = CraftingRecipe.shapedBuilder()
                                                                  .aisle("trt", "rcr", "trt")
                                                                  .where('t', Ingredient.of(ItemTypes.REDSTONE_TORCH))
                                                                  .where('r', Ingredient.of(ItemTypes.REDSTONE))
                                                                  .where('c', Ingredient.of(ItemTypes.COAL))
                                                                  .result(newTool())
                                                                  .key(ResourceKey.of(PluginZoned.ZONED_ID, "selection_tool"))
                                                                  .build();
        event.register(selectionToolRecipe);

        final Ingredient toolIngredient = Ingredient.of(ResourceKey.of(PluginZoned.ZONED_ID, "tool_ingredient"), stack -> stack.get(ZonedData.ZONE_TYPE).isPresent(), newTool());
        final RecipeRegistration savedSelectionRecipe = CraftingRecipe.shapedBuilder()
                                                                      .aisle(" t ", "ses", " i ")
                                                                      .where('s', Ingredient.of(ItemTypes.STRING))
                                                                      .where('i', Ingredient.of(ItemTypes.SLIME_BALL))
                                                                      .where('e', Ingredient.of(ItemTypes.ENDER_EYE))
                                                                      .where('t', toolIngredient)
                                                                      .remainingItems(g ->
                                                                          Arrays.asList(ItemStack.empty(), g.asGrid().peek(0, 1).get(), ItemStack.empty(),
                                                                                        ItemStack.empty(), ItemStack.empty(), ItemStack.empty(),
                                                                                        ItemStack.empty(), ItemStack.empty(), ItemStack.empty())
                                                                      )
                                                                      .result(g -> {
                                                                          final ServerPlayer craftingPlayer = Sponge.server().causeStackManager().currentCause().first(ServerPlayer.class).orElse(null);
                                                                          if (craftingPlayer == null)
                                                                          {
                                                                              return ItemStack.empty();
                                                                          }
                                                                          return savedSelection(craftingPlayer, listener);
                                                                      }, savedSelection())
                                                                      .key(ResourceKey.of(PluginZoned.ZONED_ID, "saved_selection"))
                                                                      .build();
        event.register(savedSelectionRecipe);
    }

    static ItemStack newTool()
    {
        final ItemStack newTool = ItemStack.of(COAL);
        newTool.offer(ZonedData.ZONE_TYPE, "default");
        newTool.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)));
        newTool.offer(Keys.HIDE_ENCHANTMENTS, true);
        newTool.offer(Keys.CUSTOM_NAME, Component.text("Selector-Tool", NamedTextColor.DARK_AQUA));
        newTool.offer(Keys.LORE, Arrays.asList(Component.text("Cuboid Selector", NamedTextColor.GRAY)));
        return newTool;
    }

    static ItemStack savedSelection(ServerPlayer player, ZonedListener listener)
    {
        final ItemStack newTool = savedSelection();
        if (player != null)
        {
            final ZoneConfig zone = listener.getZone(player);
            if (zone == null)
            {
                return ItemStack.empty();
            }
            final Cuboid cuboid = zone.shape.getBoundingCuboid();
            final Vector3d min = cuboid.getMinimumPoint();
            final Vector3d max = cuboid.getMaximumPoint();
            final ResourceKey worldKey = ResourceKey.resolve(zone.world.getName());
            newTool.offer(ZonedData.ZONE_WORLD, worldKey);
            newTool.offer(ZonedData.ZONE_MAX, max);
            newTool.offer(ZonedData.ZONE_MIN, min);
        }
        return newTool;
    }

    @NotNull
    static ItemStack savedSelection()
    {
        final ItemStack newTool = ItemStack.of(ItemTypes.ENDER_EYE);
        newTool.offer(Keys.APPLIED_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)));
        newTool.offer(Keys.HIDE_ENCHANTMENTS, true);
        newTool.offer(Keys.CUSTOM_NAME, Component.text("Saved Selection", NamedTextColor.DARK_AQUA));
        newTool.offer(Keys.LORE, Arrays.asList(Component.text("Cuboid Selection", NamedTextColor.GRAY)));
        return newTool;
    }

}
