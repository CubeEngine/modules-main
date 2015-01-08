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

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Greed;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.core.ban.UserBan;
import de.cubeisland.engine.core.bukkit.BukkitUtils;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserList;
import de.cubeisland.engine.core.user.UserManager;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.TimeUtil;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.basics.storage.BasicsUserEntity;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.basics.storage.TableBasicsUser.TABLE_BASIC_USER;
import static java.text.DateFormat.SHORT;

public class PlayerCommands
{
    private final UserManager um;
    private final Basics module;
    private AfkListener afkListener;

    public PlayerCommands(Basics basics)
    {
        this.module = basics;
        this.um = basics.getCore().getUserManager();
        final long autoAfk;
        final long afkCheck;
        afkCheck = basics.getConfiguration().autoAfk.check.getMillis();
        if (afkCheck > 0)
        {
            autoAfk = basics.getConfiguration().autoAfk.after.getMillis();
            this.afkListener = new AfkListener(basics, autoAfk, afkCheck);
            basics.getCore().getEventManager().registerListener(basics, this.afkListener);
            if (autoAfk > 0)
            {
                basics.getCore().getTaskManager().runTimer(basics, this.afkListener, 20, afkCheck / 50); // this is in ticks so /50
            }
        }
    }

    @Command(desc = "Refills your hunger bar")
    public void feed(CommandContext context, @Optional @Label("players") UserList users)
    {
        if (users == null)
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Don't feed the troll!");
                return;
            }
            User sender = (User)context.getSource();
            sender.setFoodLevel(20);
            sender.setSaturation(20);
            sender.setExhaustion(0);
            context.sendTranslated(POSITIVE, "You are now fed!");
            return;
        }
        if (!module.perms().COMMAND_FEED_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to feed other players!");
            return;
        }
        List<User> userList = users.list();
        if (users.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
            }
            context.sendTranslated(POSITIVE, "You made everyone fat!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "shared food with everyone.", context.getSource());
            // TODO MessageType separate for translate Messages and messages from external input e.g. /me
        }
        else
        {
            context.sendTranslated(POSITIVE, "Fed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!users.isAll())
            {
                user.sendTranslated(POSITIVE, "You got fed by {user}!", context.getSource());
            }
            user.setFoodLevel(20);
            user.setSaturation(20);
            user.setExhaustion(0);
        }
    }

    @Command(desc = "Empties the hunger bar")
    public void starve(CommandContext context, @Optional @Label("players") UserList users)
    {
        if (users == null)
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEGATIVE, "\n\n\n\n\n\n\n\n\n\n\n\n\nI'll give you only one line to eat!");
                return;
            }
            User sender = (User)context.getSource();
            sender.setFoodLevel(0);
            sender.setSaturation(0);
            sender.setExhaustion(4);
            context.sendTranslated(NEGATIVE, "You are now starving!");
            return;
        }
        if (!module.perms().COMMAND_STARVE_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to let other players starve!");
            return;
        }
        List<User> userList = users.list();
        if (users.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(NEUTRAL, "You let everyone starve to death!");
            this.um.broadcastStatus(ChatFormat.YELLOW + "took away all food.", context.getSource());
        }
        else
        {
            context.sendTranslated(POSITIVE, "Starved {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!users.isAll())
            {
                user.sendTranslated(NEUTRAL, "You are suddenly starving!");
            }
            user.setFoodLevel(0);
            user.setSaturation(0);
            user.setExhaustion(4);
        }
    }

    @Command(desc = "Heals a player")
    public void heal(CommandContext context, @Optional @Label("players") UserList users)
    {
        if (users == null)
        {
            if (!(context.getSource() instanceof User))
            {
                context.sendTranslated(NEGATIVE, "Only time can heal your wounds!");
                return;
            }
            User sender = (User)context.getSource();
            sender.setHealth(sender.getMaxHealth());
            sender.sendTranslated(POSITIVE, "You are now healed!");
            return;
        }
        if (!module.perms().COMMAND_HEAL_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to heal other players!");
            return;
        }
        List<User> userList = users.list();
        if (users.isAll())
        {
            if (userList.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "There are no players online at the moment!");
                return;
            }
            context.sendTranslated(POSITIVE, "You healed everyone!");
            this.um.broadcastStatus(ChatFormat.BRIGHT_GREEN + "healed every player.", context.getSource());
        }
        else
        {
            context.sendTranslated(POSITIVE, "Healed {amount} players!", userList.size());
        }
        for (User user : userList)
        {
            if (!users.isAll())
            {
                user.sendTranslated(POSITIVE, "You got healed by {sender}!", context.getSource().getName());
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
                return GameMode.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameMode.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameMode.ADVENTURE;
            default:
                return null;
        }
    }

    private GameMode toggleGameMode(GameMode mode)
    {
        switch (mode)
        {
            case SURVIVAL:
                return GameMode.CREATIVE;
            case ADVENTURE:
            case CREATIVE:
            default:
                return GameMode.SURVIVAL;
        }
    }

    @Command(alias = "gm", desc = "Changes the gamemode")
    public void gamemode(CommandContext context, @Optional @Label("gamemode") String gameModeString, @Default @Label("player") User target)
    {
        if (!context.getSource().equals(target) && !module.perms().COMMAND_GAMEMODE_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the game mode of an other player!");
            return;
        }
        GameMode newMode = getGameMode(gameModeString);
        if (newMode == null)
        {
            newMode = toggleGameMode(target.getGameMode());
        }
        target.setGameMode(newMode);
        if (context.getSource().equals(target))
        {
            context.sendTranslated(POSITIVE, "You changed your game mode to {input#gamemode}!", newMode.name());
            return;
        }
        context.sendTranslated(POSITIVE, "You changed the game mode of {user} to {input#gamemode}!", target.getDisplayName(), newMode.name()); // TODO translate gamemode
        target.sendTranslated(NEUTRAL, "Your game mode has been changed to {input#gamemode}!", newMode.name());
    }

    @Command(alias = "slay", desc = "Kills a player")
    public void kill(CommandContext context, @Label("players") UserList users, // TODO default line of sight player
                     @Flag(longName = "force", name = "f") boolean force,
                     @Flag(longName = "quiet", name = "q") boolean quiet,
                     @Flag(longName = "lightning", name = "l") boolean lightning)
    {
        lightning = lightning && module.perms().COMMAND_KILL_LIGHTNING.isAuthorized(context.getSource());
        force = force && module.perms().COMMAND_KILL_FORCE.isAuthorized(context.getSource());
        quiet = quiet && module.perms().COMMAND_KILL_QUIET.isAuthorized(context.getSource());
        List<String> killed = new ArrayList<>();
        Object arg0 = context.get(0);
        List<User> userList = users.list();
        if (users.isAll())
        {
            if (!module.perms().COMMAND_KILL_ALL.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to kill everyone!");
                return;
            }
            if (context.getSource() instanceof User)
            {
                userList.remove(context.getSource());
            }
        }
        for (User user : userList)
        {
            if (this.kill(user, lightning, context, false, force, quiet))
            {
                killed.add(user.getDisplayName());
            }
        }
        if (!killed.isEmpty())
        {
            context.sendTranslated(POSITIVE, "You killed {user#list}!", StringUtils.implode(",", killed));
            return;
        }
        if (arg0 instanceof List && ((List)arg0).size() == 1)
        {
            context.sendTranslated(NEGATIVE, "Could not kill {user}", ((List)arg0).get(0));
            return;
        }
        context.sendTranslated(NEUTRAL, "Could not kill any player!");
    }

    private boolean kill(User user, boolean lightning, CommandContext context, boolean showMessage, boolean force, boolean quiet)
    {
        if (!force)
        {
            if (module.perms().COMMAND_KILL_PREVENT.isAuthorized(user) || this.module.getBasicsUser(user).getEntity().getValue(TABLE_BASIC_USER.GODMODE))
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
            user.sendTranslated(NEUTRAL, "You were killed by {user}", context.getSource());
        }
        return true;
    }

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandContext context, @Label("player") User user)
    {
        if (user.isOnline())
        {
            context.sendTranslated(NEUTRAL, "{user} is currently online!", user);
            return;
        }
        long lastPlayed = user.getLastPlayed();
        if (System.currentTimeMillis() - lastPlayed <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            context.sendTranslated(NEUTRAL, "{user} was last seen {input#date}.", user, TimeUtil.format(context.getSource().getLocale(), new Date(lastPlayed)));
            return;
        }
        Date date = new Date(lastPlayed);
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getSource().getLocale());
        context.sendTranslated(NEUTRAL, "{user} is offline since {input#time}", user, format.format(date));
    }

    @Command(desc = "Makes a player send a message (including commands)")
    public void sudo(CommandContext context, @Label("player") User user, @Label("message") @Greed(INFINITE) String message)
    {
        if (!message.startsWith("/"))
        {
            user.chat(message);
            context.sendTranslated(POSITIVE, "Forced {user} to chat: {input#message}", user, message);
            return;
        }
        if (this.module.getCore().getCommandManager().runCommand(user, message.substring(1)))
        {
            context.sendTranslated(POSITIVE, "Command {input#command} executed as {user}", message, user);
            return;
        }
        context.sendTranslated(NEGATIVE, "Command was not executed successfully!");
    }

    @Command(desc = "Kills yourself")
    @Restricted(value = User.class, msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}") // TODO replace User.class /w interface that has life stuff?
    public void suicide(CommandContext context)
    {
        User sender = (User)context.getSource();
        sender.setHealth(0);
        sender.setLastDamageCause(new EntityDamageEvent(sender, EntityDamageEvent.DamageCause.CUSTOM, sender.getMaxHealth()));
        context.sendTranslated(NEGATIVE, "You ended your life. Why? {text:\\:(:color=DARK_RED}");
    }

    @Command(desc = "Displays that you are afk")
    public void afk(CommandContext context, @Label("player") @Default User user)
    {
        if (!context.getSource().equals(user))
        {
            context.ensurePermission(module.perms().COMMAND_AFK_OTHER);
        }
        if (!user.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", user);
            return;
        }
        if (user.get(BasicsAttachment.class).isAfk())
        {
            user.get(BasicsAttachment.class).updateLastAction();
            this.afkListener.run();
            return;
        }
        user.get(BasicsAttachment.class).setAfk(true);
        user.get(BasicsAttachment.class).resetLastAction();
        this.um.broadcastStatus("is now afk.", user);
    }

    @Command(desc = "Displays informations from a player!")
    public void whois(CommandContext context, @Label("player") User user)
    {
        if (!user.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user} ({text:offline})", user);
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user}", user);
        }
        if (user.hasPlayedBefore() || user.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", user.getHealth(), user.getMaxHealth());
            context.sendTranslated(NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", user.getFoodLevel(), (int)user.getSaturation(), user.getFoodLevel());
            context.sendTranslated(NEUTRAL, "Level: {integer#level} + {integer#percent}%", user.getLevel(), (int)(user.getExp() * 100));
            Location loc = user.getLocation();
            if (loc != null)
            {
                context.sendTranslated(NEUTRAL, "Position: {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld());
            }
            if (user.getAddress() != null)
            {
                context.sendTranslated(NEUTRAL, "IP: {input#ip}", user.getAddress().getAddress().getHostAddress());
            }
            if (user.getGameMode() != null)
            {
                context.sendTranslated(NEUTRAL, "Gamemode: {input#gamemode}", user.getGameMode().toString());
            }
            if (user.getAllowFlight())
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", user.isFlying() ? "flying" : "not flying");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            if (user.isOp())
            {
                context.sendTranslated(NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }
            Timestamp muted = module.getBasicsUser(user).getEntity().getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && muted.getTime() > System.currentTimeMillis())
            {
                context.sendTranslated(NEUTRAL, "Muted until {input#time}", DateFormat.getDateTimeInstance(SHORT, SHORT, context.getSource().getLocale()).format(muted));
            }
            if (user.getGameMode() != GameMode.CREATIVE)
            {
                context.sendTranslated(NEUTRAL, "GodMode: {input#godmode}", user.isInvulnerable() ? ChatFormat.BRIGHT_GREEN + "true" : ChatFormat.RED + "false");
            }
            if (user.get(BasicsAttachment.class).isAfk())
            {
                context.sendTranslated(NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SHORT, SHORT, Locale.ENGLISH);
            context.sendTranslated(NEUTRAL, "First played: {input#date}", dateFormat.format(new Date(user.getFirstPlayed())));
        }
        if (this.module.getCore().getBanManager().isUserBanned(user.getUniqueId()))
        {
            UserBan ban = this.module.getCore().getBanManager().getUserBan(user.getUniqueId());
            String expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getSource().getLocale());
            if (ban.getExpires() != null)
            {
                expires = format.format(ban.getExpires());
            }
            else
            {
                expires = context.getSource().getTranslation(NONE, "for ever");
            }
            context.sendTranslated(NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})", ban.getSource(), format.format(ban.getCreated()), ban.getReason(), expires);
        }
    }

    @Command(desc = "Toggles the god-mode!")
    public void god(CommandContext context, @Label("player") @Default User user)
    {
        boolean other = false;
        if (!context.getSource().equals(user))
        {
            if (!module.perms().COMMAND_GOD_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }
        BasicsUserEntity bUser = module.getBasicsUser(user).getEntity();
        bUser.setValue(TABLE_BASIC_USER.GODMODE, !bUser.getValue(TABLE_BASIC_USER.GODMODE));
        BukkitUtils.setInvulnerable(user, bUser.getValue(TABLE_BASIC_USER.GODMODE));
        if (bUser.getValue(TABLE_BASIC_USER.GODMODE))
        {
            if (!other)
            {
                context.sendTranslated(POSITIVE, "You are now invincible!");
                return;
            }
            user.sendTranslated(POSITIVE, "You are now invincible!");
            context.sendTranslated(POSITIVE, "{user} is now invincible!", user);
            return;
        }
        if (!other)
        {
            context.sendTranslated(NEUTRAL, "You are no longer invincible!");
            return;
        }
        user.sendTranslated(NEUTRAL, "You are no longer invincible!");
        context.sendTranslated(NEUTRAL, "{user} is no longer invincible!", user);
    }

    @Command(desc = "Changes your walkspeed.")
    public void walkspeed(CommandContext context, @Label("speed") Float speed, @Default @Label("player") User user)
    {
        boolean other = false;
        if (!context.getSource().equals(user))
        {
            if (!module.perms().COMMAND_WALKSPEED_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to change the walk speed of an other player!");
                return;
            }
            other = true;
        }
        if (!user.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is offline!", user.getName());
            return;
        }
        if (speed >= 0 && speed <= 10)
        {
            user.setWalkSpeed(speed / 10f);
            user.sendTranslated(POSITIVE, "You can now walk at {decimal:2}!", speed);
            return;
        }
        user.setWalkSpeed(0.2f);
        if (speed != null && speed > 9000)
        {
            user.sendTranslated(NEGATIVE, "It's over 9000!");
        }
        user.sendTranslated(NEUTRAL, "Walk speed has to be a Number between {text:0} and {text:10}!");
    }

    @Command(desc = "Lets you fly away")
    public void fly(CommandContext context,@Optional @Label("flyspeed") Float speed, @Default @Named("player") @Label("player") User target)
    {
        // new cmd system does not provide a way for defaultProvider to give custom messages
        //context.sendTranslated(NEUTRAL, "{text:ProTip}: If your server flies away it will go offline.");
        //context.sendTranslated(NEUTRAL, "So... Stopping the Server in {text:3..:color=RED}");

        // PermissionChecks
        if (!context.getSource().equals(target) && !module.perms().COMMAND_FLY_OTHER.isAuthorized(context.getSource()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the fly mode of other player!");
            return;
        }
        //I Believe I Can Fly ...
        if (speed != null)
        {
            if (speed >= 0 && speed <= 10)
            {
                target.setFlySpeed(speed / 10f);
                target.sendTranslated(POSITIVE, "You can now fly at {decimal#speed:2}!", speed);
                if (!target.equals(context.getSource()))
                {
                    context.sendTranslated(POSITIVE, "{player} can now fly at {decimal#speed:2}!", target, speed);
                }
            }
            else
            {
                if (speed > 9000)
                {
                    context.sendTranslated(NEUTRAL, "It's over 9000!");
                }
                context.sendTranslated(NEGATIVE, "FlySpeed has to be a Number between {text:0} and {text:10}!");
            }
            target.setAllowFlight(true);
            target.setFlying(true);
            return;
        }
        target.setAllowFlight(!target.getAllowFlight());
        if (target.getAllowFlight())
        {
            target.setFlySpeed(0.1f);
            target.sendTranslated(POSITIVE, "You can now fly!");
            if (!target.equals(context.getSource()))
            {
                context.sendTranslated(POSITIVE, "{player} can now fly!", target);
            }
            return;
        }
        target.sendTranslated(NEUTRAL, "You cannot fly anymore!");
        if (!target.equals(context.getSource()))
        {
            context.sendTranslated(POSITIVE, "{player} cannot fly anymore!", target);
        }
    }
}
