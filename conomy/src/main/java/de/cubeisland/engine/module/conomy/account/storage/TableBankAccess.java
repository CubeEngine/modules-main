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
package de.cubeisland.engine.module.conomy.account.storage;

import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.AutoIncrementTable;
import org.cubeengine.service.database.Database;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;

import static org.cubeengine.service.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;

public class TableBankAccess extends AutoIncrementTable<BankAccessModel, UInteger>
{
    public static TableBankAccess TABLE_BANK_ACCESS;

    public TableBankAccess(String prefix, Database database)
    {
        super(prefix + "account_access", new Version(1), database);
        this.setAIKey(ID);
        this.addUniqueKey(USERID, ACCOUNTID);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), USERID);
        this.addForeignKey(TABLE_ACCOUNT.getPrimaryKey(), ACCOUNTID);
        this.addFields(ID, USERID, ACCOUNTID, ACCESSLEVEL);
        TABLE_BANK_ACCESS = this;
    }

    public final TableField<BankAccessModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<BankAccessModel, UInteger> USERID = createField("userId", U_INTEGER.nullable(false), this);
    public final TableField<BankAccessModel, UInteger> ACCOUNTID = createField("accountId", U_INTEGER.nullable(false), this);
    public final TableField<BankAccessModel, Byte> ACCESSLEVEL = createField("accessLevel", SQLDataType.TINYINT.nullable(false), this);

    @Override
    public Class<BankAccessModel> getRecordType() {
        return BankAccessModel.class;
    }
}
