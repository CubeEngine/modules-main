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

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.*;

public class LockerData implements DataManipulator<LockerData, ImmutableLockerData>
{
    public static Key<Value<Long>> LOCK_ID = KeyFactory.makeSingleKey(Long.class, Value.class, new DataQuery("LockID"));
    public static Key<ListValue<Byte>> LOCK_PASS = KeyFactory.makeListKey(Byte.class, new DataQuery("LockPass"));

    private long lockID;
    private byte[] pass;
    private ValueFactory valueFactory;

    public LockerData(long lockID, byte[] pass, ValueFactory valueFactory)
    {
        this.lockID = lockID;
        this.pass = pass;
        this.valueFactory = valueFactory;
    }

    public LockerData(ValueFactory valueFactory)
    {
        this(0, null, valueFactory);
    }

    @Override
    public Optional<LockerData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<Long> lockID = dataHolder.get(LOCK_ID);
        if (lockID.isPresent())
        {
            LockerData data = this.copy();
            data.lockID = lockID.get();
            data.pass = null;
            Optional<List<Byte>> pass = dataHolder.get(LOCK_PASS);
            if (pass.isPresent())
            {
                data.pass = new byte[pass.get().size()];
                for (int i = 0; i < pass.get().size(); i++)
                {
                    data.pass[i] = pass.get().get(i);
                }
            }

            data = overlap.merge(this, data);

            if (data != this)
            {
                this.lockID = data.lockID;
                this.pass = data.pass;
            }

            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<LockerData> from(DataContainer container)
    {
        Optional<Long> lockID = container.getLong(LOCK_ID.getQuery());
        if (lockID.isPresent())
        {
            this.lockID = lockID.get();
            Optional<List<Byte>> pass = container.getByteList(LOCK_PASS.getQuery());
            if (pass.isPresent())
            {
                this.pass = new byte[pass.get().size()];
                for (int i = 0; i < pass.get().size(); i++)
                {
                    this.pass[i] = pass.get().get(i);
                }
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public <E> LockerData set(Key<? extends BaseValue<E>> key, E value)
    {
        if (LOCK_ID.equals(key))
        {
            this.lockID = (Long)value;
        }
        else if (LOCK_PASS.equals(key))
        {
            List<Byte> list = (List<Byte>) value;
            this.pass = new byte[list.size()];
            for (int i = 0; i < list.size(); i++)
            {
                this.pass[i] = list.get(i);
            }
        }
        return this;
    }

    @Override
    public LockerData copy()
    {
        return new LockerData(lockID, pass, valueFactory);
    }

    @Override
    public ImmutableLockerData asImmutable()
    {
        return new ImmutableLockerData(lockID, pass, valueFactory);
    }

    @Override
    public int compareTo(LockerData o)
    {
        return Long.compare(lockID, o.lockID);
    }

    @Override
    public DataContainer toContainer()
    {
        MemoryDataContainer container = new MemoryDataContainer();
        container.set(LOCK_ID, this.lockID);
        if (pass == null || pass.length == 0)
        {
            return container;
        }
        List<Byte> pass = passAsList();
        return container.set(LOCK_PASS, pass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key)
    {
        if (supports(key))
        {
            if (LOCK_ID.equals(key))
            {
                return Optional.ofNullable((E)(Long)lockID);
            }
            else if (LOCK_PASS.equals(key))
            {
                if (pass == null || pass.length == 0)
                {
                    return Optional.empty();
                }
                List<Byte> list = passAsList();
                return Optional.of((E)list);
            }
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key)
    {
        if (supports(key))
        {
            if (LOCK_ID.equals(key))
            {
                return Optional.ofNullable((V) valueFactory.createValue(LOCK_ID, lockID));
            }
            else if (LOCK_PASS.equals(key))
            {
                if (pass == null || pass.length == 0)
                {
                    return Optional.empty();
                }
                List<Byte> list = passAsList();

                return Optional.of((V) valueFactory.createListValue(LOCK_PASS, list));
            }
        }
        return Optional.empty();
    }

    private List<Byte> passAsList()
    {
        List<Byte> list = new ArrayList<>();
        for (byte b : pass)
        {
            list.add(b);
        }
        return list;
    }

    @Override
    public boolean supports(Key<?> key)
    {
        return LOCK_ID.equals(key) || LOCK_PASS.equals(key);
    }

    @Override
    public Set<Key<?>> getKeys()
    {
        return new HashSet<>(Arrays.asList(LOCK_ID, LOCK_PASS));
    }

    @Override
    public Set<ImmutableValue<?>> getValues()
    {
        HashSet<ImmutableValue<?>> set = new HashSet<>();
        set.add(valueFactory.createValue(LOCK_ID, lockID).asImmutable());
        if (pass != null)
        {
            set.add((ImmutableValue<?>)valueFactory.createListValue(LOCK_PASS, passAsList()));
        }
        return set;
    }

}
