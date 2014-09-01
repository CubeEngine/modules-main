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
package de.cubeisland.engine.module.basics.command.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.context.CubeContext;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.OnlyIngame;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.basics.storage.Mail;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.basics.storage.TableMail.TABLE_MAIL;

public class MailCommand extends ContainerCommand
{
    private final Basics module;

    public MailCommand(Basics module)
    {
        super(module, "mail", "Manages your server mail.");
        this.module = module;
    }

    @Alias(names = "readmail")
    @Command(desc = "Reads your mail.")
    @IParams(@Grouped(value = @Indexed(label = "player", staticValues = "console", type = User.class), req = false))
    public void read(CubeContext context)
    {
        User sender;
        User mailof = null;
        String nameMailOf = null;
        if (context.hasIndexed(0))
        {
            sender = null;
            if (context.getSender() instanceof User)
            {
                sender = (User)context.getSender();
            }
            if (sender == null)
            {
                context.sendTranslated(NEUTRAL, "If you wanted to look into other players mail use: {text:/mail spy} {input#player}.", context.getArg(0));
                context.sendTranslated(NEGATIVE, "Otherwise be quiet!");
                return;
            }
            mailof = context.getArg(0, null);
            if (mailof == null)
            {
                if (!"console".equalsIgnoreCase(context.getString(0)))
                {
                    context.sendTranslated(NEGATIVE, "User {user} not found!", context.getArg(0));
                    return;
                }
                nameMailOf = "CONSOLE";
            }
            else
            {
                nameMailOf = mailof.getDisplayName();
            }
        }
        else
        {
            sender = null;
            if (context.getSender() instanceof User)
            {
                sender = (User)context.getSender();
            }
            if (sender == null)
            {
                context.sendTranslated(NEUTRAL, "Log into the game to check your mailbox!");
                return;
            }
        }
        BasicsUser bUser = sender.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser();
        if (bUser.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        List<Mail> mails;
        if (mailof == null) //get mails
        {
            mails = bUser.getMails();
        }
        else //Search for mail of that user
        {
            mails = bUser.getMailsFrom(mailof);
        }
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail from {user}.", nameMailOf);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Mail mail : mails)
        {
            i++;
            sb.append("\n").append(ChatFormat.WHITE).append(i).append(": ").append(mail.readMail());
        }
        context.sendTranslated(POSITIVE, "Your mail: {input#mails}", ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(names = "spymail")
    @Command(desc = "Shows the mail of other players.")
    @IParams(@Grouped(@Indexed(label = "player", type = User.class)))
    public void spy(CubeContext context)
    {
        User user = context.getArg(0);
        List<Mail> mails = user.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().getMails();
        if (mails.isEmpty()) // Mailbox is not empty but no message from that player
        {
            context.sendTranslated(NEUTRAL, "{user} does not have any mail!", user);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Mail mail : mails)
        {
            i++;
            sb.append("\n").append(ChatFormat.WHITE).append(i).append(": ").append(mail.getValue(TABLE_MAIL.MESSAGE));
        }
        context.sendTranslated(NEUTRAL, "{user}'s mail: {input#mails}", user, ChatFormat.parseFormats(sb.toString()));
    }

    @Alias(names = "sendmail")
    @Command(desc = "Sends mails to other players.")
    @IParams({@Grouped(@Indexed(label = "player", type = User.class)),
              @Grouped(value = @Indexed(label = "message"), greedy = true)})
    public void send(CubeContext context)
    {
        User user = context.getArg(0);
        String message = context.getStrings(1);
        this.mail(message, context.getSender(), user);
        context.sendTranslated(POSITIVE, "Mail send to {user}!", user);
    }

    @Alias(names = "sendallmail")
    @Command(desc = "Sends mails to all players.")
    @IParams(@Grouped(value = @Indexed(label = "message"), greedy = true))
    public void sendAll(CubeContext context)
    {
        Set<User> users = this.module.getCore().getUserManager().getOnlineUsers();
        final TLongSet alreadySend = new TLongHashSet();
        User sender = null;
        if (context.getSender() instanceof User)
        {
            sender = (User)context.getSender();
        }
        final String message = context.getStrings(0);
        for (User user : users)
        {
            user.attachOrGet(BasicsAttachment.class, module).getBasicsUser().addMail(sender, message);
            alreadySend.add(user.getId());
        }
        final UInteger senderId = sender == null ? null : sender.getEntity().getKey();
        this.module.getCore().getTaskManager().runAsynchronousTaskDelayed(this.getModule(),new Runnable()
        {
            public void run() // Async sending to all Users ever
            {
                DSLContext dsl = module.getCore().getDB().getDSL();
                Collection<Query> queries = new ArrayList<>();
                for (Long userId : module.getCore().getUserManager().getAllIds())
                {
                    if (!alreadySend.contains(userId))
                    {
                        queries.add(dsl.insertInto(TABLE_MAIL, TABLE_MAIL.MESSAGE, TABLE_MAIL.USERID, TABLE_MAIL.SENDERID).values(message, UInteger.valueOf(userId), senderId));
                    }
                }
                dsl.batch(queries).execute();
            }
        },0);
        context.sendTranslated(POSITIVE, "Sent mail to everyone!");
    }

    @Command(desc = "Removes a single mail")
    @IParams(@Grouped(@Indexed(label = "mailid")))
    @OnlyIngame("The console has no mails!")
    public void remove(CubeContext context)
    {
        User user = (User)context.getSender();
        Integer mailId = context.getArg(0, null);
        BasicsUser bUser = user.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser();
        if (bUser.countMail() == 0)
        {
            context.sendTranslated(NEUTRAL, "You do not have any mail!");
            return;
        }
        try
        {
            Mail mail = bUser.getMails().get(mailId);
            module.getCore().getDB().getDSL().delete(TABLE_MAIL).where(TABLE_MAIL.KEY.eq(mail.getValue(TABLE_MAIL.KEY))).execute();
            context.sendTranslated(POSITIVE, "Deleted Mail #{integer#mailid}", mailId);
        }
        catch (IndexOutOfBoundsException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid Mail Id!");
        }
    }

    @Command(desc = "Clears your mail.")
    @IParams(@Grouped(value = @Indexed(label = "player", staticValues = "console", type = User.class), req = false))
    public void clear(CubeContext context)
    {
        User sender = null;
        if (context.getSender() instanceof User)
        {
            sender = (User)context.getSender();
        }
        if (sender == null)
        {
            context.sendTranslated(NEGATIVE, "You will never have mail here!");
            return;
        }
        if (!context.hasIndexed(0))
        {
            sender.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().clearMail();
            context.sendTranslated(NEUTRAL, "Cleared all mails!");
            return;
        }
        User from = "console".equalsIgnoreCase(context.getString(0)) ? null : context.<User>getArg(0);
        sender.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().clearMailFrom(from);
        context.sendTranslated(NEUTRAL, "Cleared all mail from {user}!", from == null ? "console" : from);
    }

    private void mail(String message, CommandSender from, User... users)
    {
        for (User user : users)
        {
            user.attachOrGet(BasicsAttachment.class, this.module).getBasicsUser().addMail(from, message);
            if (user.isOnline())
            {
                user.sendTranslated(NEUTRAL, "You just got a mail from {user}!", from.getName());
            }
        }
    }
}
