/*
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
package org.cubeengine.module.sql.database.impl;

import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;

public class Keys
{
    public static <R extends Record, T> Identity<R, T> identity(Table<R> table, TableField<R, T> field)
    {
        return Internal.createIdentity(table, field);
    }

    public static <R extends Record, U extends Record> ForeignKey<R, U> foreignKey(UniqueKey<U> key, Table<R> table, TableField <R, ?>... fields)
    {
        return Internal.createForeignKey(key, table, fields);
    }

    public static <R extends Record> UniqueKey<R> uniqueKey(Table<R> table, TableField<R, ?>... fields)
    {
        return Internal.createUniqueKey(table, fields);
    }
}
