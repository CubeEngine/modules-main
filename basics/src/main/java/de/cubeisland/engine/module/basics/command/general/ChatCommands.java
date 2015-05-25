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

import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.TimeUtil;
import de.cubeisland.engine.module.core.util.converter.DurationConverter;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.joda.time.Duration;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.service.command.CommandSender.NON_PLAYER_UUID;
import static java.util.concurrent.TimeUnit.DAYS;

public class ChatCommands
{
    private final DurationConverter converter = new DurationConverter();
    private final UserManager um;
    private CommandManager cm;
    private final Basics module;

    private UUID lastWhisperOfConsole = null;

    public ChatCommands(Basics basics, UserManager um, CommandManager cm)
    {
        this.module = basics;
        this.um = um;
        this.cm = cm;
    }

    @Command(desc = "Sends a private message to someone", alias = {"tell", "message", "pm", "m", "t", "whisper", "w"})
    public void msg(CommandContext context, CommandSender player, @Greed(INFINITE) String message)
    {
        if (player instanceof ConsoleCommandSender)
        {
            sendWhisperTo(NON_PLAYER_UUID, message, context.getSource());
            return;
        }
        if (!this.sendWhisperTo(player.getUniqueId(), message, context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player {user} to send the message to. Is the player offline?", player);
        }
    }

    @Command(alias = "r", desc = "Replies to the last person that whispered to you.")
    public void reply(CommandSender context, @Greed(INFINITE) String message)
    {
        UUID lastWhisper;
        if (context instanceof User)
        {
            lastWhisper = ((User)context).get(BasicsAttachment.class).getLastWhisper();
        }
        else
        {
            lastWhisper = lastWhisperOfConsole;
        }
        if (lastWhisper == null)
        {
            context.sendTranslated(NEUTRAL, "No one has sent you a message that you could reply to!");
            return;
        }
        if (!this.sendWhisperTo(lastWhisper, message, context))
        {
            context.sendTranslated(NEGATIVE, "Could not find the player to reply to. Is the player offline?");
        }
    }

    private boolean sendWhisperTo(UUID whisperTarget, String message, CommandSender context)
    {
        if (NON_PLAYER_UUID.equals(whisperTarget))
        {
            if (context instanceof ConsoleCommandSender)
            {
                context.sendTranslated(NEUTRAL, "Talking to yourself?");
                return true;
            }
            if (context instanceof User)
            {
                ConsoleCommandSender console = cm.getConsoleSender();
                console.sendTranslated(NEUTRAL, "{sender} -> {text:You}: {message:color=WHITE}", context, message);
                context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", console.getDisplayName(), message);
                this.lastWhisperOfConsole = context.getUniqueId();
                ((User)context).get(BasicsAttachment.class).setLastWhisper(NON_PLAYER_UUID);
                return true;
            }
            context.sendTranslated(NONE, "Who are you!?");
            return true;
        }
        User user = um.getExactUser(whisperTarget);
        if (!user.isOnline())
        {
            return false;
        }
        if (context.equals(user))
        {
            context.sendTranslated(NEUTRAL, "Talking to yourself?");
            return true;
        }
        user.sendTranslated(NONE, "{sender} -> {text:You}: {message:color=WHITE}", context.getName(), message);
        if (user.get(BasicsAttachment.class).isAfk())
        {
            context.sendTranslated(NEUTRAL, "{user} is afk!", user);
        }
        context.sendTranslated(NEUTRAL, "{text:You} -> {user}: {message:color=WHITE}", user, message);
        if (context instanceof User)
        {
            ((User)context).get(BasicsAttachment.class).setLastWhisper(user.getUniqueId());
        }
        else
        {
            this.lastWhisperOfConsole = user.getUniqueId();
        }
        user.get(BasicsAttachment.class).setLastWhisper(context.getUniqueId());
        return true;
    }

    @Command(desc = "Broadcasts a message")
    public void broadcast(CommandContext context, @Greed(INFINITE) String message)
    {
        this.um.broadcastMessage(NEUTRAL, "[{text:Broadcast}] {input}", message);
    }

    @Command(desc = "Mutes a player")
    public void mute(CommandSender context, User player, @Optional String duration)
    {
        BasicsUserEntity bUser = player.attachOrGet(BasicsAttachment.class, module).getBasicsUser().getEntity();
        Timestamp muted = bUser.getValue(TABLE_BASIC_USER.MUTED);
        if (muted != null && muted.getTime() < System.currentTimeMillis())
        {
            context.sendTranslated(NEUTRAL, "{user} was already muted!", player);
        }
        Duration dura = module.getConfiguration().commands.defaultMuteTime;
        if (duration != null)
        {
            try
            {
                dura = converter.fromNode(StringNode.of(duration), null, null);
            }
            catch (ConversionException e)
            {
                context.sendTranslated(NEGATIVE, "Invalid duration format!");
                return;
            }
        }
        bUser.setValue(TABLE_BASIC_USER.MUTED, new Timestamp(System.currentTimeMillis() + (dura.getMillis() == 0 ? DAYS.toMillis(9001) : dura.getMillis())));
        bUser.updateAsync();
        String timeString = dura.getMillis() == 0 ? player.getTranslation(NONE, "ever").get(player.getLocale()) : TimeUtil.format(player.getLocale(), dura.getMillis());
        player.sendTranslated(NEGATIVE, "You are now muted for {input#amount}!", timeString);
        context.sendTranslated(NEUTRAL, "You muted {user} globally for {input#amount}!", player, timeString);
    }

    @Command(desc = "Unmutes a player")
    public void unmute(CommandSender context, User player)
    {
        BasicsUserEntity basicsUserEntity = player.attachOrGet(BasicsAttachment.class, module).getBasicsUser().getEntity();
        basicsUserEntity.setValue(TABLE_BASIC_USER.MUTED, null);
        basicsUserEntity.updateAsync();
        context.sendTranslated(POSITIVE, "{user} is no longer muted!", player);
    }

    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandSender context)
    {
        this.um.broadcastTranslatedStatus(NEUTRAL, "rolled a {integer}!", context, new Random().nextInt(100));
    }

    @Command(desc = "Displays the colors")
    public void chatcolors(CommandSender context)
    {
        context.sendTranslated(POSITIVE, "The following chat codes are available:");
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (ChatFormat chatFormat : ChatFormat.values())
        {
            if (i++ % 3 == 0)
            {
                builder.append("\n");
            }
            builder.append(" ").append(chatFormat.getChar()).append(" ").append(chatFormat.toString()).append(chatFormat.name()).append(ChatFormat.RESET);
        }
        context.sendMessage(builder.toString());
        context.sendTranslated(POSITIVE, "To use these type {text:&} followed by the code above");
    }
}
