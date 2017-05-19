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
package org.cubeengine.module.locker.storage;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.UUID;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.database.AsyncRecord;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;

public class LockModel extends AsyncRecord<LockModel>
{
    public LockModel()
    {
        super(TABLE_LOCKS);
    }

    public LockModel newLock(User user, LockType lockType, ProtectedType type)
    {
        return this.newLock(user, lockType, type, null);
    }

    public LockModel newLock(User user, LockType lockType, ProtectedType type, UUID entityUUID)
    {
        this.setValue(TABLE_LOCKS.OWNER_ID, user.getUniqueId());
        this.setValue(TABLE_LOCKS.LOCK_TYPE, lockType.id);
        this.setValue(TABLE_LOCKS.FLAGS, (short)0); // none
        this.setValue(TABLE_LOCKS.PROTECTED_TYPE, type.id);
        if (entityUUID != null)
        {
            this.setValue(TABLE_LOCKS.ENTITY_UUID, entityUUID);
        }
        this.setValue(TABLE_LOCKS.LAST_ACCESS, new Timestamp(System.currentTimeMillis()));
        this.setValue(TABLE_LOCKS.CREATED, new Timestamp(System.currentTimeMillis()));
        return this;
    }

    private UUID uuid = null;

    public UUID getUUID()
    {
        if (this.uuid == null)
        {
            if (this.getValue(TABLE_LOCKS.ENTITY_UUID) == null) return null;
            this.uuid = this.getValue(TABLE_LOCKS.ENTITY_UUID);
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
                this.setValue(TABLE_LOCKS.PASSWORD, manager.messageDigest.digest(pass.getBytes()));
            }
        }
        else
        {
            this.setValue(TABLE_LOCKS.PASSWORD, StringUtils.randomString(new SecureRandom(), 4, "0123456789abcdefklmnor").getBytes());
        }
        return this;
    }
}
