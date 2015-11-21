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
import org.spongepowered.api.data.value.mutable.Value;

import java.util.function.Function;

public abstract class AbstractValue<E> extends AbstractBaseValue<E> implements Value<E>
{
    public AbstractValue(Key<? extends BaseValue<E>> key, E actualValue, E defaultValue)
    {
        super(key, actualValue, defaultValue);
    }

    @Override
    public Value<E> set(E value)
    {
        this.actualValue = value;
        return this;
    }

    @Override
    public Value<E> transform(Function<E, E> function) {
        this.actualValue = function.apply(this.actualValue);
        return this;
    }
}
