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

import java.util.UUID;
import de.cubeisland.engine.service.database.AsyncRecord;

import static de.cubeisland.engine.module.roles.storage.TableOption.TABLE_META;

public class UserOption extends AsyncRecord<UserOption>
{
    public UserOption()
    {
        super(TABLE_META);
    }

    public UserOption newMeta(UUID uuid, String context, String key, String value)
    {
        this.setValue(TABLE_META.USER, uuid);
        this.setValue(TABLE_META.CONTEXT, context);
        this.setValue(TABLE_META.KEY, key);
        this.setValue(TABLE_META.VALUE, value);
        return this;
    }
}
