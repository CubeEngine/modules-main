package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.block.ChangeBlockEvent.Place;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.data.key.Keys.CAN_FLY;
import static org.spongepowered.api.data.key.Keys.FLYING_SPEED;
import static org.spongepowered.api.data.key.Keys.IS_FLYING;

public class MovementCommands
{
    private I18n i18n;

    private final PermissionDescription COMMAND_FLY = COMMAND.childWildcard("fly");
    public final PermissionDescription COMMAND_FLY_KEEP = COMMAND_FLY.child("keep");
    public final PermissionDescription COMMAND_FLY_OTHER = COMMAND_FLY.child("other");

    /**
     * Allows to change the walkspeed of other players
     */
    public final PermissionDescription COMMAND_WALKSPEED_OTHER = COMMAND.childWildcard("walkspeed").child("other");

    public MovementCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandSource context, Double speed, @Default Player player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!context.hasPermission(COMMAND_WALKSPEED_OTHER.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }

        if (!player.isOnline())
        {
            i18n.sendTranslated(context, NEGATIVE, "{user} is offline!", player.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            player.offer(Keys.WALKING_SPEED, speed / 10.0);
            i18n.sendTranslated(player, POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        player.offer(Keys.WALKING_SPEED, 0.2);
        if (speed != null && speed > 9000)
        {
            i18n.sendTranslated(player, NEGATIVE, "It's over 9000!");
        }
        i18n.sendTranslated(player, NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandSource context, @Optional Float flyspeed, @Default @Named("player") Player player)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //i18n.sendTranslated(context, NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //i18n.sendTranslated(context, NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        if (!context.equals(player) && !context.hasPermission(COMMAND_FLY_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (flyspeed != null)
        {
            if (flyspeed >= 0 && flyspeed <= 10)
            {
                player.offer(FLYING_SPEED, flyspeed / 10d);
                i18n.sendTranslated(player, POSITIVE, "You can now fly at {decimal#speed:2}!", flyspeed);
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
            player.offer(CAN_FLY, true);
            player.offer(IS_FLYING, true);
            return;
        }
        player.offer(CAN_FLY, !player.getValue(CAN_FLY).get().get());
        if (player.getValue(CAN_FLY).get().get())
        {
            player.offer(FLYING_SPEED, 0.1);
            i18n.sendTranslated(player, POSITIVE, "You can now fly!");
            if (!player.equals(context))
            {
                i18n.sendTranslated(context, POSITIVE, "{player} can now fly!", player);
            }
            return;
        }
        i18n.sendTranslated(player, NEUTRAL, "You cannot fly anymore!");
        if (!player.equals(context))
        {
            i18n.sendTranslated(context, POSITIVE, "{player} cannot fly anymore!", player);
        }
    }
}
