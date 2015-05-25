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
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.service.user.UserManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.cubeisland.engine.module.core.util.ChatFormat.DARK_GREEN;
import de.cubeisland.engine.module.core.util.ChatFormat.WHITE;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.ChatFormat.DARK_GREEN;
import static de.cubeisland.engine.module.core.util.ChatFormat.WHITE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

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
    private UserManager um;

    public TeleportCommands(Basics module, UserManager um)
    {
        this.module = module;
        this.um = um;
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
    public void tp(CommandSender context, User player, @Optional User target, @Flag boolean force, @Flag boolean unsafe)
    {
        if (target == null)
        {
            target = player;
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "You have to provide both players");
                return;
            }
            player = (User)context;
        }
        if (!target.isOnline())
        {
            context.sendTranslated(NEGATIVE, "Teleportation only works with online players!");
            return;
        }
        force = force && module.perms().COMMAND_TP_FORCE.isAuthorized(context);
        if (!context.equals(player) && !module.perms().COMMAND_TP_OTHER.isAuthorized(context)) // teleport other persons
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        if (!context.equals(player))
        {
            if (module.perms().TELEPORT_PREVENT_TP.isAuthorized(player)) // teleport the user
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport {user}!", player);
                return;
            }
        } // else equals tp -> no need to check tp perm
        if (!context.equals(target))
        {
            if (module.perms().TELEPORT_PREVENT_TPTO.isAuthorized(target)) // teleport to the target
            {
                if (module.perms().COMMAND_TP_FORCE.isAuthorized(context))
                {
                    context.sendTranslated(POSITIVE, "Use the {text:-force (-f)} flag to teleport to this player."); //Show force flag if has permission
                }
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport to {user}!", target);
                return;
            }
        } // else equals tphere -> no need to check tpto perm

        if (player.equals(target))
        {
            if (context.equals(player))
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
        for (User p : um.getOnlineUsers())
        {
            if (!force && module.perms().TELEPORT_PREVENT_TP.isAuthorized(p))
            {
                noTp.add(p.getName());
                continue;
            }
            if (!teleport(um.getExactUser(p.getUniqueId()), player.getLocation(), !unsafe,
                          force, true))
            {
                noTp.add(p.getName());
            }
        }
        um.broadcastTranslated(POSITIVE, "Teleporting everyone to {user}", player);
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
        for (User player : um.getOnlineUsers())
        {
            if (!force && module.perms().TELEPORT_PREVENT_TP.isAuthorized(player))
            {
                noTp.add(player.getName());
                continue;
            }
            if (!teleport(player, sender.getLocation(), !unsafe, force, true))
            {
                noTp.add(player.getName());
            }
        }
        context.sendTranslated(POSITIVE, "You teleported everyone to you!");
        um.broadcastTranslated(POSITIVE, "Teleporting everyone to {sender}", sender);
        if (!noTp.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(
                WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Direct teleport to a coordinate.")
    public void tppos(CommandSender context, Integer x, Integer y, Integer z, // TODO optional y coord
                      @Default @Named({"world", "w"}) World world,
                      @Default @Named({"player", "p"}) User player,
                      @Flag boolean safe)
    {
        Location loc = new Location(world, x, y, z).add(0.5, 0, 0.5);
        safe = safe && module.perms().COMMAND_TPPOS_SAFE.isAuthorized(context);
        if (TeleportCommands.teleport(player, loc, safe, false, true))
        {
            context.sendTranslated(POSITIVE, "Teleported to {vector:x\\=:y\\=:z\\=} in {world}!", new BlockVector3(x, y, z), world);
        }
    }
}
