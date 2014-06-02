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
package de.cubeisland.engine.module.locker.storage;

import java.sql.Timestamp;

import de.cubeisland.engine.core.storage.database.AutoIncrementTable;
import de.cubeisland.engine.core.util.Version;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;
import static org.jooq.impl.SQLDataType.*;
import static org.jooq.util.mysql.MySQLDataType.DATETIME;

public class TableLocks extends AutoIncrementTable<LockModel, UInteger>
{
    public static TableLocks TABLE_LOCK;
    public final TableField<LockModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<LockModel, UInteger> OWNER_ID = createField("owner_id", U_INTEGER.nullable(false), this);
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
    public final TableField<LockModel, Long> ENTITY_UID_LEAST = createField("entity_uid_least", BIGINT, this);
    public final TableField<LockModel, Long> ENTITY_UID_MOST = createField("entity_uid_most", BIGINT, this);
    public final TableField<LockModel, Timestamp> LAST_ACCESS = createField("last_access", DATETIME.nullable(false), this);
    public final TableField<LockModel, Timestamp> CREATED = createField("created", DATETIME.nullable(false), this);

    public TableLocks(String prefix)
    {
        super(prefix + "locks", new Version(1));
        this.setAIKey(ID);
        this.addUniqueKey(ENTITY_UID_LEAST, ENTITY_UID_MOST);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), OWNER_ID);
        this.addFields(ID, OWNER_ID, FLAGS, PROTECTED_TYPE, LOCK_TYPE, PASSWORD, ENTITY_UID_LEAST, ENTITY_UID_MOST,
                       LAST_ACCESS, CREATED);
        TABLE_LOCK = this;
    }

    @Override
    public Class<LockModel> getRecordType()
    {
        return LockModel.class;
    }
}