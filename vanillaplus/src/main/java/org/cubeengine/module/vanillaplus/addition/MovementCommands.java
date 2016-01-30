package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.spongepowered.api.data.key.Keys;

public class MovementCommands
{
    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandSender context, Double speed, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!context.hasPermission(module.perms().COMMAND_WALKSPEED_OTHER))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }

        if (!player.getUser().isOnline())
        {
            i18n.sendTranslated(context, NEGATIVE, "{user} is offline!", player.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            player.asPlayer().offer(Keys.WALKING_SPEED, speed / 10.0);
            player.sendTranslated(POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        player.asPlayer().offer(Keys.WALKING_SPEED, 0.2);
        if (speed != null && speed > 9000)
        {
            player.sendTranslated(NEGATIVE, "It's over 9000!");
        }
        player.sendTranslated(NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandSender context, @Optional Float flyspeed, @Default @Named("player") User player)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //i18n.sendTranslated(context, NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //i18n.sendTranslated(context, NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_FLY_OTHER))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (flyspeed != null)
        {
            if (flyspeed >= 0 && flyspeed <= 10)
            {
                player.asPlayer().offer(Keys.FLYING_SPEED, flyspeed / 10f);
                player.sendTranslated(POSITIVE, "You can now fly at {decimal#speed:2}!", flyspeed);
                if (!player.equals(context))
                {
                    i18n.sendTranslated(context, POSITIVE, "{player} can now fly at {decimal#speed:2}!", player, flyspeed);
                }
            }
            else
            {
                if (flyspeed > 9000)
                {
                    i18n.sendTranslated(context, NEUTRAL, "It's over 9000!");
                }
                i18n.sendTranslated(context, NEGATIVE, "FlySpeed has to be a Number between {text:0} and {text:10}!");
            }
            player.asPlayer().offer(Keys.CAN_FLY, true);
            player.asPlayer().offer(Keys.FLYING, true);
            return;
        }
        player.asPlayer().offer(Keys.CAN_FLY, !player.asPlayer().getValue(Keys.CAN_FLY));
        if (player.asPlayer().getValue(Keys.CAN_FLY))
        {
            player.asPlayer().offer(FLYING_SPEED, 0.1);
            player.sendTranslated(POSITIVE, "You can now fly!");
            if (!player.equals(context))
            {
                i18n.sendTranslated(context, POSITIVE, "{player} can now fly!", player);
            }
            return;
        }
        player.sendTranslated(NEUTRAL, "You cannot fly anymore!");
        if (!player.equals(context))
        {
            i18n.sendTranslated(context, POSITIVE, "{player} cannot fly anymore!", player);
        }
    }
}
