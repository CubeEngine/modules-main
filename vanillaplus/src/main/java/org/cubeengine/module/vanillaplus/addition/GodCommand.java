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
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Provides Gamemodelike Protection
 */
public class GodCommand extends PermissionContainer<VanillaPlus>
{
    private I18n i18n;
    public final PermissionDescription COMMAND_GOD_OTHER = register("command.god.other",
                                                                    "Allows to enable god-mode for other players", null);

    public GodCommand(VanillaPlus module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
    }


    @Command(desc = "Toggles the god-mode!")
    public void god(CommandSource context, @Default Player player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!context.hasPermission(COMMAND_GOD_OTHER.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }

        Integer invTime = player.get(Keys.INVULNERABILITY).orElse(0);
        if (invTime > 0)
        {
            player.remove(Keys.INVULNERABILITY);
            if (!other)
            {
                i18n.sendTranslated(context, NEUTRAL, "You are no longer invincible!");
                return;
            }
            i18n.sendTranslated(player, NEUTRAL, "You are no longer invincible!");
            i18n.sendTranslated(context, NEUTRAL, "{user} is no longer invincible!", player);
            return;
        }
        player.offer(Keys.INVULNERABILITY, Integer.MAX_VALUE);
        if (!other)
        {
            i18n.sendTranslated(context, POSITIVE, "You are now invincible!");
            return;
        }
        i18n.sendTranslated(player, POSITIVE, "You are now invincible!");
        i18n.sendTranslated(context, POSITIVE, "{user} is now invincible!", player);
    }
}
