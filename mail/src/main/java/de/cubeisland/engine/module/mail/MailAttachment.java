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
package de.cubeisland.engine.module.mail;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.module.mail.storage.Mail;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserAttachment;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.mail.storage.TableMail.TABLE_MAIL;

public class MailAttachment extends UserAttachment
{
    private List<Mail> mailbox = new ArrayList<>();

    public void loadMails()
    {
        this.mailbox = dsl().selectFrom(TABLE_MAIL).where(TABLE_MAIL.USERID.eq(getHolder().getEntity().getId())).fetch();
    }

    private DSLContext dsl()
    {
        return getModule().getModularity().provide(Database.class).getDSL();
    }

    public List<Mail> getMails()
    {
        if (this.mailbox.isEmpty())
        {
            this.loadMails();
        }
        return this.mailbox;
    }

    public List<Mail> getMailsFrom(CommandSender sender)
    {
        List<Mail> mails = new ArrayList<>();
        for (Mail mail : this.getMails())
        {
            UInteger senderId = UInteger.valueOf(0);
            if (sender instanceof User)
            {
                senderId = ((User)sender).getEntity().getId();
            }
            if (mail.getValue(TABLE_MAIL.SENDERID).equals(senderId))
            {
                mails.add(mail);
            }
        }
        return mails;
    }

    public void addMail(CommandSender from, String message)
    {
        this.getMails(); // load if not loaded
        Mail mail;
        if (from instanceof User)
        {
            mail = dsl().newRecord(TABLE_MAIL).newMail(getHolder(), ((User)from).getEntity().getId(), message);
        }
        else
        {
            mail = dsl().newRecord(TABLE_MAIL).newMail(getHolder(), null, message);
        }
        this.mailbox.add(mail);
        mail.insertAsync();
    }

    public int countMail()
    {
        return this.getMails().size();
    }

    public void clearMail()
    {
        dsl().delete(TABLE_MAIL).where(TABLE_MAIL.USERID.eq(getHolder().getEntity().getId())).execute();
        this.mailbox = new ArrayList<>();
    }

    public void clearMailFrom(CommandSender sender)
    {
        final List<Mail> mailsFrom = this.getMailsFrom(sender);
        this.mailbox.removeAll(mailsFrom);
        UInteger senderId = sender instanceof User ? ((User)sender).getEntity().getId() : UInteger.valueOf(0);
        dsl().delete(TABLE_MAIL).where(
            TABLE_MAIL.USERID.eq(getHolder().getEntity().getId()),
            TABLE_MAIL.SENDERID.eq(senderId)).execute();
    }
}
