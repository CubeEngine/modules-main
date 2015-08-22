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
package org.cubeengine.module.roles.storage;

import java.util.UUID;
import org.cubeengine.service.database.AsyncRecord;

public class UserRole extends AsyncRecord<UserRole>
{
    public UserRole()
    {
        super(TableRole.TABLE_ROLE);
    }

    public UserRole newAssignedRole(UUID uuid, String context, String roleName)
    {
        this.setValue(TableRole.TABLE_ROLE.USER, uuid);
        this.setValue(TableRole.TABLE_ROLE.CONTEXT, context);
        this.setValue(TableRole.TABLE_ROLE.ROLE, roleName);
        return this;
    }
}
