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
package de.cubeisland.engine.module.vanillaplus;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Optional;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.entity.player.gamemode.GameMode;
import org.spongepowered.api.entity.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.entity.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.entity.player.gamemode.GameModes.SURVIVAL;

/**
 * {@link #gamemode}
 * {@link #kill}
 * {@link #sudo}
 * {@link #suicide}
 */
public class PlayerCommands
{
    private VanillaPlus module;

    public PlayerCommands(VanillaPlus module)
    {
        this.module = module;
    }

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

    @Command(alias = "slay", desc = "Kills a player")
    public void kill(CommandSender context, UserList players, // TODO default line of sight player
                     @Flag boolean force, @Flag boolean quiet, @Flag boolean lightning)
    {
        lightning = lightning && module.perms().COMMAND_KILL_LIGHTNING.isAuthorized(context);
        force = force && module.perms().COMMAND_KILL_FORCE.isAuthorized(context);
        quiet = quiet && module.perms().COMMAND_KILL_QUIET.isAuthorized(context);
        List<Text> killed = new ArrayList<>();
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (!module.perms().COMMAND_KILL_ALL.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to kill everyone!");
                return;
            }
            if (context instanceof User)
            {
                userList.remove(context);
            }
        }
        for (User user : userList)
        {
            if (this.kill(user, lightning, context, false, force, quiet))
            {
                killed.add(user.getDisplayName());
            }
        }
        if (killed.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No one was killed!");
            return;
        }
        context.sendTranslated(POSITIVE, "You killed {user#list}!", StringUtils.implode(",", killed));
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
                return GameModes.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameModes.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameModes.ADVENTURE;
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

    private boolean kill(User user, boolean lightning, CommandSender context, boolean showMessage, boolean force, boolean quiet)
    {
        if (!force)
        {
            if (module.perms().COMMAND_KILL_PREVENT.isAuthorized(user) || this.module.getBasicsUser(user.asPlayer()).getEntity().getValue(TABLE_BASIC_USER.GODMODE))
            {
                context.sendTranslated(NEGATIVE, "You cannot kill {user}!", user);
                return false;
            }
        }
        if (lightning)
        {
            user.getWorld().strikeLightningEffect(user.getLocation());
        }
        user.setHealth(0);
        if (showMessage)
        {
            context.sendTranslated(POSITIVE, "You killed {user}!", user);
        }
        if (!quiet && module.perms().COMMAND_KILL_NOTIFY.isAuthorized(user))
        {
            user.sendTranslated(NEUTRAL, "You were killed by {user}", context);
        }
        return true;
    }

    @Command(desc = "Makes a player send a message (including commands)")
    public void sudo(CommandContext context, User player, @Greed(INFINITE) String message)
    {
        if (!message.startsWith("/"))
        {
            player.chat(message);
            context.sendTranslated(POSITIVE, "Forced {user} to chat: {input#message}", player, message);
            return;
        }
        if (cm.runCommand(player, message.substring(1)))
        {
            context.sendTranslated(POSITIVE, "Command {input#command} executed as {user}", message, player);
            return;
        }
        context.sendTranslated(NEGATIVE, "Command was not executed successfully!");
    }

    @Command(desc = "Kills yourself")
    @Restricted(value = User.class, msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}") // TODO replace User.class /w interface that has life stuff?
    public void suicide(User context)
    {
        context.setHealth(0);

        // TODO context.setLastDamageCause(new EntityDamageEvent(context, CUSTOM, context.getMaxHealth()));
        // maybe DamageableData
        context.sendTranslated(NEGATIVE, "You ended your life. Why? {text:\\:(:color=DARK_RED}");
    }


}
