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
package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.data.key.Keys.*;

public class MovementCommands extends PermissionContainer<VanillaPlus>
{
    private I18n i18n;

    public final PermissionDescription COMMAND_FLY_OTHER = register("command.fly.other", "", null);
    public final PermissionDescription COMMAND_WALKSPEED_OTHER = register("command.walkspeed.other", "Allows to change the walkspeed of other players", null);

    public MovementCommands(VanillaPlus module, I18n i18n)
    {
        super(module);
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
            if (other)
            {
                i18n.sendTranslated(player, POSITIVE, "{user} can now walk at {decimal:2}!", player, speed);
                return;
            }
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
        player.offer(IS_FLYING, false);
        i18n.sendTranslated(player, NEUTRAL, "You cannot fly anymore!");
        if (!player.equals(context))
        {
            i18n.sendTranslated(context, POSITIVE, "{player} cannot fly anymore!", player);
        }
    }
}
