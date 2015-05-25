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

import de.cubeisland.engine.module.service.database.AsyncRecord;
import de.cubeisland.engine.module.roles.role.DataStore.PermissionValue;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.roles.role.DataStore.PermissionValue.TRUE;
import static de.cubeisland.engine.module.roles.storage.TablePerm.TABLE_PERM;

public class UserPermission extends AsyncRecord<UserPermission>
{
    public UserPermission()
    {
        super(TABLE_PERM);
    }

    public UserPermission newPerm(UInteger userId, UInteger worldId, String perm, PermissionValue set)
    {
        this.setValue(TABLE_PERM.USERID, userId);
        this.setValue(TABLE_PERM.WORLDID, worldId);
        this.setValue(TABLE_PERM.PERM, perm);
        this.setValue(TABLE_PERM.ISSET, set == TRUE);
        return this;
    }
}
