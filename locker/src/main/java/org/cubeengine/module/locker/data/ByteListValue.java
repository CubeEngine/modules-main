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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ByteListValue extends AbstractValue<List<Byte>> implements ListValue<Byte>
{
    public ByteListValue(Key<? extends BaseValue<List<Byte>>> key, List<Byte> actualValue)
    {
        super(key, actualValue, Collections.emptyList());
    }

    @Override
    public Byte get(int index)
    {
        return actualValue.get(index);
    }

    @Override
    public ListValue<Byte> add(int index, Byte value)
    {
        actualValue.add(index, value);
        return this;
    }

    @Override
    public ListValue<Byte> add(int index, Iterable<Byte> values)
    {
        for (Byte value : values)
        {
            add(index++, value);
        }
        return this;
    }

    @Override
    public ListValue<Byte> remove(int index)
    {
        actualValue.remove(index);
        return this;
    }

    @Override
    public ListValue<Byte> set(int index, Byte element)
    {
        actualValue.set(index, element);
        return this;
    }

    @Override
    public int indexOf(Byte element)
    {
        return actualValue.indexOf(element);
    }

    @Override
    public ListValue<Byte> set(List<Byte> value)
    {
        actualValue = value;
        return this;
    }

    @Override
    public ListValue<Byte> transform(Function<List<Byte>, List<Byte>> function)
    {
        this.actualValue = new ArrayList<>(function.apply(actualValue));
        return this;
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
    public ListValue<Byte> add(Byte element)
    {
        actualValue.add(element);
        return this;
    }

    @Override
    public ListValue<Byte> addAll(Iterable<Byte> elements)
    {
        for (Byte element : elements)
        {
            add(element);
        }
        return this;
    }

    @Override
    public ListValue<Byte> remove(Byte element)
    {
        actualValue.remove(element);
        return this;
    }

    @Override
    public ListValue<Byte> removeAll(Iterable<Byte> elements)
    {
        for (Byte element : elements)
        {
            remove(element);
        }
        return this;
    }

    @Override
    public ListValue<Byte> removeAll(Predicate<Byte> predicate)
    {
        actualValue.removeIf(predicate);
        return this;
    }

    @Override
    public boolean contains(Byte element)
    {
        return actualValue.contains(element);
    }

    @Override
    public boolean containsAll(Collection<Byte> iterable)
    {
        return actualValue.containsAll(iterable);
    }

    @Override
    public ListValue<Byte> filter(Predicate<? super Byte> predicate)
    {
        actualValue = actualValue.stream().filter(predicate).collect(Collectors.toList());
        return this;
    }

    @Override
    public List<Byte> getAll()
    {
        return new ArrayList<>(actualValue);
    }

    @Override
    public ImmutableListValue<Byte> asImmutable()
    {
        return new ImmutableByteListValue(getKey(), actualValue);
    }

    @Override
    public Iterator<Byte> iterator()
    {
        return actualValue.iterator();
    }
}
