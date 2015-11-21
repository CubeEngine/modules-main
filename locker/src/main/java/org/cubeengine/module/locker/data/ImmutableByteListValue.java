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
import org.spongepowered.api.data.value.immutable.ImmutableListValue;
import org.spongepowered.api.data.value.mutable.ListValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ImmutableByteListValue extends AbstractBaseValue<List<Byte>> implements ImmutableListValue<Byte>
{
    public ImmutableByteListValue(Key<? extends BaseValue<List<Byte>>> key, List<Byte> actualValue)
    {
        super(key, actualValue, Collections.<Byte>emptyList());
    }

    @Override
    public Byte get(int index)
    {
        return actualValue.get(index);
    }

    @Override
    public ImmutableListValue<Byte> with(int index, Byte value)
    {
        return transform(list -> {
            list.set(index, value);
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> with(int index, Iterable<Byte> values)
    {
        ArrayList<Byte> newList = new ArrayList<>(actualValue);
        for (Byte value : values)
        {
            newList.set(index++, value);
        }
        return new ImmutableByteListValue(getKey(), newList);
    }

    @Override
    public ImmutableListValue<Byte> without(int index)
    {
        return transform(list -> {
            list.remove(index);
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> set(int index, Byte element)
    {
        return transform(list -> {
            list.set(index, element);
            return list;
        });
    }

    @Override
    public int indexOf(Byte element)
    {
        return actualValue.indexOf(element);
    }

    @Override
    public int size()
    {
        return actualValue.size();
    }

    @Override
    public boolean isEmpty()
    {
        return actualValue.isEmpty();
    }

    @Override
    public ImmutableListValue<Byte> with(List<Byte> collection)
    {
        return new ImmutableByteListValue(getKey(), collection);
    }

    @Override
    public ImmutableListValue<Byte> withElement(Byte elements)
    {
        return transform(list -> {
            list.add(elements);
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> transform(Function<List<Byte>, List<Byte>> function)
    {
        return new ImmutableByteListValue(getKey(), function.apply(new ArrayList<>(actualValue)));
    }

    @Override
    public ImmutableListValue<Byte> withAll(Iterable<Byte> elements)
    {
        return transform(list -> {
            for (Byte element : elements)
            {
                list.add(element);
            }
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> without(Byte element)
    {
        return transform(list -> {
            list.remove(element);
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> withoutAll(Iterable<Byte> elements)
    {
        return transform(list -> {
            for (Byte element : elements)
            {
                list.remove(element);
            }
            return list;
        });
    }

    @Override
    public ImmutableListValue<Byte> withoutAll(Predicate<Byte> predicate)
    {
        return transform(list -> {
            list.removeIf(predicate);
            return list;
        });
    }

    @Override
    public boolean contains(Byte element)
    {
        return actualValue.contains(element);
    }

    @Override
    public boolean containsAll(Iterable<Byte> iterable)
    {
        for (Byte aByte : iterable)
        {
            if (!contains(aByte))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Byte> getAll()
    {
        return new ArrayList<>(actualValue);
    }

    @Override
    public ListValue<Byte> asMutable()
    {
        return new ByteListValue(getKey(), actualValue);
    }
}
