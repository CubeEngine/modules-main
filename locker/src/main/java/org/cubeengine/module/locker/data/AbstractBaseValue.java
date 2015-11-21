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

import java.util.Optional;

public abstract class AbstractBaseValue<E> implements BaseValue<E>
{
    private final Key<? extends BaseValue<E>> key;
    private final E defaultValue;
    protected E actualValue;

    public AbstractBaseValue(Key<? extends BaseValue<E>> key, E actualValue, E defaultValue)
    {
        this.key = key;
        this.actualValue = actualValue;
        this.defaultValue = defaultValue;
    }

    public AbstractBaseValue(Key<? extends BaseValue<E>> key, E defaultValue)
    {
        this(key, defaultValue, defaultValue);
    }

    @Override
    public E get()
    {
        return exists() ? actualValue : defaultValue;
    }

    @Override
    public boolean exists()
    {
        return actualValue != null;
    }

    @Override
    public E getDefault()
    {
        return defaultValue;
    }

    @Override
    public Optional<E> getDirect()
    {
        return Optional.ofNullable(actualValue);
    }

    @Override
    public Key<? extends BaseValue<E>> getKey()
    {
        return key;
    }
}
