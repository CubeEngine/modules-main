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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.mail.storage.Mail;
import org.cubeengine.module.mail.storage.TableMail;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.event.Order.POST;
import static org.spongepowered.api.text.format.TextColors.WHITE;

@Command(name = "mail", desc = "Manages your server mail.")
public class MailCommand extends ContainerCommand
{
    private final MailModule module;
    private final TaskManager taskManager;
    private final Database db;
    private I18n i18n;

    private Map<UUID, PlayerMails> mails = new HashMap<>();

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


        PlayerMails attachment = getMails(sender);
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

    private PlayerMails getMails(User sender)
    {
        PlayerMails mails = this.mails.get(sender.getUniqueId());
        if (mails == null)
        {
            mails = new PlayerMails(sender.getUniqueId(), db);
            this.mails.put(sender.getUniqueId(), mails);
        }
        return mails;
    }

    @Alias(value = "spymail")
    @Command(desc = "Shows the mail of other players.")
    public void spy(CommandSource context, User player)
    {
        List<Mail> mails = getMails(player).getMails();
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
        Collection<Player> users = Sponge.getServer().getOnlinePlayers();
        final Set<UUID> alreadySend = new HashSet<>();
        for (User user : users)
        {
            getMails(user).addMail(context, message);
            alreadySend.add(user.getUniqueId());
        }
        taskManager.runAsynchronousTaskDelayed(this.module, () ->
            Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getAll().stream()
                .filter(p -> alreadySend.contains(p.getUniqueId()))
                .forEach(p -> new PlayerMails(p.getUniqueId(), db).addMail(context, message)), 0);
        i18n.sendTranslated(context,  POSITIVE, "Sent mail to everyone!");
    }

    @Command(desc = "Removes a single mail")
    @Restricted(value = Player.class, msg = "The console has no mails!")
    public void remove(Player context, Integer mailId)
    {
        PlayerMails attachment = getMails(context);
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
    @Restricted(value = Player.class, msg = "You will never have mail here!")
    public void clear(Player context, @Optional CommandSource player)
    {
        if (player == null)
        {
            getMails(context).clearMail();
            i18n.sendTranslated(context,  NEUTRAL, "Cleared all mails!");
            return;
        }
        getMails(context).clearMailFrom(player);
        i18n.sendTranslated(context,  NEUTRAL, "Cleared all mail from {user}!", player instanceof User ? player : "console");
    }

    private void mail(String message, CommandSource from, User... users)
    {
        for (User user : users)
        {
            getMails(user).addMail(from, message);
            if (user.isOnline())
            {
                i18n.sendTranslated(user.getPlayer().get(), NEUTRAL, "You just got a mail from {user}!", from.getName());
            }
        }
    }


    @Listener(order = POST)
    public void onAfterJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        PlayerMails mails = getMails(player);
        int amount = mails.countMail();
        if (amount > 0)
        {
            i18n.sendTranslatedN(player, POSITIVE, amount, "You have a new mail!", "You have {amount} of mail!", amount);
            i18n.sendTranslated(player, NEUTRAL, "Use {text:/mail read} to display them.");
        }
    }
}
