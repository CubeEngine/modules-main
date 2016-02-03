package org.cubeengine.module.vanillaplus.addition;

import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class PlayerFoodCommands
{

    private I18n i18n;
    private Broadcaster bc;

    private final PermissionDescription COMMAND_FEED = COMMAND.childWildcard("feed");
    public final PermissionDescription COMMAND_FEED_OTHER = COMMAND_FEED.child("other");

    private final PermissionDescription COMMAND_STARVE = COMMAND.childWildcard("starve");
    public final PermissionDescription COMMAND_STARVE_OTHER = COMMAND_STARVE.child("other");

    public PlayerFoodCommands(I18n i18n, Broadcaster bc)
    {
        this.i18n = i18n;
        this.bc = bc;
    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandSource context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context;
            sender.offer(Keys.FOOD_LEVEL, 20);
            sender.offer(Keys.SATURATION, 20.0);
            sender.offer(Keys.EXHAUSTION, 0.0);
            i18n.sendTranslated(context, POSITIVE, "You are now fed!");
            return;
        }
        if (!context.hasPermission(COMMAND_FEED_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                i18n.sendTranslated(context, NEGATIVE, "There are no players online at the moment!");
            }
            i18n.sendTranslated(context, POSITIVE, "You made everyone fat!");
            bc.broadcastStatus(ChatFormat.BRIGHT_GREEN + "shared food with everyone.", context);
            // TODO MessageType separate for translate Messages and messages from external input e.g. /me
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (Player user : userList)
        {
            if (!players.isAll())
            {
                i18n.sendTranslated(user, POSITIVE, "You got fed by {user}!", context);
            }
            user.offer(Keys.FOOD_LEVEL, 20);
            user.offer(Keys.SATURATION, 20.0);
            user.offer(Keys.EXHAUSTION, 0.0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandSource context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof Player))
            {
                context.sendMessage(Text.of("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                i18n.sendTranslated(context, NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            User sender = (User)context;
            sender.offer(Keys.FOOD_LEVEL, 0);
            sender.offer(Keys.SATURATION, 0.0);
            sender.offer(Keys.EXHAUSTION, 4.0);
            i18n.sendTranslated(context, NEGATIVE, "You are now starving!");
            return;
        }
        if (!context.hasPermission(COMMAND_STARVE_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                i18n.sendTranslated(context, NEGATIVE, "There are no players online at the moment!");
                return;
            }
            i18n.sendTranslated(context, NEUTRAL, "You let everyone starve to death!");
            bc.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context);
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (Player user : userList)
        {
            if (!players.isAll())
            {
                i18n.sendTranslated(user, NEUTRAL, "You are suddenly starving!");
            }
            user.offer(Keys.FOOD_LEVEL, 0);
            user.offer(Keys.SATURATION, 0.0);
            user.offer(Keys.EXHAUSTION, 4.0);
        }
    }
}
