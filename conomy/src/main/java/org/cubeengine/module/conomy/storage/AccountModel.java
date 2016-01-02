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

import org.cubeengine.service.database.AsyncRecord;

import java.util.Optional;
import java.util.UUID;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;

public class AccountModel extends AsyncRecord<AccountModel>
{
    public AccountModel()
    {
        super(TABLE_ACCOUNT);
    }

    public AccountModel newAccount(UUID uuid, String name, boolean hidden, boolean needsInvite)
    {
        return newAccount(uuid.toString(), name, hidden, needsInvite, true);
    }

    public AccountModel newAccount(String name, boolean hidden, boolean needsInvite)
    {
        return newAccount(name, name, hidden, needsInvite, false);
    }

    private AccountModel newAccount(String id, String name, boolean hidden, boolean needsInvite, boolean isUUID)
    {
        this.setValue(TABLE_ACCOUNT.ID, id);
        this.setValue(TABLE_ACCOUNT.NAME, name);
        this.setValue(TABLE_ACCOUNT.MASK, (byte)((hidden ? 1 : 0) + (needsInvite ? 2 : 0) + (isUUID ? 4 : 0)));
        return this;
    }

    public boolean isUUID()
    {
        return (this.getValue(TABLE_ACCOUNT.MASK) & 4) == 4;
    }

    public boolean isInvite()
    {
        return (this.getValue(TABLE_ACCOUNT.MASK) & 2) == 2;
    }

    public boolean isHidden()
    {
        return (this.getValue(TABLE_ACCOUNT.MASK) & 1) == 1;
    }

    public void setInvite(boolean set)
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
        this.updateAsync();
    }

    public Optional<UUID> getUUID()
    {
        if (isUUID())
        {
            return Optional.of(UUID.fromString(getID()));
        }
        return Optional.empty();
    }

    public String getID()
    {
        return getValue(TABLE_ACCOUNT.ID);
    }

    public String getName()
    {
        return getValue(TABLE_ACCOUNT.NAME);
    }

    public boolean setName(String newName)
    {
        setValue(TABLE_ACCOUNT.NAME, newName);
        // TODO check if name exists
        return true;
    }
}
