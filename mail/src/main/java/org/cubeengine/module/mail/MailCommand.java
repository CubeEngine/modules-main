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
package org.cubeengine.module.mail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.mail.storage.Mail;
import org.cubeengine.module.mail.storage.TableMail;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.task.TaskManager;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record1;
import org.jooq.types.UInteger;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.WHITE;

@Command(name = "mail", desc = "Manages your server mail.")
public class MailCommand extends ContainerCommand
{
    private final MailModule module;
    private final TaskManager taskManager;
    private final Database db;
    private I18n i18n;

    public MailCommand(MailModule module, TaskManager taskManager, Database db, I18n i18n)
    {
        super(module);
        this.module = module;
        this.taskManager = taskManager;
        this.db = db;
        this.i18n = i18n;
    }

    @Alias(value = "readmail")
    @Command(desc = "Reads your mail.")
    public void read(CommandSource context, @Optional CommandSource player)
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        if (sender == null)
        {
            if (player == null)
            {
                i18n.sendTranslated(context,  NEUTRAL, "Log into the game to check your mailbox!");

                return;
            }
            i18n.sendTranslated(context,  NEUTRAL, "If you wanted to look into other players mail use: {text:/mail spy} {input#player}.", player);
            i18n.sendTranslated(context,  NEGATIVE, "Otherwise be quiet!");
            return;
        }
        MailAttachment attachment = sender.attachOrGet(MailAttachment.class, module);
        if (attachment.countMail() == 0)
        {
            i18n.sendTranslated(context,  NEUTRAL, "You do not have any mail!");
            return;
        }
        List<Mail> mails;
        if (player == null) //get mails
        {
            mails = attachment.getMails();
        }
        else //Search for mail of that user
        {
            mails = attachment.getMailsFrom(player);
        }
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            i18n.sendTranslated(context,  NEUTRAL, "You do not have any mail from {user}.", player);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Your mail:");
        for (int i = 0; i < mails.size(); i++)
        {
            Mail mail = mails.get(i);
            context.sendMessage(Text.of("\n", WHITE, i + 1, ": ", mail.readMail()));
        }

    }

    @Alias(value = "spymail")
    @Command(desc = "Shows the mail of other players.")
    public void spy(CommandSource context, User player)
    {
        List<Mail> mails = player.attachOrGet(MailAttachment.class, module).getMails();
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            i18n.sendTranslated(context,  NEUTRAL, "{user} does not have any mail!", player);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Mail mail : mails)
        {
            i++;
            sb.append("\n").append(ChatFormat.WHITE).append(i).append(": ").append(mail.getValue(TableMail.TABLE_MAIL.MESSAGE));
        }
        i18n.sendTranslated(context,  NEUTRAL, "{user}'s mail: {input#mails}", player, ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(value = "sendmail")
    @Command(desc = "Sends mails to other players.")
    public void send(CommandSource context, User player, @Greed(INFINITE) String message)
    {
        this.mail(message, context, player);
        i18n.sendTranslated(context,  POSITIVE, "Mail send to {user}!", player);
    }

    @Alias(value = "sendallmail")
    @Command(desc = "Sends mails to all players.")
    public void sendAll(CommandSource context, final @Greed(INFINITE) String message)
    {
        Set<User> users = um.getOnlineUsers();
        final Set<UInteger> alreadySend = new HashSet<>();
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        for (User user : users)
        {
            user.attachOrGet(MailAttachment.class, module).addMail(sender, message);
            alreadySend.add(user.getEntity().getId());
        }
        final UInteger senderId = sender == null ? null : sender.getEntity().getId();
        taskManager.runAsynchronousTaskDelayed(this.module, () -> {
            DSLContext dsl = db.getDSL();

            Collection<Query> queries = dsl.select(TABLE_USER.KEY).from(TABLE_USER).where(TABLE_USER.KEY.notIn(
                alreadySend)).fetch()
                       .map(Record1::value1).stream()
                       .map(userKey -> dsl.insertInto(
                           TableMail.TABLE_MAIL, TableMail.TABLE_MAIL.MESSAGE, TableMail.TABLE_MAIL.USERID, TableMail.TABLE_MAIL.SENDERID)
                                          .values(message, userKey, senderId))
                       .collect(Collectors.toList());
            dsl.batch(queries).execute();
        }, 0);
        i18n.sendTranslated(context,  POSITIVE, "Sent mail to everyone!");
    }

    @Command(desc = "Removes a single mail")
    @Restricted(value = User.class, msg = "The console has no mails!")
    public void remove(User context, Integer mailId)
    {
        MailAttachment attachment = context.attachOrGet(MailAttachment.class, module);
        if (attachment.countMail() == 0)
        {
            i18n.sendTranslated(context,  NEUTRAL, "You do not have any mail!");
            return;
        }
        try
        {
            Mail mail = attachment.getMails().get(mailId);
            db.getDSL().delete(TableMail.TABLE_MAIL).where(TableMail.TABLE_MAIL.ID.eq(mail.getValue(
                TableMail.TABLE_MAIL.ID))).execute();
            i18n.sendTranslated(context,  POSITIVE, "Deleted Mail #{integer#mailid}", mailId);
        }
        catch (IndexOutOfBoundsException e)
        {
            i18n.sendTranslated(context,  NEGATIVE, "Invalid Mail Id!");
        }
    }

    @Command(desc = "Clears your mail.")
    @Restricted(value = User.class, msg = "You will never have mail here!")
    public void clear(User context, @Optional CommandSource player)
    {
        if (player == null)
        {
            context.attachOrGet(MailAttachment.class, module).clearMail();
            i18n.sendTranslated(context,  NEUTRAL, "Cleared all mails!");
            return;
        }
        context.attachOrGet(MailAttachment.class, module).clearMailFrom(player);
        i18n.sendTranslated(context,  NEUTRAL, "Cleared all mail from {user}!", player instanceof User ? player : "console");
    }

    private void mail(String message, CommandSource from, User... users)
    {
        for (User user : users)
        {
            user.attachOrGet(MailAttachment.class, module).addMail(from, message);
            if (user.asPlayer().isOnline())
            {
                user.sendTranslated(NEUTRAL, "You just got a mail from {user}!", from.getName());
            }
        }
    }
}
