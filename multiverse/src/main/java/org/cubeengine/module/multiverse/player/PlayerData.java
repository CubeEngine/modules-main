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
package org.cubeengine.module.multiverse.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Queries;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.world.World;

import static java.util.Collections.emptyList;
import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.key.KeyFactory.makeListKey;
import static org.spongepowered.api.data.key.KeyFactory.makeMapKey;
import static org.spongepowered.api.data.key.KeyFactory.makeSingleKey;

public class PlayerData implements DataSerializable
{
    private static TypeToken<Integer> TT_Int = new TypeToken<Integer>() {};
    private static TypeToken<Value<Integer>> TTV_Int = new TypeToken<Value<Integer>>() {};
    private static TypeToken<Double> TT_Double = new TypeToken<Double>() {};
    private static TypeToken<Value<Double>> TTV_Double = new TypeToken<Value<Double>>() {};
    private static TypeToken<String> TT_String = new TypeToken<String>() {};
    private static TypeToken<Value<String>> TTV_String = new TypeToken<Value<String>>() {};
    private static TypeToken<List<PotionEffect>> TTL_PotionEffect = new TypeToken<List<PotionEffect>>() {};
    private static TypeToken<ListValue<PotionEffect>> TTLV_PotionEffect = new TypeToken<ListValue<PotionEffect>>() {};
    private static TypeToken<Map<Integer, ItemStack>> TTM_Inventory = new TypeToken<Map<Integer, ItemStack>>() {};
    private static TypeToken<MapValue<Integer, ItemStack>> TTMV_Inventory = new TypeToken<MapValue<Integer, ItemStack>>() {};

    public static final Key<Value<Integer>> HELD_ITEM = makeSingleKey(TT_Int, TTV_Int, of("heldItemSlot"), "cubeengine:multiverse:player-helditem", "Held Item Index");
    public static final Key<Value<Double>> HEALTH = makeSingleKey(TT_Double, TTV_Double, of("health"), "cubeengine:multiverse:player-health", "Health");
    public static final Key<Value<Double>> MAX_HEALTH = makeSingleKey(TT_Double, TTV_Double, of("maxHealth"), "cubeengine:multiverse:player-max-health", "Max Health");
    public static final Key<Value<Integer>> FOOD = makeSingleKey(TT_Int, TTV_Int, of("foodLevel"), "cubeengine:multiverse:player-food", "Food");
    public static final Key<Value<Double>> SATURATION = makeSingleKey(TT_Double, TTV_Double, of("saturation"), "cubeengine:multiverse:player-saturation", "Saturation");
    public static final Key<Value<Double>> EXHAUSTION = makeSingleKey(TT_Double, TTV_Double, of("exhaustion"), "cubeengine:multiverse:player-exhaustion", "Exhaustion");
    public static final Key<Value<Integer>> EXP = makeSingleKey(TT_Int, TTV_Int, of("exp"), "cubeengine:multiverse:player-exp", "Exp");
    public static final Key<Value<Integer>> LVL = makeSingleKey(TT_Int, TTV_Int, of("lvl"), "cubeengine:multiverse:player-lvl", "Lvl");
    public static final Key<Value<Integer>> FIRE_TICKS = makeSingleKey(TT_Int, TTV_Int, of("fireticks"), "cubeengine:multiverse:player-fireticks", "Fire-Ticks");
    public static final Key<ListValue<PotionEffect>> ACTIVE_EFFECTS = makeListKey(TTL_PotionEffect, TTLV_PotionEffect, of("activeEffects"), "cubeengine:multiverse:player-effects", "Effects");
    public static final Key<MapValue<Integer, ItemStack>> INVENTORY = makeMapKey(TTM_Inventory, TTMV_Inventory, of("inventory"), "cubeengine:multiverse:player-inventory", "Inventory");
    public static final Key<MapValue<Integer, ItemStack>> ENDER_INVENTORY = makeMapKey(TTM_Inventory, TTMV_Inventory, of("enderInventory"), "cubeengine:multiverse:player-enderchest", "Enderchest");
    public static final Key<Value<String>> GAMEMODE = makeSingleKey(TT_String, TTV_String, of("gamemode"), "cubeengine:multiverse:player-gamemode", "Gamemode");

    public int heldItemSlot = 0;
    public double health = 20;
    public double maxHealth = 20;
    public int foodLevel = 20;
    public double saturation = 20;
    public double exhaustion = 0;
    public int exp = 0;
    public int lvl = 0;
    public int fireTicks = 0;

    public List<PotionEffect> activePotionEffects = new ArrayList<>();
    public Map<Integer, ItemStack> inventory  = new HashMap<>();
    public Map<Integer, ItemStack> enderChest = new HashMap<>();

    private GameMode gameMode;

    // TODO bedspawn?

    public PlayerData(World world)
    {
        gameMode = world.getProperties().getGameMode();
        // TODO defaultdata from world
    }

