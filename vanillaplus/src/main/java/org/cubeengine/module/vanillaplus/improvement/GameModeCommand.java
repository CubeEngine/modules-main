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
package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.data.key.Keys.GAME_MODE;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.*;

public class GameModeCommand extends PermissionContainer
{
    private I18n i18n;

    public final Permission COMMAND_GAMEMODE_OTHER = register("command.gamemode.other", "Allows to change the game-mode of other players too");

    public GameModeCommand(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    // TODO completer
    @Command(alias = "gm", desc = "Changes the gamemode")
    public void gamemode(CommandSource context, @Optional String gamemode, @Default User player)
    {
        if (!context.getIdentifier().equals(player.getIdentifier())
            && !context.hasPermission(COMMAND_GAMEMODE_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the game mode of an other player!");
            return;
        }
        GameMode newMode = getGameMode(gamemode);
        if (newMode == null)
        {
            newMode = toggleGameMode(player.get(GAME_MODE).get());
        }
        player.offer(GAME_MODE, newMode);
        if (context.equals(player))
        {
            i18n.sendTranslated(context, POSITIVE, "You changed your game mode to {input#gamemode}!", newMode.getTranslation());
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "You changed the game mode of {user} to {input#gamemode}!", player, newMode.getTranslation());
        if (player.isOnline())
        {
            i18n.sendTranslated(player.getPlayer().get(), NEUTRAL, "Your game mode has been changed to {input#gamemode}!", newMode.getName());
        }

    }

    private GameMode getGameMode(String name)
    {
        if (name == null)
        {
            return null;
        }
        switch (name.trim().toLowerCase())
        {
            case "survival":
            case "s":
            case "0":
                return SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return ADVENTURE;
            default:
                return null;
        }
    }


    private GameMode toggleGameMode(GameMode mode)
    {
        if (mode == SURVIVAL)
        {
            return CREATIVE;
        }
        //if (mode == ADVENTURE || mode == CREATIVE)
        return SURVIVAL;
    }

}
