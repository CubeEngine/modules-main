package de.cubeisland.engine.module.roles.sponge.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import de.cubeisland.engine.module.roles.sponge.collection.RoleCollection;
import de.cubeisland.engine.module.roles.storage.AssignedRole;
import de.cubeisland.engine.module.roles.storage.TableRole;
import de.cubeisland.engine.module.service.database.AsyncRecord;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.module.service.database.Table;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.types.UInteger;
import org.spongepowered.api.service.permission.Subject;

import static java.util.stream.Collectors.toList;

public class RecordBackedList<RecordT extends AsyncRecord> implements List<Subject>
{
    private final Result<RecordT> records;
    private final List<RecordT> removed = new CopyOnWriteArrayList<>();
    private RoleCollection collection;
    private final Database db;
    private final Table<RecordT> table;
    private final TableField<RecordT, String> type;
    private final TableField<RecordT, UUID> userField;
    private final UUID uuid;
    private final TableField<RecordT, String> contextField;
    private final String context;

    public RecordBackedList(RoleCollection collection, Database db, Table<RecordT> table, TableField<RecordT, String> type,
                            TableField<RecordT, UUID> userField, UUID uuid, TableField<RecordT, String> contextField,
                            String context)
    {
        this.collection = collection;
        this.db = db;
        this.table = table;
        this.type = type;
        this.userField = userField;
        this.uuid = uuid;
        this.contextField = contextField;
        this.context = context;
        this.records = db.getDSL().selectFrom(table).where(userField.eq(uuid), contextField.eq(context)).fetch();
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
    public boolean contains(Object o)
    {
        for (RecordT record : records)
        {
            if (o.equals(collection.get(record.getValue(type))))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Subject> iterator()
    {
        return records.stream().map(r -> (Subject)collection.get(r.getValue(type))).iterator();
    }

    @Override
    public Object[] toArray()
    {
        return records.stream().map(r -> r.getValue(type)).toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a)
    {
        if (a.length < size()) // Make a new array of a's runtime type, but my contents:
        {
            return (T[])Arrays.copyOf(toArray(), size(), a.getClass());
        }
        System.arraycopy(toArray(), 0, a, 0, size());
        if (a.length > size())
        {
            a[size()] = null;
        }
        return a;
    }

    @Override
    public boolean add(Subject v)
    {
        records.add(newRecord(v));
        return false;
    }

    private RecordT newRecord(Subject v)
    {
        RecordT recordT = db.getDSL().newRecord(table);
        recordT.setValue(userField, uuid);
        recordT.setValue(contextField, context);
        recordT.setValue(type, v.getIdentifier());
        return recordT;
    }

    @Override
    public boolean remove(Object o)
    {
        RecordT found = null;
        for (RecordT record : records)
        {
            if (o.equals(collection.get(record.getValue(type))))
            {
                found = record;
            }
        }
        if (found != null)
        {
            removed.add(found);
            records.remove(found);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        for (Object o : c)
        {
            if (!contains(o))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Subject> c)
    {
        boolean changed = false;
        for (Subject v : c)
        {
            if (add(v))
            {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Subject> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean changed = false;
        for (Object o : c)
        {
            if (remove(o))
            {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        removed.addAll(records);
        records.clear();
    }

    @Override
    public Subject get(int index)
    {
        return collection.get(records.get(index).getValue(type));
    }

    @Override
    public Subject set(int index, Subject element)
    {
        RecordT replaced = records.set(index, newRecord(element));
        removed.add(replaced);
        return collection.get(replaced.getValue(type));
    }

    @Override
    public void add(int index, Subject element)
    {
        records.add(index, newRecord(element));
    }

    @Override
    public Subject remove(int index)
    {
        RecordT remove = records.remove(index);
        removed.add(remove);
        return collection.get(remove.getValue(type));
    }

    @Override
    public int indexOf(Object o)
    {
        for (int i = 0; i < records.size(); i++)
        {
            final RecordT record = records.get(i);
            if (o.equals(record.getValue(type)))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return indexOf(o);
    }

    @Override
    public ListIterator<Subject> listIterator()
    {
        return records.stream().map(r -> (Subject)collection.get(r.getValue(type))).collect(toList()).listIterator();
    }

    @Override
    public ListIterator<Subject> listIterator(int index)
    {
        return records.stream().map(r -> (Subject)collection.get(r.getValue(type))).collect(toList()).listIterator(index);
    }

    @Override
    public List<Subject> subList(int fromIndex, int toIndex)
    {
        return records.stream().map(r -> (Subject)collection.get(r.getValue(type))).collect(toList()).subList(fromIndex, toIndex);
    }

    public void save()
    {
        records.forEach(RecordT::updateAsync);
        removed.forEach(RecordT::deleteAsync);
    }
}
