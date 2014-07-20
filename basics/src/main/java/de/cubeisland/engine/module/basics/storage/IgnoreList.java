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

import de.cubeisland.engine.core.user.User;
import org.jooq.impl.UpdatableRecordImpl;

import static de.cubeisland.engine.module.basics.storage.TableIgnorelist.TABLE_IGNORE_LIST;

public class IgnoreList extends UpdatableRecordImpl<IgnoreList>
{
    public IgnoreList()
    {
        super(TABLE_IGNORE_LIST);
    }

    public IgnoreList newIgnore(User user, User ignore)
    {
        this.setValue(TABLE_IGNORE_LIST.KEY, user.getEntity().getKey());
        this.setValue(TABLE_IGNORE_LIST.KEY, ignore.getEntity().getKey());
        return this;
    }
}
