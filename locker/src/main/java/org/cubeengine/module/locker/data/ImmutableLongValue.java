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
package org.cubeengine.module.locker.data;

import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.function.Function;

public class ImmutableLongValue extends AbstractBaseValue<Long> implements ImmutableValue<Long>
{
    public ImmutableLongValue(Key<? extends BaseValue<Long>> key, Long actualValue)
    {
        super(key, actualValue, 0L);
    }

    @Override
    public ImmutableValue<Long> with(Long value)
    {
        return new ImmutableLongValue(getKey(), value);
    }

    @Override
    public ImmutableValue<Long> transform(Function<Long, Long> function)
    {
        return new ImmutableLongValue(getKey(), function.apply(actualValue));
    }

    @Override
    public Value<Long> asMutable()
    {
        return new LongValue(getKey(), actualValue);
    }
}
