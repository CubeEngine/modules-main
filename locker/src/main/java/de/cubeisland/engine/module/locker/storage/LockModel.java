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
package de.cubeisland.engine.module.locker.storage;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.UUID;
import de.cubeisland.engine.module.service.database.AsyncRecord;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.StringUtils;

import static de.cubeisland.engine.module.locker.storage.TableLocks.TABLE_LOCK;

public class LockModel extends AsyncRecord<LockModel>
{
    public LockModel()
    {
        super(TABLE_LOCK);
    }

    public LockModel newLock(User user, LockType lockType, ProtectedType type)
    {
        return this.newLock(user, lockType, type, null);
    }

    public LockModel newLock(User user, LockType lockType, ProtectedType type, UUID entityUUID)
    {
        this.setValue(TABLE_LOCK.OWNER_ID, user.getEntity().getId());
        this.setValue(TABLE_LOCK.LOCK_TYPE, lockType.id);
        this.setValue(TABLE_LOCK.FLAGS, (short)0); // none
        this.setValue(TABLE_LOCK.PROTECTED_TYPE, type.id);
        if (entityUUID != null)
        {
            this.setValue(TABLE_LOCK.ENTITY_UID_LEAST, entityUUID.getLeastSignificantBits());
            this.setValue(TABLE_LOCK.ENTITY_UID_MOST, entityUUID.getMostSignificantBits());
        }
        this.setValue(TABLE_LOCK.LAST_ACCESS, new Timestamp(System.currentTimeMillis()));
        this.setValue(TABLE_LOCK.CREATED, new Timestamp(System.currentTimeMillis()));
        return this;
    }

    private UUID uuid = null;

    public String getColorPass()
    {
        StringBuilder builder = new StringBuilder();
        String stringPass;
        byte[] pass = this.getValue(TABLE_LOCK.PASSWORD);
        if (pass.length != 4)
        {
            for (byte b : pass)
            {
                builder.append(String.format("%02X", b));
            }
            stringPass = builder.toString();
            builder = new StringBuilder();
        }
        else
        {
            stringPass = new String(pass);
        }
        for (char c : stringPass.toCharArray())
        {
            builder.append("&").append(c);
        }
        return ChatFormat.parseFormats(builder.toString());
    }

    public UUID getUUID()
    {
        if (this.uuid == null)
        {
            if (this.getValue(TABLE_LOCK.ENTITY_UID_LEAST) == null) return null;
            this.uuid = new UUID(this.getValue(TABLE_LOCK.ENTITY_UID_MOST), this.getValue(TABLE_LOCK.ENTITY_UID_LEAST));
        }
        return this.uuid;
    }

    /**
     * Sets a new password for given lock-model
     *
     * @param manager
     * @param pass
     *
     * @return fluent interface
     */
    protected LockModel createPassword(LockManager manager, String pass)
    {
        if (pass != null)
        {
            synchronized (manager.messageDigest)
            {
                manager.messageDigest.reset();
                this.setValue(TABLE_LOCK.PASSWORD, manager.messageDigest.digest(pass.getBytes()));
            }
        }
        else
        {
            this.setValue(TABLE_LOCK.PASSWORD, StringUtils.randomString(new SecureRandom(), 4, "0123456789abcdefklmnor").getBytes());
        }
        return this;
    }
}
