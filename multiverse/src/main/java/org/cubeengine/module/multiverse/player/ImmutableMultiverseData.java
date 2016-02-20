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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableMapValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import static org.cubeengine.module.multiverse.player.MultiverseData.DATA;
import static org.cubeengine.module.multiverse.player.MultiverseData.WORLD;

public class ImmutableMultiverseData extends AbstractImmutableData<ImmutableMultiverseData, MultiverseData> implements IMultiverseData
{
    private final String currentWorld;
    private final Map<String, PlayerData> playerData;

    public ImmutableMultiverseData(String currentWorld, Map<String, PlayerData> playerData)
    {
        this.currentWorld = currentWorld;
        this.playerData = playerData;
        registerGetters();
    }

    @Override
    protected void registerGetters()
    {
        registerFieldGetter(WORLD, ImmutableMultiverseData.this::getCurrentUniverse);
        registerKeyValue(WORLD, ImmutableMultiverseData.this::currentWorld);

        registerFieldGetter(DATA, ImmutableMultiverseData.this::getPlayerData);
        registerKeyValue(DATA, ImmutableMultiverseData.this::playerData);
    }

    @Override
    public <E> Optional<ImmutableMultiverseData> with(Key<? extends BaseValue<E>> key, E value)
    {
        ImmutableMultiverseData data = null;
        if (WORLD.equals(key))
        {
            data = new ImmutableMultiverseData(((String)value), playerData);
        }
        if (DATA.equals(key))
        {
            @SuppressWarnings("unchecked")
            Map<String, DataContainer> map = (Map<String, DataContainer>)value;
            data = new ImmutableMultiverseData(currentWorld, map.entrySet().stream().collect(
                Collectors.toMap(Entry::getKey, e -> new PlayerData(e.getValue()))));
        }
        return Optional.ofNullable(data);
    }

    @Override
    public MultiverseData asMutable()
    {
        return new MultiverseData(currentWorld, playerData);
    }

    @Override
    public int compareTo(ImmutableMultiverseData o)
    {
        return IMultiverseData.compareTo(this, o);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }


    public ImmutableValue<String> currentWorld()
    {
        return Sponge.getRegistry().getValueFactory().createValue(WORLD, getCurrentUniverse()).asImmutable();
    }

    public String getCurrentUniverse()
    {
        return currentWorld;
    }

    public ImmutableMapValue<String, DataContainer> playerData()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(DATA, getPlayerData()).asImmutable();
    }


    public Map<String, DataContainer> getPlayerData()
    {
        return playerData.entrySet().stream().collect(
            Collectors.toMap(Entry::getKey, e -> e.getValue().toContainer()));
    }

}
