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

import de.cubeisland.engine.module.service.database.AsyncRecord;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserEntity;

import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;

public class AccountModel extends AsyncRecord<AccountModel>
{
    public AccountModel()
    {
        super(TABLE_ACCOUNT);
    }

    public AccountModel newAccount(User user, String name, long balance, boolean hidden, boolean needsInvite)
    {
        this.setUser(user.getEntity());
        this.setValue(TABLE_ACCOUNT.NAME, name);
        this.setValue(TABLE_ACCOUNT.VALUE, balance);
        this.setValue(TABLE_ACCOUNT.MASK, (byte)((hidden ? 1 : 0) + (needsInvite ? 2 : 0)));
        return this;
    }

    public void setUser(UserEntity user)
    {
        this.setValue(TABLE_ACCOUNT.USER_ID, user.getId());
    }

    public AccountModel newAccount(User user, String name, long balance, boolean hidden)
    {
        return this.newAccount(user, name, balance, hidden, false);
    }

    public boolean needsInvite()
    {
        return (this.getValue(TABLE_ACCOUNT.MASK) & 2) == 2;
    }

    public boolean isHidden()
    {
        return (this.getValue(TABLE_ACCOUNT.MASK) & 1) == 1;
    }

    public void setNeedsInvite(boolean set)
    {
        byte mask = this.getValue(TABLE_ACCOUNT.MASK);
        if (set)
        {
            mask |= 2;
        }
        else
        {
            mask &= ~2;
        }
        this.setValue(TABLE_ACCOUNT.MASK, mask);
    }

    public void setHidden(boolean set)
    {
        byte mask = this.getValue(TABLE_ACCOUNT.MASK);
        if (set)
        {
            mask |= 1;
        }
        else
        {
            mask &= ~1;
        }
        this.setValue(TABLE_ACCOUNT.MASK, mask);
    }
}
