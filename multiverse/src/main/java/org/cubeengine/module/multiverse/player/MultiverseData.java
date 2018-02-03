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

import static org.spongepowered.api.data.DataQuery.of;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

public class MultiverseData extends AbstractData<MultiverseData, ImmutableMultiverseData> implements IMultiverseData
{
    private static TypeToken<Value<String>> TTV_String = new TypeToken<Value<String>>() {};
    private static TypeToken<MapValue<String, DataContainer>> TTMV_Data = new TypeToken<MapValue<String, DataContainer>>() {};

    public static final Key<Value<String>> WORLD = Key.builder().type(TTV_String).query(of("current")).id("data-world").name("World").build();
    public static final Key<MapValue<String, DataContainer>> DATA = Key.builder().type(TTMV_Data).query(of("playerdata")).id("data-container").name("DataContainer").build();

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
        registerFieldGetter(WORLD, this::getCurrentUniverse);
        registerFieldSetter(WORLD, this::setCurrentUniverse);
        registerKeyValue(WORLD, this::currentWorld);

        registerFieldGetter(DATA, this::getPlayerData);
        registerFieldSetter(DATA,this::setPlayerData);
        registerKeyValue(DATA, this::playerData);
    }

    @Override
    public Optional<MultiverseData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        // TODO mergeFunction
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
        this.setCurrentUniverse("default");
        return Optional.of(this);
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
    public int getContentVersion()
    {
        return 2;
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
       return this.from(univsere, world, null);
    }

    public PlayerData from(String univsere, World world, Player player)
    {
        PlayerData data = this.playerData.get(univsere);
        if (data == null)
        {
            data = new PlayerData(world);
            if (player != null)
            {
                data.applyFromPlayer(player);
            }
            this.playerData.put(univsere, data);
        }
        return data;
    }
}
