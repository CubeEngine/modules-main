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
package org.cubeengine.module.mail.storage;

import java.util.UUID;

import org.cubeengine.libcube.util.Version;
import org.cubeengine.module.sql.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.BIGINT;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableMail extends Table<Mail>
{
    public static TableMail TABLE_MAIL;
    public final TableField<Mail, Long> ID = createField("id", BIGINT.nullable(false).identity(true), this);
    public final TableField<Mail, String> MESSAGE = createField("message", VARCHAR.length(100).nullable(false), this);
    public final TableField<Mail, UUID> USERID = createField("userId", SQLDataType.UUID.nullable(false), this);
    public final TableField<Mail, UUID> SENDERID = createField("senderId", SQLDataType.UUID, this);

    public TableMail()
    {
        super(Mail.class, "mail", new Version(1));
        this.setPrimaryKey(ID);
        this.addFields(ID, MESSAGE, USERID, SENDERID);
        TABLE_MAIL = this;
    }
}
