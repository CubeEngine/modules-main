package org.cubeengine.module.vanillaplus.addition;

import java.util.List;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.data.key.Keys;

public class PlayerFoodCommands
{


    @Command(desc = "Refills your hunger bar")
    public void feed(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context;
            sender.asPlayer().offer(Keys.FOOD_LEVEL, 20);
            sender.asPlayer().offer(Keys.SATURATION, 20.0);
            sender.asPlayer().offer(Keys.EXHAUSTION, 0.0);
            context.sendTranslated(POSITIVE, "You are now fed!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_FEED_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
            }
            context.sendTranslated(POSITIVE, "You made everyone fat!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "shared food with everyone.", context);
            // TODO MessageType separate for translate Messages and messages from external input e.g. /me
        }
        else
        {
            context.sendTranslated(POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(POSITIVE, "You got fed by {user}!", context);
            }
            user.asPlayer().offer(Keys.FOOD_LEVEL, 20);
            user.asPlayer().offer(Keys.SATURATION, 20.0);
            user.asPlayer().offer(Keys.EXHAUSTION, 0.0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendMessage(Texts.of("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                context.sendTranslated(NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            User sender = (User)context;
            sender.asPlayer().offer(Keys.FOOD_LEVEL, 0);
            sender.asPlayer().offer(Keys.SATURATION, 0.0);
            sender.asPlayer().offer(Keys.EXHAUSTION, 4.0);
            context.sendTranslated(NEGATIVE, "You are now starving!");
            return;
        }
        if (!context.hasPermission(module.perms().COMMAND_STARVE_OTHER))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(NEUTRAL, "You let everyone starve to death!");
            this.um.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(NEUTRAL, "You are suddenly starving!");
            }
            user.asPlayer().offer(Keys.FOOD_LEVEL, 0);
            user.asPlayer().offer(Keys.SATURATION, 0.0);
            user.asPlayer().offer(Keys.EXHAUSTION, 4.0);
        }
    }

}
