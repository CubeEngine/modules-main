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
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.cubeengine.module.core.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableOption extends Table<UserOption>
{
    public static TableOption TABLE_META;
    public final TableField<UserOption, UUID> USER = createField("userId", SQLDataType.UUID.length(255).nullable(false),
                                                                 this);
    public final TableField<UserOption, String> CONTEXT = createField("worldId", VARCHAR.length(255).nullable(false),
                                                                      this);
    public final TableField<UserOption, String> KEY = createField("key", VARCHAR.length(255).nullable(false), this);
    public final TableField<UserOption, String> VALUE = createField("value", VARCHAR.length(255).nullable(false), this);

    public TableOption(String prefix, Database database)
    {
        super(prefix + "option", new Version(1), database);
        this.setPrimaryKey(USER, CONTEXT, KEY);
        this.addFields(USER, CONTEXT, KEY, VALUE);
        TABLE_META = this;
    }

    @Override
    public Class<UserOption> getRecordType()
    {
        return UserOption.class;
    }
}
