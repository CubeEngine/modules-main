package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.ADVENTURE;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.entity.living.player.gamemode.GameModes.SURVIVAL;

public class GameModeCommand
{
    private VanillaPlus module;
    private I18n i18n;

    public GameModeCommand(VanillaPlus module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(alias = "gm", desc = "Changes the gamemode")
    public void gamemode(CommandSource context, @Optional String gamemode, @Default User player)
    {
        if (!context.getIdentifier().equals(player.getIdentifier())
            && !context.hasPermission(module.perms().COMMAND_GAMEMODE_OTHER.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the game mode of an other player!");
            return;
        }
        GameMode newMode = getGameMode(gamemode);
        if (newMode == null)
        {
            newMode = toggleGameMode(player.get(Keys.GAME_MODE).get());
        }
        player.offer(Keys.GAME_MODE, newMode);
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
