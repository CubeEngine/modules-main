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
package org.cubeengine.module.conomy.storage;

import org.cubeengine.libcube.util.Version;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

public class TableAccount extends Table<AccountModel>
{
    public static TableAccount TABLE_ACCOUNT;
    public final TableField<AccountModel, String> ID = createField("id", SQLDataType.VARCHAR.length(64).nullable(false), this);
    public final TableField<AccountModel, String> NAME = createField("name", SQLDataType.VARCHAR.length(64), this);
    public final TableField<AccountModel, Byte> MASK = createField("mask", SQLDataType.TINYINT, this);

    public TableAccount(String prefix, Database database)
    {
        super(prefix + "conomy_account", new Version(1), database);
        this.setPrimaryKey(ID);
        this.addUniqueKey(ID);
        this.addFields(ID, NAME, MASK);
        TABLE_ACCOUNT = this;
    }

    @Override
    public Class<AccountModel> getRecordType()
    {
        return AccountModel.class;
    }
}
