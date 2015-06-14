package de.cubeisland.engine.module.vanillaplus;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserList;
import org.spongepowered.api.entity.player.gamemode.GameMode;
import org.spongepowered.api.entity.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
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
            if (module.perms().COMMAND_KILL_PREVENT.isAuthorized(user) || this.module.getBasicsUser(user.getPlayer().get()).getEntity().getValue(TABLE_BASIC_USER.GODMODE))
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
