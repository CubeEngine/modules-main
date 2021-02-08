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
package org.cubeengine.module.vanillaplus.improvement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.*;

@Singleton
public class GameModeCommand extends PermissionContainer
{
    private I18n i18n;

    public final Permission COMMAND_GAMEMODE_OTHER = register("command.gamemode.other", "Allows to change the game-mode of other players too");

    @Inject
    public GameModeCommand(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    @Command(alias = "gm", desc = "Changes the gamemode")
    public void gamemode(CommandCause context, @Option GameMode gamemode, @Default User player)
    {
        if (!context.getIdentifier().equals(player.getIdentifier())
            && !context.hasPermission(COMMAND_GAMEMODE_OTHER.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to change the game mode of an other player!");
            return;
        }
        GameMode newMode = gamemode;
        if (newMode == null)
        {
            newMode = toggleGameMode(player.get(Keys.GAME_MODE).get());
        }
        player.offer(Keys.GAME_MODE, newMode);
        if (context.getSubject().equals(player.getPlayer().orElse(null)))
        {
            i18n.send(ChatType.ACTION_BAR, context, POSITIVE, "You changed your game mode to {input#gamemode}!", newMode.asComponent());
            return;
        }
        i18n.send(context, POSITIVE, "You changed the game mode of {user} to {input#gamemode}!", player, newMode.asComponent());
        if (player.isOnline() && !context.getSubject().equals(player.getPlayer().get()))
        {
            i18n.send(player.getPlayer().get(), NEUTRAL, "Your game mode has been changed to {input#gamemode}!", newMode.asComponent());
        }

    }

    private GameMode toggleGameMode(GameMode mode)
    {
        if (mode == SURVIVAL.get())
        {
            return CREATIVE.get();
        }
        //if (mode == ADVENTURE || mode == CREATIVE)
        return SURVIVAL.get();
    }

}
