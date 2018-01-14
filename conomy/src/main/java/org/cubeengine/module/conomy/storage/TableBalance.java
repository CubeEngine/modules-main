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
import org.cubeengine.module.sql.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;

public class TableBalance extends Table<BalanceModel>
{
    public static TableBalance TABLE_BALANCE;
    public final TableField<BalanceModel, String> ACCOUNT_ID = createField("id", SQLDataType.VARCHAR.length(64).nullable(false), this);
    public final TableField<BalanceModel, String> CURRENCY = createField("currency", SQLDataType.VARCHAR.length(64).nullable(false), this);
    public final TableField<BalanceModel, String> CONTEXT = createField("context", SQLDataType.VARCHAR.length(64).nullable(false), this);
    public final TableField<BalanceModel, Long> BALANCE = createField("balance", SQLDataType.BIGINT.nullable(false), this);

    public TableBalance()
    {
        super(BalanceModel.class, "conomy_balance", new Version(1));
        this.setPrimaryKey(ACCOUNT_ID, CURRENCY, CONTEXT);
        this.addFields(ACCOUNT_ID, CURRENCY, CONTEXT, BALANCE);
        this.addForeignKey(TABLE_ACCOUNT.getPrimaryKey(), ACCOUNT_ID);
        TABLE_BALANCE = this;
    }
}
