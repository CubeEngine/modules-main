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

import org.cubeengine.service.database.AutoIncrementTable;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.Database;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import static org.cubeengine.service.world.TableWorld.TABLE_WORLD;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCK;
import static org.jooq.impl.SQLDataType.INTEGER;

public class TableLockLocations extends AutoIncrementTable<LockLocationModel, UInteger>
{
    public static TableLockLocations TABLE_LOCK_LOCATION;
    public final TableField<LockLocationModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, UInteger> WORLD_ID = createField("world_id", U_INTEGER.nullable(false),this);
    public final TableField<LockLocationModel, Integer> X = createField("x", INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, Integer> Y = createField("y", INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, Integer> Z = createField("z", INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, Integer> CHUNKX = createField("chunkX", INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, Integer> CHUNKZ = createField("chunkZ", INTEGER.nullable(false), this);
    public final TableField<LockLocationModel, UInteger> LOCK_ID = createField("lock_id", U_INTEGER.nullable(false),this);

    public TableLockLocations(String prefix, Database db)
    {
        super(prefix + "locklocation", new Version(1), db);
        this.setAIKey(ID);
        this.addIndex(CHUNKX, CHUNKZ);
        this.addUniqueKey(WORLD_ID, X, Y, Z);
        this.addForeignKey(TABLE_WORLD.getPrimaryKey(), WORLD_ID);
        this.addForeignKey(TABLE_LOCK.getPrimaryKey(), LOCK_ID);
        this.addFields(ID, WORLD_ID, X, Y, Z, CHUNKX, CHUNKZ, LOCK_ID);
        TABLE_LOCK_LOCATION = this;
    }

    @Override
    public Class<LockLocationModel> getRecordType()
    {
        return LockLocationModel.class;
    }
}
