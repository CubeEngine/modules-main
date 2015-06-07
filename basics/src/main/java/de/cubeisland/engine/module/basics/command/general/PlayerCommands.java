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
package de.cubeisland.engine.module.basics.command.general;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.TimeUtil;
import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.module.service.ban.BanManager;
import de.cubeisland.engine.module.service.ban.UserBan;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserList;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.gamemode.GameMode;
import org.spongepowered.api.entity.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Location;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static java.text.DateFormat.SHORT;
import static org.spongepowered.api.entity.player.gamemode.GameModes.CREATIVE;
import static org.spongepowered.api.entity.player.gamemode.GameModes.SURVIVAL;

public class PlayerCommands
{
    private final UserManager um;
    private CommandManager cm;
    private BanManager banManager;
    private Game game;
    private final Basics module;

    public PlayerCommands(Basics basics, UserManager um, EventManager em, TaskManager taskManager, CommandManager cm, BanManager banManager, Game game)
    {
        this.module = basics;
        this.um = um;
        this.cm = cm;
        this.banManager = banManager;
        this.game = game;

    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context;
            sender.setFoodLevel(20);
            sender.setSaturation(20);
            sender.setExhaustion(0);
            context.sendTranslated(POSITIVE, "You are now fed!");
            return;
        }
        if (!module.perms().COMMAND_FEED_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
            }
            context.sendTranslated(POSITIVE, "You made everyone fat!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "shared food with everyone.", context);
            // TODO MessageType separate for translate Messages and messages from external input e.g. /me
        }
        else
        {
            context.sendTranslated(POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(POSITIVE, "You got fed by {user}!", context);
            }
            user.setFoodLevel(20);
            user.setSaturation(20);
            user.setExhaustion(0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendMessage(Texts.of("\n\n\n\n\n\n\n\n\n\n\n\n\n"));
                context.sendTranslated(NEGATIVE, "I'll give you only one line to eat!");
                return;
            }
            User sender = (User)context;
            sender.setFoodLevel(0);
            sender.setSaturation(0);
            sender.setExhaustion(4);
            context.sendTranslated(NEGATIVE, "You are now starving!");
            return;
        }
        if (!module.perms().COMMAND_STARVE_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(NEUTRAL, "You let everyone starve to death!");
            this.um.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(NEUTRAL, "You are suddenly starving!");
            }
            user.setFoodLevel(0);
            user.setSaturation(0);
            user.setExhaustion(4);
        }
    }

    @Command(desc = "Heals a player")
    public void heal(CommandSender context, @Optional UserList players)
    {
        if (players == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Only time can heal your wounds!");
                return;
            }
            User sender = (User)context;
            sender.setHealth(sender.getMaxHealth());
            sender.sendTranslated(POSITIVE, "You are now healed!");
            return;
        }
        if (!module.perms().COMMAND_HEAL_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to heal other players!");
            return;
        }
        List<User> userList = players.list();
        if (players.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(POSITIVE, "You healed everyone!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "healed every player.", context);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Healed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!players.isAll())
            {
                user.sendTranslated(POSITIVE, "You got healed by {sender}!", context);
            }
            user.setHealth(user.getMaxHealth());
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

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandSender context, User player)
    {
        if (player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Date lastPlayed = player.getLastPlayed();
        if (System.currentTimeMillis() - lastPlayed.getTime() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            context.sendTranslated(NEUTRAL, "{user} was last seen {input#date}.", player, TimeUtil.format(
                context.getLocale(), new Date(lastPlayed.getTime())));
            return;
        }
        Date date = new Date(lastPlayed.getTime());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
        context.sendTranslated(NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
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



    @Command(desc = "Displays informations from a player!")
    public void whois(CommandSender context, User player)
    {
        if (player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        if (player.hasPlayedBefore() || player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.getHealth(), player.getMaxHealth());
            context.sendTranslated(NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.getFoodLevel(), (int)player.getSaturation(), player.getFoodLevel());
            context.sendTranslated(NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.getLevel(), (int)(player.getExpPerecent() * 100));
            Location loc = player.getLocation();
            if (loc != null)
            {
                context.sendTranslated(NEUTRAL, "Position: {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
            }
            if (player.getAddress() != null)
            {
                context.sendTranslated(NEUTRAL, "IP: {input#ip}", player.getAddress().getAddress().getHostAddress());
            }
            if (player.getGameMode() != null)
            {
                context.sendTranslated(NEUTRAL, "Gamemode: {input#gamemode}", player.getGameMode().toString());
            }
            if (player.getAllowFlight())
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", player.isFlying() ? "flying" : "not flying");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            if (player.isOp())
            {
                context.sendTranslated(NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }
            Timestamp muted = module.getBasicsUser(player.getPlayer().get()).getEntity().getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && muted.getTime() > System.currentTimeMillis())
            {
                context.sendTranslated(NEUTRAL, "Muted until {input#time}", DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale()).format(muted));
            }
            if (player.getGameMode() != CREATIVE)
            {
                context.sendTranslated(NEUTRAL, "GodMode: {input#godmode}", player.isInvulnerable() ? ChatFormat.BRIGHT_GREEN + "true" : ChatFormat.RED + "false");
            }
            if (player.get(BasicsAttachment.class).isAfk())
            {
                context.sendTranslated(NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SHORT, SHORT, Locale.ENGLISH);
            context.sendTranslated(NEUTRAL, "First played: {input#date}", dateFormat.format(player.getFirstPlayed()));
        }
        if (banManager.isUserBanned(player.getUniqueId()))
        {
            UserBan ban = banManager.getUserBan(player.getUniqueId());
            String expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
            if (ban.getExpires() != null)
            {
                expires = format.format(ban.getExpires());
            }
            else
            {
                expires = context.getTranslation(NONE, "for ever").toString();
            }
            context.sendTranslated(NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})", ban.getSource(), format.format(ban.getCreated()), ban.getReason(), expires);
        }
    }

    @Command(desc = "Toggles the god-mode!")
    public void god(CommandSender context, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!module.perms().COMMAND_GOD_OTHER.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }
        BasicsUserEntity bUser = module.getBasicsUser(player.getPlayer().get()).getEntity();
        bUser.setValue(TABLE_BASIC_USER.GODMODE, !bUser.getValue(TABLE_BASIC_USER.GODMODE));
        player.setInvulnerable(bUser.getValue(TABLE_BASIC_USER.GODMODE));
        if (bUser.getValue(TABLE_BASIC_USER.GODMODE))
        {
            if (!other)
            {
                context.sendTranslated(POSITIVE, "You are now invincible!");
                return;
            }
            player.sendTranslated(POSITIVE, "You are now invincible!");
            context.sendTranslated(POSITIVE, "{user} is now invincible!", player);
            return;
        }
        if (!other)
        {
            context.sendTranslated(NEUTRAL, "You are no longer invincible!");
            return;
        }
        player.sendTranslated(NEUTRAL, "You are no longer invincible!");
        context.sendTranslated(NEUTRAL, "{user} is no longer invincible!", player);
    }

    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandSender context, Float speed, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!module.perms().COMMAND_WALKSPEED_OTHER.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is offline!", player.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            player.setWalkSpeed(speed / 10f);
            player.sendTranslated(POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        player.setWalkSpeed(0.2f);
        if (speed != null && speed > 9000)
        {
            player.sendTranslated(NEGATIVE, "It's over 9000!");
        }
        player.sendTranslated(NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandSender context,@Optional Float flyspeed, @Default @Named("player") User player)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //context.sendTranslated(NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //context.sendTranslated(NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        if (!context.equals(player) && !module.perms().COMMAND_FLY_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (flyspeed != null)
        {
            if (flyspeed >= 0 && flyspeed <= 10)
            {
                player.setFlySpeed(flyspeed / 10f);
                player.sendTranslated(POSITIVE, "You can now fly at {decimal#speed:2}!", flyspeed);
                if (!player.equals(context))
                {
                    context.sendTranslated(POSITIVE, "{player} can now fly at {decimal#speed:2}!", player, flyspeed);
                }
            }
            else
            {
                if (flyspeed > 9000)
                {
                    context.sendTranslated(NEUTRAL, "It's over 9000!");
                }
                context.sendTranslated(NEGATIVE, "FlySpeed has to be a Number between {text:0} and {text:10}!");
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            return;
        }
        player.setAllowFlight(!player.getAllowFlight());
        if (player.getAllowFlight())
        {
            player.setFlySpeed(0.1f);
            player.sendTranslated(POSITIVE, "You can now fly!");
            if (!player.equals(context))
            {
                context.sendTranslated(POSITIVE, "{player} can now fly!", player);
            }
            return;
        }
        player.sendTranslated(NEUTRAL, "You cannot fly anymore!");
        if (!player.equals(context))
        {
            context.sendTranslated(POSITIVE, "{player} cannot fly anymore!", player);
        }
    }



    @Command(alias = "roll", desc = "Shows a random number from 0 to 100")
    public void rand(CommandSender context)
    {
        this.um.broadcastTranslatedStatus(NEUTRAL, "rolled a {integer}!", context, new Random().nextInt(100));
    }
}
