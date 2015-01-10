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
package de.cubeisland.engine.module.basics.command.teleport;

import java.util.ArrayList;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.core.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.module.basics.Basics;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.ChatFormat.DARK_GREEN;
import static de.cubeisland.engine.core.util.ChatFormat.WHITE;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * Contains commands to teleport to players/worlds/position.
 * /tp
 * /tpall
 * /tphere
 * /tphereall
 * /tppos
 */
public class TeleportCommands
{
    private final Basics module;

    public TeleportCommands(Basics module)
    {
        this.module = module;
    }

    public static boolean teleport(User user, Location loc, boolean safe, boolean force, boolean keepDirection)
    {
        if (safe)
        {
            return user.safeTeleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND, keepDirection);
        }
        if (keepDirection)
        {
            final Location userLocation = user.getLocation();
            loc.setYaw(userLocation.getYaw());
            loc.setPitch(userLocation.getPitch());
        }
        return user.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    @Command(desc = "Teleport directly to a player.")
    public void tp(CommandContext context, @Default User player, User target, @Flag boolean force, @Flag boolean unsafe)
    {
        if (!target.isOnline())
        {
            context.sendTranslated(NEGATIVE, "Teleportation only works with online players!");
            return;
        }
        force = force && module.perms().COMMAND_TP_FORCE.isAuthorized(context.getSource());
        if (!context.getSource().equals(player) && !module.perms().COMMAND_TP_OTHER.isAuthorized(context.getSource())) // teleport other persons
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        if (!context.getSource().equals(player))
        {
            if (module.perms().TELEPORT_PREVENT_TP.isAuthorized(player)) // teleport the user
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport {user}!", user);
                return;
            }
        } // else equals tp -> no need to check tp perm
        if (!context.getSource().equals(target))
        {
            if (module.perms().TELEPORT_PREVENT_TPTO.isAuthorized(target)) // teleport to the target
            {
                if (module.perms().COMMAND_TP_FORCE.isAuthorized(context.getSource()))
                {
                    context.sendTranslated(POSITIVE, "Use the {text:-force (-f)} flag to teleport to this player."); //Show force flag if has permission
                }
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport to {user}!", target);
                return;
            }
        } // else equals tphere -> no need to check tpto perm

        if (player.equals(target))
        {
            if (context.getSource().equals(player))
            {
                context.sendTranslated(NEUTRAL, "You found yourself!");
                return;
            }
            context.sendTranslated(NEUTRAL, "You just teleported {user} to {user}... Not very useful right?", player, player);
            return;
        }
        if (TeleportCommands.teleport(player, target.getLocation(), !unsafe, force, true))
        {
            context.sendTranslated(POSITIVE, "You teleported to {user}!", target);
        }
    }

    @Command(desc = "Teleports everyone directly to a player.")
    public void tpall(CommandContext context, User player, @Flag boolean force, @Flag boolean unsafe)
    {
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport to an offline player!");
            return;
        }
        force = force && module.perms().COMMAND_TPALL_FORCE.isAuthorized(context.getSource());
        if (!force && module.perms().TELEPORT_PREVENT_TPTO.isAuthorized(player))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport to {user}!", player);
            return;
        }
        ArrayList<String> noTp = new ArrayList<>();
        for (Player p : context.getSource().getServer().getOnlinePlayers())
        {
            if (!force && module.perms().TELEPORT_PREVENT_TP.isAuthorized(p))
            {
                noTp.add(p.getName());
                continue;
            }
            if (!teleport(player.getCore().getUserManager().getExactUser(p.getUniqueId()), player.getLocation(), !unsafe,
                          force, true))
            {
                noTp.add(p.getName());
            }
        }
        context.getCore().getUserManager().broadcastTranslated(POSITIVE, "Teleporting everyone to {user}", player);
        if (!noTp.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Teleport a player directly to you.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphere(CommandContext context, User player, @Flag boolean force, @Flag boolean unsafe)
    {
        User sender = (User)context.getSource();
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to you!");
            return;
        }
        force = force && module.perms().COMMAND_TPHERE_FORCE.isAuthorized(sender);
        if ( sender.equals(player))
        {
            context.sendTranslated(NEUTRAL, "You found yourself!");
            return;
        }
        if (!force && module.perms().TELEPORT_PREVENT_TP.isAuthorized(player))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport {user}!", player);
            return;
        }
        if (TeleportCommands.teleport(player, sender.getLocation(), !unsafe, force, true))
        {
            context.sendTranslated(POSITIVE, "You teleported {user} to you!", player);
            player.sendTranslated(POSITIVE, "You were teleported to {sender}", sender);
        }
    }

    @Command(desc = "Teleport every player directly to you.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphereall(CommandContext context, @Flag boolean force, @Flag boolean unsafe)
    {
        User sender = (User)context.getSource();
        force = force && module.perms().COMMAND_TPHEREALL_FORCE.isAuthorized(context.getSource());
        ArrayList<String> noTp = new ArrayList<>();
        for (Player player : context.getSource().getServer().getOnlinePlayers())
        {
            if (!force && module.perms().TELEPORT_PREVENT_TP.isAuthorized(player))
            {
                noTp.add(player.getName());
                continue;
            }
            if (!teleport(sender.getCore().getUserManager().getExactUser(player.getUniqueId()), sender.getLocation(), !unsafe, force, true))
            {
                noTp.add(player.getName());
            }
        }
        context.sendTranslated(POSITIVE, "You teleported everyone to you!");
        context.getCore().getUserManager().broadcastTranslated(POSITIVE, "Teleporting everyone to {sender}", sender);
        if (!noTp.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(WHITE + "," + DARK_GREEN, noTp));
        }
    }
    @Command(desc = "Direct teleport to a coordinate.")
    @Params(positional = {@Param(label = "x", type = Integer.class),
                         @Param(label = "y", type = Integer.class), // TODO optional y coord
                         @Param(label = "z", type = Integer.class)},
            nonpositional = {@Param(names = {"world", "w"}, type = World.class),
                             @Param(names = {"player", "p"}, type = User.class)})
    @Flags(@Flag(longName = "safe", name = "s"))
    public void tppos(CommandContext context)
    {
        User user;
        if (context.hasNamed("player"))
        {
            user = context.get("player");
        }
        else if (context.isSource(User.class))
        {
            user = (User)context.getSource();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "{text:Pro Tip}: Teleport does not work IRL!");
            return;
        }
        Integer x = context.get(0);
        Integer y;
        Integer z;
        World world = user.getWorld();
        if (context.hasNamed("world"))
        {
            world = context.get("world");
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World not found!");
                return;
            }
        }
        if (context.hasPositional(2))
        {
            y = context.get(1, null);
            z = context.get(2, null);
        }
        else
        {
            z = context.get(1, null);
            if (x == null || z == null)
            {
                context.sendTranslated(NEGATIVE, "Coordinates have to be numbers!");
                return;
            }
            y = user.getWorld().getHighestBlockAt(x, z).getY() + 1;
        }
        if (x == null || y == null || z == null)
        {
            context.sendTranslated(NEGATIVE, "Coordinates have to be numbers!");
            return;
        }
        Location loc = new Location(world, x, y, z).add(0.5, 0, 0.5);
        if (TeleportCommands.teleport(user, loc, context.hasFlag("s")
            && module.perms().COMMAND_TPPOS_SAFE.isAuthorized(context.getSource()), false, true))
        {
            context.sendTranslated(POSITIVE, "Teleported to {vector:x\\=:y\\=:z\\=} in {world}!", new BlockVector3(x, y, z), world);
        }
    }
}
