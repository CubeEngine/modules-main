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
package org.cubeengine.module.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.cubeengine.module.mail.storage.Mail;
import org.cubeengine.libcube.service.database.Database;
import org.jooq.DSLContext;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.module.mail.storage.TableMail.TABLE_MAIL;

public class PlayerMails
{
    private UUID player;
    private Database db;

    public PlayerMails(UUID player, Database db)
    {
        this.player = player;
        this.db = db;
    }

    private List<Mail> mailbox = new ArrayList<>();

    public void loadMails()
    {
        this.mailbox = dsl().selectFrom(TABLE_MAIL)
                        .where(TABLE_MAIL.USERID.eq(player)).fetch();
    }

    private DSLContext dsl()
    {
        return db.getDSL();
    }

    public List<Mail> getMails()
    {
        if (this.mailbox.isEmpty())
        {
            this.loadMails();
        }
        return this.mailbox;
    }

    public List<Mail> getMailsFrom(CommandSource sender)
    {
        List<Mail> mails = new ArrayList<>();
        for (Mail mail : this.getMails())
        {
            UUID senderId = null;
            if (sender instanceof Player)
            {
                senderId = ((Player)sender).getUniqueId();
            }
            if (mail.getValue(TABLE_MAIL.SENDERID).equals(senderId))
            {
                mails.add(mail);
            }
        }
        return mails;
    }

    public void addMail(CommandSource from, String message)
    {
        this.getMails(); // load if not loaded
        Mail mail;
        if (from instanceof Player)
        {
            mail = dsl().newRecord(TABLE_MAIL).newMail(player, ((Player)from).getUniqueId(), message);
        }
        else
        {
            mail = dsl().newRecord(TABLE_MAIL).newMail(player, null, message);
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
        dsl().delete(TABLE_MAIL).where(TABLE_MAIL.USERID.eq(player)).execute();
        this.mailbox = new ArrayList<>();
    }

    public void clearMailFrom(CommandSource sender)
    {
        final List<Mail> mailsFrom = this.getMailsFrom(sender);
        this.mailbox.removeAll(mailsFrom);
        UUID senderId = sender instanceof Player ? ((Player)sender).getUniqueId() : null;
        dsl().delete(TABLE_MAIL).where(
            TABLE_MAIL.USERID.eq(player),
            TABLE_MAIL.SENDERID.eq(senderId)).execute();
    }
}
