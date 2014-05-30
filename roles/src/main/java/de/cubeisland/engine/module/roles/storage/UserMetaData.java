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

import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.roles.storage.TableData.TABLE_META;

public class UserMetaData extends UpdatableRecordImpl<UserMetaData>
{
    public UserMetaData()
    {
        super(TABLE_META);
    }

    public UserMetaData newMeta(UInteger userId, UInteger worldId, String key, String value)
    {
        this.setValue(TABLE_META.USERID, userId);
        this.setValue(TABLE_META.WORLDID, worldId);
        this.setValue(TABLE_META.KEY, key);
        this.setValue(TABLE_META.VALUE, value);
        return this;
    }
}
