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

import javax.persistence.Entity;
import javax.persistence.Table;

import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.storage.database.AsyncRecord;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.basics.storage.TableMail.TABLE_MAIL;

@Entity
@Table(name = "mail")
public class Mail extends AsyncRecord<Mail>
{
    public Mail()
    {
        super(TABLE_MAIL);
    }

    public Mail newMail(BasicsUserEntity userId, UInteger senderId, String message)
    {
        this.setValue(TABLE_MAIL.MESSAGE, message);
        this.setValue(TABLE_MAIL.USERID, userId.getValue(TABLE_BASIC_USER.KEY));
        this.setValue(TABLE_MAIL.SENDERID, senderId);
        return this;
    }

    public String readMail()
    {
        UInteger value = this.getValue(TABLE_MAIL.SENDERID);
        if (value == null || value.longValue() == 0)
        {
            return ChatFormat.RED + "CONSOLE" + ChatFormat.WHITE + ": " + this.getValue(TABLE_MAIL.MESSAGE);
        }
        User user = CubeEngine.getUserManager().getUser(this.getValue(TABLE_MAIL.SENDERID));
        return ChatFormat.DARK_GREEN + user.getDisplayName() + ChatFormat.WHITE + ": " + this.getValue(TABLE_MAIL.MESSAGE);
    }
}