    public PlayerData(DataContainer value)
    {
        if (!value.getInt(Queries.CONTENT_VERSION).get().equals(getContentVersion()))
        {
            throw new IllegalStateException("Different Version");
        }

        this.heldItemSlot = value.getInt(HELD_ITEM.getQuery()).get();
        this.health = value.getDouble(HEALTH.getQuery()).get();
        this.maxHealth = value.getDouble(MAX_HEALTH.getQuery()).get();
        this.foodLevel = value.getInt(FOOD.getQuery()).get();
        this.saturation = value.getDouble(SATURATION.getQuery()).get();
        this.exhaustion = value.getDouble(EXHAUSTION.getQuery()).get();
        this.exp = value.getInt(EXP.getQuery()).get();
        this.lvl = value.getInt(LVL.getQuery()).get();
        this.fireTicks = value.getInt(FIRE_TICKS.getQuery()).get();
        this.activePotionEffects = value.getSerializableList(ACTIVE_EFFECTS.getQuery(), PotionEffect.class).orElse(new ArrayList<>());

        inventory.clear();
        DataView inventoryView = value.getView(INVENTORY.getQuery()).orElse(DataContainer.createNew());
        for (DataQuery key : inventoryView.getKeys(false))
        {
            try
            {
                inventory.put(Integer.valueOf(key.asString("")), ItemStack.builder().fromContainer(inventoryView.getView(key).get()).build());
            }
            catch (RuntimeException ignored)
            {
                System.err.println("Cannot deserialize inventory!");
                System.err.println(ignored.getMessage());
                // TODO maybe try to recover?
                // This can happen when the item was using legacy DataManipulators e.g. SpongeDisplayNameData
                // in addition to the UnsafeData used now
            }
        }

        enderChest.clear();
        inventoryView = value.getView(ENDER_INVENTORY.getQuery()).orElse(DataContainer.createNew());
        for (DataQuery key : inventoryView.getKeys(false))
        {
            enderChest.put(Integer.valueOf(key.asString("")), ItemStack.builder().fromContainer(inventoryView.getView(key).get()).build());
        }

        this.gameMode = Sponge.getRegistry().getType(GameMode.class, value.getString(GAMEMODE.getQuery()).get()).get();
    }

    @Override
    public DataContainer toContainer()
    {
        DataContainer result = DataContainer.createNew().set(Queries.CONTENT_VERSION, getContentVersion());

        result.set(HELD_ITEM, heldItemSlot)
            .set(HEALTH, health)
            .set(MAX_HEALTH, maxHealth)
            .set(FOOD, foodLevel)
            .set(SATURATION, saturation)
            .set(EXHAUSTION, exhaustion)
            .set(EXP, exp)
            .set(LVL, lvl)
            .set(FIRE_TICKS, fireTicks)
              // TODO are those working?:
            .set(ACTIVE_EFFECTS, activePotionEffects)
            .set(INVENTORY, inventory)
            .set(ENDER_INVENTORY, enderChest)
            .set(GAMEMODE, gameMode.getId());
        return result;
    }

    public void applyToPlayer(Player player)
    {
        Inventory inv = player.getInventory();
        ((Hotbar)inv.query(Hotbar.class)).setSelectedSlotIndex(heldItemSlot);
        player.offer(Keys.MAX_HEALTH, maxHealth);
        player.offer(Keys.HEALTH, health);
        player.offer(Keys.FOOD_LEVEL, foodLevel);
        player.offer(Keys.SATURATION, saturation);
        player.offer(Keys.EXHAUSTION, exhaustion);
        player.offer(Keys.TOTAL_EXPERIENCE, exp);
        player.offer(Keys.EXPERIENCE_LEVEL, exp);
        if (fireTicks != 0)
        {
            player.offer(Keys.FIRE_TICKS, fireTicks);
        }

        player.remove(Keys.POTION_EFFECTS);
        player.offer(Keys.POTION_EFFECTS, activePotionEffects);

        int i = 0;
        for (Inventory slot : inv.slots())
        {
            ItemStack item = inventory.get(i);
            if (item != null)
            {
                slot.set(item);
            }
            else
            {
                slot.clear();
            }
            i++;
        }

         /* TODO EnderChet
        i = 0;
        for (Inventory slot : player.getEnderChest().slots())
        {
            ItemStack item = enderChest.get(i);
            if (item != null)
            {
                slot.set(item);
            }
            else
            {
                slot.clear();
            }
            i++;
        }
        */

        player.offer(Keys.GAME_MODE, gameMode);
    }

    public void applyFromPlayer(Player player)
    {
        Inventory playerInventory = player.getInventory();
        this.heldItemSlot = ((Hotbar)playerInventory.query(Hotbar.class)).getSelectedSlotIndex();
        this.maxHealth = player.get(Keys.MAX_HEALTH).get();
        this.health = player.get(Keys.HEALTH).get();
        this.foodLevel = player.get(Keys.FOOD_LEVEL).get();
        this.saturation = player.get(Keys.SATURATION).get();
        this.exhaustion = player.get(Keys.EXHAUSTION).get();
        this.lvl = player.get(Keys.EXPERIENCE_LEVEL).get();
        this.exp = player.get(Keys.TOTAL_EXPERIENCE).get();
        this.fireTicks = player.get(Keys.FIRE_TICKS).orElse(0);
        this.activePotionEffects = player.get(Keys.POTION_EFFECTS).orElse(emptyList());

        this.inventory = new HashMap<>();
        int i = 0;
        for (Inventory slot : playerInventory.slots())
        {
            Optional<ItemStack> item = slot.peek();
            if (item.isPresent())
            {
                this.inventory.put(i, item.get());
            }
            i++;
        }

        this.enderChest = new HashMap<>();
        /* TODO EnderChest
        i = 0;
        for (Inventory slot : player.getEnderChest().slots())
        {
            Optional<ItemStack> item = slot.peek();
            if (item.isPresent())
            {
                this.enderChest.put(i, item.get());
            }
            i++;
        }
        */
        this.gameMode = player.get(Keys.GAME_MODE).get();
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

}
