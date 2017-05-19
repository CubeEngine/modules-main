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
package org.cubeengine.module.locker.storage;

import java.sql.Timestamp;
import java.util.UUID;
import org.cubeengine.libcube.util.Version;
import org.cubeengine.libcube.service.database.AutoIncrementTable;
import org.cubeengine.libcube.service.database.Database;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;

import static org.jooq.impl.SQLDataType.*;
import static org.jooq.util.mysql.MySQLDataType.DATETIME;

public class TableLocks extends AutoIncrementTable<LockModel, UInteger>
{
    public static TableLocks TABLE_LOCKS;
    public final TableField<LockModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<LockModel, UUID> OWNER_ID = createField("owner_id", SQLDataType.UUID.length(36).nullable(false), this);
    /**
     * Flags see {@link ProtectionFlag}
     */
    public final TableField<LockModel, Short> FLAGS = createField("flags", SMALLINT.nullable(false), this);
    /**
     * Protected Type see {@link ProtectedType}
     */
    public final TableField<LockModel, Byte> PROTECTED_TYPE = createField("type", TINYINT.nullable(false), this);
    /**
     * LockType see {@link LockType}
     */
    public final TableField<LockModel, Byte> LOCK_TYPE = createField("lock_type", TINYINT.nullable(false), this);
    // eg. /cguarded [pass <password>] (flag to create pw book/key?)
    public final TableField<LockModel, byte[]> PASSWORD = createField("password", VARBINARY.length(128).nullable(false), this);
    // optional for entity protection:
    public final TableField<LockModel, UUID> ENTITY_UUID = createField("entity_uuid", SQLDataType.UUID.length(36), this);
    public final TableField<LockModel, Timestamp> LAST_ACCESS = createField("last_access", DATETIME.nullable(false), this);
    public final TableField<LockModel, Timestamp> CREATED = createField("created", DATETIME.nullable(false), this);

    public TableLocks(String prefix, Database db)
    {
        super(prefix + "locker_locks", new Version(1), db);
        this.setAIKey(ID);
        this.addUniqueKey(ENTITY_UUID);
        this.addFields(ID, OWNER_ID, FLAGS, PROTECTED_TYPE, LOCK_TYPE, PASSWORD, ENTITY_UUID, LAST_ACCESS, CREATED);
        TABLE_LOCKS = this;
    }

    @Override
    public Class<LockModel> getRecordType()
    {
        return LockModel.class;
    }
}
