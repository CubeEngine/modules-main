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
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.Ticks;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * Provides create gamemode like Protection
 */
@Singleton
public class GodCommand extends PermissionContainer
{
    private I18n i18n;
    public final Permission COMMAND_GOD_OTHER = register("command.god.other", "Allows to enable god-mode for other players");

    @Inject
    public GodCommand(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(desc = "Toggles the god-mode!")
    public void god(CommandCause context, @Default ServerPlayer player)
    {
        boolean other = false;
        if (!context.subject().equals(player))
        {
            if (!context.hasPermission(COMMAND_GOD_OTHER.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }

        final Boolean invulnerable = player.get(Keys.INVULNERABLE).orElse(false);
        if (invulnerable)
        {
            player.offer(Keys.INVULNERABLE, false);
            if (!other)
            {
                i18n.send(context, NEUTRAL, "You are no longer invincible!");
                return;
            }
            i18n.send(player, NEUTRAL, "You are no longer invincible!");
            i18n.send(context, NEUTRAL, "{user} is no longer invincible!", player);
            return;
        }
        player.offer(Keys.INVULNERABLE, true);
        if (!other)
        {
            i18n.send(context, POSITIVE, "You are now invincible!");
            return;
        }
        i18n.send(player, POSITIVE, "You are now invincible!");
        i18n.send(context, POSITIVE, "{user} is now invincible!", player);
    }
}
