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
package org.cubeengine.module.roles.sponge.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.jooq.Result;
import org.jooq.TableField;

public class RecordBackedMap<K, V, RecordT extends AsyncRecord<RecordT>> implements Map<K, V>
{
    private Database db;
    private Table<RecordT> table;
    private final Result<RecordT> records;
    private final TableField<RecordT, K> keyType;
    private final TableField<RecordT, V> valueType;

    private final TableField<RecordT, UUID> userField;
    private final UUID userId;
    private final TableField<RecordT, String> contextField;
    private final String context;
    private final List<RecordT> removed = new CopyOnWriteArrayList<>();

    public RecordBackedMap(Database db, Table<RecordT> table,
                           TableField<RecordT, K> key, TableField<RecordT, V> value,
                           TableField<RecordT, UUID> userField, UUID userId,
                           TableField<RecordT, String> contextField, String context)
    {
        this.db = db;
        this.table = table;
        this.userField = userField;
        this.userId = userId;
        this.contextField = contextField;
        this.context = context;
        this.records = db.getDSL().selectFrom(table).where(userField.eq(userId), contextField.eq(context)).fetch();;
        this.keyType = key;
        this.valueType = value;
    }

    @Override
    public int size()
    {
        return records.size();
    }

    @Override
    public boolean isEmpty()
    {
        return records.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        for (RecordT record : records)
        {
            K recordKey = record.getValue(keyType);
            if (key.equals(recordKey))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value)
    {
        for (RecordT record : records)
        {
            V recordValue = record.getValue(valueType);
            if (value.equals(recordValue))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key)
    {
        for (RecordT record : records)
        {
            K recordKey = record.getValue(keyType);
            if (key.equals(recordKey))
            {
                return record.getValue(valueType);
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value)
    {
        if (value == null)
        {
            return remove(key);
        }
        for (RecordT record : records)
        {
            K recordKey = record.getValue(keyType);
            if (key.equals(recordKey))
            {
                V oldValue = record.getValue(valueType);
                record.setValue(valueType, value);
                return oldValue;
            }
        }

        RecordT recordT = db.getDSL().newRecord(table);
        recordT.setValue(userField, userId);
        recordT.setValue(contextField, context);
        recordT.setValue(keyType, key);
        recordT.setValue(valueType, value);
        records.add(recordT);
        return null;
    }

    @Override
    public V remove(Object key)
    {
        RecordT found = null;
        for (RecordT record : records)
        {
            K recordKey = record.getValue(keyType);
            if (key.equals(recordKey))
            {
                found = record;
            }
        }
        if (found != null)
        {
            removed.add(found);
            records.remove(found);
            return found.getValue(valueType);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        for (Entry<? extends K, ? extends V> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear()
    {
        removed.addAll(records);
        records.clear();
    }

    @Override
    public Set<K> keySet()
    {
        Set<K> set = new HashSet<>();
        for (RecordT record : records)
        {
            set.add(record.getValue(keyType));
        }
        return set;
    }

    @Override
    public Collection<V> values()
    {
        Collection<V> list = new ArrayList<>();
        for (RecordT record : records)
        {
            list.add(record.getValue(valueType));
        }
        return list;
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        Set<Entry<K, V>> entries = new HashSet<>();
        for (RecordT record : records)
        {
            entries.add(new Entry<K, V>()
            {
                @Override
                public K getKey()
                {
                    return record.getValue(keyType);
                }

                @Override
                public V getValue()
                {
                    return record.getValue(valueType);
                }

                @Override
                public V setValue(V value)
                {
                    V oldValue = getValue();
                    record.setValue(valueType, value);
                    return oldValue;
                }
            });
        }
        return entries;
    }


    public void save()
    {
        records.forEach(RecordT::store); // TODO async
        removed.forEach(RecordT::deleteAsync);
        removed.clear();
    }
}
