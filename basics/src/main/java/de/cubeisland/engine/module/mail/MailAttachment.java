package de.cubeisland.engine.module.mail;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.module.mail.storage.Mail;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserAttachment;
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
        return getModule().getModularity().start(Database.class).getDSL();
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
            long senderId = 0;
            if (sender instanceof User)
            {
                senderId = ((User)sender).getId();
            }
            if (mail.getValue(TABLE_MAIL.SENDERID).longValue() == senderId)
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
