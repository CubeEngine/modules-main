/*
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
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.sql.database.Table;
import org.cubeengine.module.sql.database.TableUpdateCreator;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.SQLException;

public class TableAccount extends Table<AccountModel> implements TableUpdateCreator<AccountModel>
{
    public static TableAccount TABLE_ACCOUNT;
    public final TableField<AccountModel, String> ID = createField("id", SQLDataType.VARCHAR.length(64).nullable(false), this);
    public final TableField<AccountModel, String> NAME = createField("name", SQLDataType.VARCHAR.length(64), this);
    public final TableField<AccountModel, Boolean> HIDDEN = createField("hidden", SQLDataType.BOOLEAN, this);
    public final TableField<AccountModel, Boolean> INVITE = createField("invite", SQLDataType.BOOLEAN, this);
    public final TableField<AccountModel, Boolean> IS_UUID = createField("is_uuid", SQLDataType.BOOLEAN, this);

    public TableAccount()
    {
        super(AccountModel.class, "conomy_account", new Version(2));
        this.setPrimaryKey(ID);
        this.addUniqueKey(ID);
        this.addFields(ID, NAME, HIDDEN, INVITE, IS_UUID);
        TABLE_ACCOUNT = this;
    }

    @Override
    public void update(Database database, Version dbVersion) throws SQLException
    {
        if (new Version(1).equals(dbVersion)) // Update to Version 2
        {
            // Remove mask;
            DSLContext dsl = database.getDSL();
            dsl.alterTable(TABLE_ACCOUNT).addColumn(HIDDEN, HIDDEN.getDataType()).execute();
            dsl.alterTable(TABLE_ACCOUNT).addColumn(INVITE, INVITE.getDataType()).execute();
            dsl.alterTable(TABLE_ACCOUNT).addColumn(IS_UUID, IS_UUID.getDataType()).execute();
            Field<Byte> maskField = DSL.field("mask", SQLDataType.TINYINT);
            dsl.update(TABLE_ACCOUNT)
                    .set(HIDDEN, DSL.field(maskField.bitAnd((byte) 1).eq((byte) 1)))
                    .set(INVITE, DSL.field(maskField.bitAnd((byte) 2).eq((byte) 2)))
                    .set(IS_UUID, DSL.field(maskField.bitAnd((byte) 4).eq((byte) 4)))
                    .execute();
            try {
                dsl.alterTable(TABLE_ACCOUNT).dropColumn(maskField).execute();
            } catch (DataAccessException e) {
                e.printStackTrace();
            }

        }
    }
}
