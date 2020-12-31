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
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataSerializable;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;

import static java.util.Collections.emptyList;

public class PlayerData implements DataSerializable
{
    public static final DataQuery HELD_ITEM = DataQuery.of("heldItemSlot");
    public static final DataQuery HEALTH = DataQuery.of("health");
    public static final DataQuery MAX_HEALTH = DataQuery.of("maxHealth");
    public static final DataQuery FOOD = DataQuery.of("foodLevel");
    public static final DataQuery SATURATION = DataQuery.of("saturation");
    public static final DataQuery EXHAUSTION = DataQuery.of("exhaustion");
    public static final DataQuery EXP = DataQuery.of("exp");
    public static final DataQuery FIRE_TICKS = DataQuery.of("fireticks");
    public static final DataQuery ACTIVE_EFFECTS = DataQuery.of("activeEffects");
    public static final DataQuery INVENTORY = DataQuery.of("inventory");
    public static final DataQuery ENDER_INVENTORY = DataQuery.of("enderInventory");
    public static final DataQuery GAMEMODE = DataQuery.of("gamemode");

    public int heldItemSlot = 0;
    public double health = 20;
    public double maxHealth = 20;
    public int foodLevel = 20;
    public double saturation = 20;
    public double exhaustion = 0;
    public int exp = 0;
    public long fireTicks = 0;

    public List<PotionEffect> activePotionEffects = new ArrayList<>();
    public Map<Integer, ItemStack> inventory  = new HashMap<>();
    public Map<Integer, ItemStack> enderChest = new HashMap<>();

    private GameMode gameMode;

    // TODO bedspawn?

    public PlayerData(ServerWorld world)
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

        this.heldItemSlot = value.getInt(HELD_ITEM).get();
        this.health = value.getDouble(HEALTH).get();
        this.maxHealth = value.getDouble(MAX_HEALTH).get();
        this.foodLevel = value.getInt(FOOD).get();
        this.saturation = value.getDouble(SATURATION).get();
        this.exhaustion = value.getDouble(EXHAUSTION).get();
        this.exp = value.getInt(EXP).get();
        this.fireTicks = value.getInt(FIRE_TICKS).get();
        this.activePotionEffects = value.getSerializableList(ACTIVE_EFFECTS, PotionEffect.class).orElse(new ArrayList<>());

        inventory.clear();
        DataView inventoryView = value.getView(INVENTORY).orElse(DataContainer.createNew());
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
        inventoryView = value.getView(ENDER_INVENTORY).orElse(DataContainer.createNew());
        for (DataQuery key : inventoryView.getKeys(false))
        {
            enderChest.put(Integer.valueOf(key.asString("")), ItemStack.builder().fromContainer(inventoryView.getView(key).get()).build());
        }

        this.gameMode = Sponge.getGame().registries().registry(RegistryTypes.GAME_MODE).value(ResourceKey.resolve(value.getString(GAMEMODE).get()));
    }

    public static PlayerData of(DataContainer dataContainer, ServerWorld world)
    {
        if (dataContainer == null)
        {
            return new PlayerData(world);
        }
        return new PlayerData(dataContainer);
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
            .set(FIRE_TICKS, fireTicks)
              // TODO are those working?:
            .set(ACTIVE_EFFECTS, activePotionEffects)
            .set(INVENTORY, inventory)
            .set(ENDER_INVENTORY, enderChest)
            .set(GAMEMODE, Sponge.getGame().registries().registry(RegistryTypes.GAME_MODE).valueKey(gameMode).asString());
        return result;
    }

    public PlayerData applyToPlayer(ServerPlayer player)
    {
        final PlayerInventory inv = player.getInventory();
        inv.getHotbar().setSelectedSlotIndex(heldItemSlot);
        player.offer(Keys.MAX_HEALTH, maxHealth);
        player.offer(Keys.HEALTH, health);
        player.offer(Keys.FOOD_LEVEL, foodLevel);
        player.offer(Keys.SATURATION, saturation);
        player.offer(Keys.EXHAUSTION, exhaustion);
        player.offer(Keys.EXPERIENCE, exp);
        if (fireTicks != 0)
        {
            player.offer(Keys.FIRE_TICKS, Ticks.of(fireTicks));
        }

        player.remove(Keys.POTION_EFFECTS);
        player.offer(Keys.POTION_EFFECTS, activePotionEffects);

        int i = 0;
        for (Slot slot : inv.slots())
        {
            ItemStack item = this.inventory.get(i);
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

        i = 0;
        for (Slot slot : player.getEnderChestInventory().slots())
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

        player.offer(Keys.GAME_MODE, gameMode);
        return this;
    }

    public PlayerData applyFromPlayer(ServerPlayer player)
    {
        PlayerInventory playerInventory = player.getInventory();
        this.heldItemSlot = playerInventory.getHotbar().getSelectedSlotIndex();
        this.maxHealth = player.get(Keys.MAX_HEALTH).get();
        this.health = player.get(Keys.HEALTH).get();
        this.foodLevel = player.get(Keys.FOOD_LEVEL).get();
        this.saturation = player.get(Keys.SATURATION).get();
        this.exhaustion = player.get(Keys.EXHAUSTION).get();
        this.exp = player.get(Keys.EXPERIENCE).get();
        this.fireTicks = player.get(Keys.FIRE_TICKS).map(Ticks::getTicks).orElse(0L);
        this.activePotionEffects = player.get(Keys.POTION_EFFECTS).orElse(emptyList());

        this.inventory = new HashMap<>();
        int i = 0;
        for (Slot slot : playerInventory.slots())
        {
            final ItemStack item = slot.peek();
            if (!item.isEmpty())
            {
                this.inventory.put(i, item);
            }
            i++;
        }

        this.enderChest = new HashMap<>();
        i = 0;
        for (Slot slot : player.getEnderChestInventory().slots())
        {
            final ItemStack item = slot.peek();
            if (!item.isEmpty())
            {
                this.enderChest.put(i, item);
            }
            i++;
        }
        this.gameMode = player.get(Keys.GAME_MODE).get();
        return this;
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

}
