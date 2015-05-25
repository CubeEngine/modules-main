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
package de.cubeisland.engine.module.basics.storage;

import de.cubeisland.engine.module.service.database.AsyncRecord;
import de.cubeisland.engine.module.service.user.User;

import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;

public class BasicsUserEntity extends AsyncRecord<BasicsUserEntity>
{
    public BasicsUserEntity()
    {
        super(TABLE_BASIC_USER);
    }

    public BasicsUserEntity newBasicUser(User user)
    {
        this.setValue(TABLE_BASIC_USER.KEY, user.getEntity().getKey());
        this.setValue(TABLE_BASIC_USER.GODMODE, false);
        return this;
    }
}
