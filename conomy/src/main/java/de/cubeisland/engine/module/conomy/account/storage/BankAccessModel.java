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

import de.cubeisland.engine.service.database.AsyncRecord;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserEntity;

import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;
import static de.cubeisland.engine.module.conomy.account.storage.TableBankAccess.TABLE_BANK_ACCESS;

public class BankAccessModel extends AsyncRecord<BankAccessModel>
{
    public enum AccessLevel
    {
        OWNER(1), MEMBER(2), INVITED(4);
        public final byte value;

        AccessLevel(int value)
        {
            this.value = (byte)value;
        }
    }

    public BankAccessModel()
    {
        super(TABLE_BANK_ACCESS);
    }

    public BankAccessModel newAccess(AccountModel accountModel, User user, AccessLevel level)
    {
        this.setUser(user.getEntity());
        this.setValue(TABLE_BANK_ACCESS.ACCOUNTID, accountModel.getValue(TABLE_ACCOUNT.KEY));
        this.setAccessLevel(level);
        return this;
    }

    public void setUser(UserEntity userEntity)
    {
        this.setValue(TABLE_BANK_ACCESS.USERID, userEntity.getId());
    }

    public void setAccessLevel(AccessLevel level)
    {
        this.setValue(TABLE_BANK_ACCESS.ACCESSLEVEL, level.value);
    }

    public AccessLevel getAccessLevel()
    {
        switch (this.getValue(TABLE_BANK_ACCESS.ACCESSLEVEL))
        {
            case 1:
                return AccessLevel.OWNER;
            case 2:
                return AccessLevel.MEMBER;
            case 4:
                return AccessLevel.INVITED;
        }
        return null;
    }
}
