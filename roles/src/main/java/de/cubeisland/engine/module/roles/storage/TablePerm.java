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
package de.cubeisland.engine.module.roles.storage;

import java.util.UUID;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.VARCHAR;

public class TablePerm extends Table<UserPermission>
{
    public static TablePerm TABLE_PERM;
    public final TableField<UserPermission, UUID> USER = createField("user", SQLDataType.UUID.length(255).nullable(false), this);
    public final TableField<UserPermission, String> CONTEXT = createField("world", VARCHAR.length(255).nullable(false), this);
    public final TableField<UserPermission, String> PERM = createField("perm", VARCHAR.length(255).nullable(false), this);
    public final TableField<UserPermission, Boolean> ISSET = createField("isSet", BOOLEAN.nullable(false), this);

    public TablePerm(String prefix, Database database)
    {
        super(prefix + "permission", new Version(1), database);
        this.setPrimaryKey(USER, CONTEXT, PERM);
        this.addFields(USER, CONTEXT, PERM, ISSET);
        TABLE_PERM = this;
    }

    @Override
    public Class<UserPermission> getRecordType()
    {
        return UserPermission.class;
    }
}
