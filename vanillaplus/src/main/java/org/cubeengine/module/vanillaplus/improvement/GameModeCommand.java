package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class GameModeCommand
{

    @Command(alias = "gm", desc = "Changes the gamemode")
    public void gamemode(CommandSender context, @Optional String gamemode, @Default User player)
    {
        if (!context.equals(player) && !module.perms().COMMAND_GAMEMODE_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the game mode of an other player!");
            return;
        }
        GameMode newMode = getGameMode(gamemode);
        if (newMode == null)
        {
            newMode = toggleGameMode(player.getGameMode());
        }
        player.setGameMode(newMode);
        if (context.equals(player))
        {
            context.sendTranslated(POSITIVE, "You changed your game mode to {input#gamemode}!", newMode.getName());
            return;
        }
        context.sendTranslated(POSITIVE, "You changed the game mode of {user} to {input#gamemode}!", player.getDisplayName(), newMode.getName()); // TODO translate gamemode
        player.sendTranslated(NEUTRAL, "Your game mode has been changed to {input#gamemode}!", newMode.getName());
    }
}
