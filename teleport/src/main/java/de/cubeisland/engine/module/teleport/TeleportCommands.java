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
package de.cubeisland.engine.module.teleport;

import java.util.ArrayList;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.core.util.ChatFormat.DARK_GREEN;
import static org.cubeengine.module.core.util.ChatFormat.WHITE;

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
    private final Teleport module;
    private UserManager um;

    public TeleportCommands(Teleport module, UserManager um)
    {
        this.module = module;
        this.um = um;
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
        if (!target.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "Teleportation only works with online players!");
            return;
        }
        force = force && context.hasPermission(module.perms().COMMAND_TP_FORCE.getId());
        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_TP_OTHER.getId())) // teleport other persons
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        if (!context.equals(player))
        {
            if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId())) // teleport the user
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to teleport {user}!", player);
                return;
            }
        } // else equals tp -> no need to check tp perm
        if (!context.equals(target))
        {
            if (target.hasPermission(module.perms().TELEPORT_PREVENT_TPTO.getId())) // teleport to the target
            {
                if (context.hasPermission(module.perms().COMMAND_TP_FORCE.getId()))
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
        if (!unsafe || player.getPlayer().get().setLocationSafely(target.asPlayer().getLocation()))
        {
            if (unsafe)
            {
                player.getPlayer().get().setLocation(target.asPlayer().getLocation());
            }
            context.sendTranslated(POSITIVE, "You teleported to {user}!", target);
        }
    }

    @Command(desc = "Teleports everyone directly to a player.")
    public void tpall(CommandContext context, User player, @Flag boolean force, @Flag boolean unsafe)
    {
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport to an offline player!");
            return;
        }
        force = force && context.getSource().hasPermission(module.perms().COMMAND_TPALL_FORCE.getId());
        if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TPTO.getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport to {user}!", player);
            return;
        }
        ArrayList<String> noTp = new ArrayList<>();
        for (User p : um.getOnlineUsers())
        {
            if (!force && p.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.getName());
                continue;
            }
            Location target = player.asPlayer().getLocation();
            if (unsafe)
            {
                p.getPlayer().get().setLocation(target);
            }
            else if (!p.getPlayer().get().setLocationSafely(target))
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
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to you!");
            return;
        }
        force = force && sender.hasPermission(module.perms().COMMAND_TPHERE_FORCE.getId());
        if ( sender.equals(player))
        {
            context.sendTranslated(NEUTRAL, "You found yourself!");
            return;
        }
        if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId()))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to teleport {user}!", player);
            return;
        }

        if (!unsafe || player.getPlayer().get().setLocationSafely(sender.asPlayer().getLocation()))
        {
            if (unsafe)
            {
                player.getPlayer().get().setLocation(sender.asPlayer().getLocation());
            }
            context.sendTranslated(POSITIVE, "You teleported {user} to you!", player);
            player.sendTranslated(POSITIVE, "You were teleported to {sender}", sender);
        }
    }

    @Command(desc = "Teleport every player directly to you.")
    @Restricted(value = User.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphereall(CommandContext context, @Flag boolean force, @Flag boolean unsafe)
    {
        User sender = (User)context.getSource();
        force = force && context.getSource().hasPermission(module.perms().COMMAND_TPHEREALL_FORCE.getId());
        ArrayList<String> noTp = new ArrayList<>();
        Location target = sender.asPlayer().getLocation();
        for (User p : um.getOnlineUsers())
        {
            if (!force && p.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.getName());
                continue;
            }
            if (unsafe)
            {
                p.getPlayer().get().setLocation(target);
            }
            else if (!p.getPlayer().get().setLocationSafely(target))
            {
                noTp.add(p.getName());
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
                      @Flag boolean unsafe)
    {
        Location loc = new Location(world, x, y, z).add(0.5, 0, 0.5);
        unsafe = unsafe && context.hasPermission(module.perms().COMMAND_TPPOS_UNSAFE.getId());
        if (!unsafe || player.getPlayer().get().setLocationSafely(loc))
        {
            if (unsafe)
            {
                player.getPlayer().get().setLocation(loc);
            }
            context.sendTranslated(POSITIVE, "Teleported to {vector:x\\=:y\\=:z\\=} in {world}!", new BlockVector3(x, y, z), world);
        }
    }
}
