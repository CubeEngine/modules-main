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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LockerData extends AbstractData<LockerData, ImmutableLockerData>
{
    public static Key<Value<Long>> LOCK_ID = KeyFactory.makeSingleKey(Long.class, Value.class, DataQuery.of("LockID"));
    public static Key<ListValue<Byte>> LOCK_PASS = KeyFactory.makeListKey(Byte.class, DataQuery.of("LockPass"));

    private long lockID;
    private byte[] pass;
    private ValueFactory valueFactory;

    public LockerData(long lockID, byte[] pass, ValueFactory valueFactory)
    {
        this.lockID = lockID;
        this.pass = pass;
        this.valueFactory = valueFactory;
    }

    @Override
    protected void registerGettersAndSetters()
    {
        registerFieldGetter(LOCK_ID, LockerData.this::getLockID);
        registerFieldSetter(LOCK_ID, LockerData.this::setLockID);
        registerKeyValue(LOCK_ID, LockerData.this::lockid);

        registerFieldGetter(LOCK_PASS, LockerData.this::getPass);
        registerFieldSetter(LOCK_PASS, LockerData.this::setPass);
        registerKeyValue(LOCK_PASS, LockerData.this::pass);
    }

    private void setPass(List<Byte> bytes)
    {
        List<Byte> list = bytes;
        this.pass = new byte[list.size()];
        for (int i = 0; i < list.size(); i++)
        {
            this.pass[i] = list.get(i);
        }
    }

    public ListValue<Byte> pass()
    {
        return Sponge.getRegistry().getValueFactory().createListValue(LOCK_PASS, getPass());
    }

    public List<Byte> getPass()
    {
        if (pass == null || pass.length == 0)
        {
            return Collections.emptyList();
        }
        return passAsList();
    }

    public Value<Long> lockid()
    {
        return Sponge.getRegistry().getValueFactory().createValue(LOCK_ID, lockID);
    }

    public void setLockID(Long id)
    {
        this.lockID = id;
    }

    public long getLockID()
    {
        return lockID;
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
        DataContainer container = super.toContainer();
        container.set(LOCK_ID, this.lockID);
        if (pass == null || pass.length == 0)
        {
            return container;
        }
        List<Byte> pass = passAsList();
        return container.set(LOCK_PASS, pass);
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
    public int getContentVersion()
    {
        return 1;
    }
}
