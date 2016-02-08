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
package org.cubeengine.module.multiverse.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.world.World;

public class MultiverseData extends AbstractData<MultiverseData, ImmutableMultiverseData> implements IMultiverseData
{
    public static final Key<Value<String>> WORLD = KeyFactory.makeSingleKey(String.class, BaseValue.class, DataQuery.of("current"));
    public static final Key<MapValue<String, DataContainer>> DATA = KeyFactory.makeMapKey(String.class, DataContainer.class, DataQuery.of("playerdata"));

    public String currentUniverse;
    private Map<String, PlayerData> playerData;

    public MultiverseData(String currentUniverse, Map<String, PlayerData> playerData)
    {
        this.currentUniverse = currentUniverse;
        this.playerData = playerData;
        registerGettersAndSetters();
    }

    @Override
    protected void registerGettersAndSetters()
    {
        registerFieldGetter(WORLD, MultiverseData.this::getCurrentUniverse);
        registerFieldSetter(WORLD, MultiverseData.this::setCurrentUniverse);
        registerKeyValue(WORLD, MultiverseData.this::currentWorld);

        registerFieldGetter(DATA, MultiverseData.this::getPlayerData);
        registerFieldSetter(DATA, MultiverseData.this::setPlayerData);
        registerKeyValue(DATA, MultiverseData.this::playerData);
    }

    @Override
    public Optional<MultiverseData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        return apply(dataHolder.get(WORLD), dataHolder.get(DATA));
    }

    private Optional<MultiverseData> apply(Optional<String> world, Optional<Map<String, DataContainer>> data)
    {
        if (world.isPresent() && data.isPresent())
        {
            setCurrentUniverse(world.get());
            setPlayerData(data.get());
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<MultiverseData> from(DataContainer container)
    {
        Optional<DataView> data = container.getView(DATA.getQuery());
        if (data.isPresent())
        {
            Map<String, DataContainer> map = new HashMap<>();
            for (DataQuery key : data.get().getKeys(false))
            {
                map.put(key.asString("."), data.get().getView(key).get().copy());
            }
            return apply(container.getString(WORLD.getQuery()), Optional.of(map));
        }
        return Optional.empty();
    }

    @Override
    public MultiverseData copy()
    {
        return new MultiverseData(currentUniverse, playerData);
    }

    @Override
    public ImmutableMultiverseData asImmutable()
    {
        return new ImmutableMultiverseData(currentUniverse, playerData);
    }

    @Override
    public int compareTo(MultiverseData o)
    {
        return IMultiverseData.compareTo(this, o);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

    @Override
    public DataContainer toContainer()
    {
        DataContainer result = super.toContainer();
        result.set(WORLD, getCurrentUniverse());
        result.set(DATA, getPlayerData());
        return result;
    }

    public Value<String> currentWorld()
    {
        return Sponge.getRegistry().getValueFactory().createValue(WORLD, getCurrentUniverse());
    }

    public void setCurrentUniverse(String e)
    {
        this.currentUniverse = e;
    }

    public String getCurrentUniverse()
    {
        return currentUniverse;
    }

    public MapValue<String, DataContainer> playerData()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(DATA, getPlayerData());
    }

    public void setPlayerData(Map<String, DataContainer> e)
    {
        this.playerData = new HashMap<>();
        for (Entry<String, DataContainer> entry : e.entrySet())
        {
            this.playerData.put(entry.getKey(), new PlayerData(entry.getValue()));
        }
    }

    public Map<String, DataContainer> getPlayerData()
    {
        return playerData.entrySet().stream().collect(
            Collectors.toMap(Entry::getKey, e -> e.getValue().toContainer()));
    }

    public PlayerData from(String univsere, World world)
    {
        PlayerData data = this.playerData.get(univsere);
        if (data == null)
        {
            data = new PlayerData(world);
            this.playerData.put(univsere, data);
        }
        return data;
    }
}
