/*
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class MovementCommands extends PermissionContainer
{
    private I18n i18n;

    public final Permission COMMAND_FLY_OTHER = register("command.fly.other", "", null);
    public final Permission COMMAND_WALKSPEED_OTHER = register("command.walkspeed.other", "Allows to change the walkspeed of other players", null);

    @Inject
    public MovementCommands(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandCause context, double speed, @Default ServerPlayer player)
    {
        boolean other = false;
        if (!context.getSubject().equals(player))
        {
            if (!COMMAND_WALKSPEED_OTHER.check(context))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }

        if (!player.isOnline())
        {
            i18n.send(context, NEGATIVE, "{user} is offline!", player.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            player.offer(Keys.WALKING_SPEED, speed / 10.0);
            if (other)
            {
                i18n.send(player, POSITIVE, "{user} can now walk at {decimal:2}!", player, speed);
                return;
            }
            i18n.send(player, POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        player.offer(Keys.WALKING_SPEED, 0.2);
        if (speed > 9000)
        {
            i18n.send(player, NEGATIVE, "It's over 9000!");
        }
        i18n.send(player, NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandCause context, @Option Float flyspeed, @Default @Named("player") Player player)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //i18n.sendTranslated(context, NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //i18n.sendTranslated(context, NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        final boolean isNotTarget = !player.equals(context.getSubject());
        if (isNotTarget && !context.hasPermission(COMMAND_FLY_OTHER.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (flyspeed != null)
        {
            if (flyspeed >= 0 && flyspeed <= 10)
            {
                player.offer(Keys.FLYING_SPEED, flyspeed / 10d);
                i18n.send(player, POSITIVE, "You can now fly at {decimal#speed:2}!", flyspeed);
                if (isNotTarget)
                {
                    i18n.send(context, POSITIVE, "{player} can now fly at {decimal#speed:2}!", player, flyspeed);
                }
            }
            else
            {
                if (flyspeed > 9000)
                {
                    i18n.send(context, NEUTRAL, "It's over 9000!");
                }
                i18n.send(context, NEGATIVE, "FlySpeed has to be a Number between {text:0} and {text:10}!");
            }
            player.offer(Keys.CAN_FLY, true);
            player.offer(Keys.IS_FLYING, true);
            return;
        }
        player.offer(Keys.CAN_FLY, !player.getValue(Keys.CAN_FLY).get().get());
        if (player.getValue(Keys.CAN_FLY).get().get())
        {
            player.offer(Keys.FLYING_SPEED, 0.1);
            i18n.send(player, POSITIVE, "You can now fly!");
            if (isNotTarget)
            {
                i18n.send(context, POSITIVE, "{player} can now fly!", player);
            }
            return;
        }
        player.offer(Keys.IS_FLYING, false);
        i18n.send(player, NEUTRAL, "You cannot fly anymore!");
        if (isNotTarget)
        {
            i18n.send(context, POSITIVE, "{player} cannot fly anymore!", player);
        }
    }
}
