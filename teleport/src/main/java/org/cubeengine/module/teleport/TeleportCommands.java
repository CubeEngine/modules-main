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
package org.cubeengine.module.teleport;

import java.util.ArrayList;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.Broadcaster;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.core.util.ChatFormat.DARK_GREEN;
import static org.cubeengine.module.core.util.ChatFormat.WHITE;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

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
    private Game game;
    private Broadcaster bc;
    private TeleportListener tl;
    private I18n i18n;

    public TeleportCommands(Teleport module, Game game, Broadcaster bc, TeleportListener tl, I18n i18n)
    {
        this.module = module;
        this.game = game;
        this.bc = bc;
        this.tl = tl;
        this.i18n = i18n;
    }

    @Command(desc = "Teleport directly to a player.")
    public void tp(CommandSource context, Player player, @Optional Player target, @Flag boolean force, @Flag boolean unsafe)
    {
        if (target == null)
        {
            target = player;
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You have to provide both players");
                return;
            }
            player = (Player)context;
        }
        force = force && context.hasPermission(module.perms().COMMAND_TP_FORCE.getId());
        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_TP_OTHER.getId())) // teleport other persons
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport other people!");
            return;
        }

        if (!context.equals(player))
        {
            if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId())) // teleport the user
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport {user}!", player);
                return;
            }
        } // else equals tp -> no need to check tp perm
        if (!context.equals(target))
        {
            if (target.hasPermission(module.perms().TELEPORT_PREVENT_TPTO.getId())) // teleport to the target
            {
                if (context.hasPermission(module.perms().COMMAND_TP_FORCE.getId()))
                {
                    i18n.sendTranslated(context, POSITIVE, "Use the {text:-force (-f)} flag to teleport to this player."); //Show force flag if has permission
                }
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport to {user}!", target);
                return;
            }
        } // else equals tphere -> no need to check tpto perm

        if (player.equals(target))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, NEUTRAL, "You found yourself!");
                return;
            }
            i18n.sendTranslated(context, NEUTRAL, "You just teleported {user} to {user}... Not very useful right?", player, player);
            return;
        }
        if (!unsafe || player.setLocationSafely(target.getLocation()))
        {
            if (unsafe)
            {
                player.setLocation(target.getLocation());
            }
            i18n.sendTranslated(context, POSITIVE, "You teleported to {user}!", target);
        }
    }

    @Command(desc = "Teleports everyone directly to a player.")
    public void tpall(CommandSource context, Player player, @Flag boolean force, @Flag boolean unsafe)
    {
        force = force && context.hasPermission(module.perms().COMMAND_TPALL_FORCE.getId());
        if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TPTO.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport to {user}!", player);
            return;
        }
        ArrayList<String> noTp = new ArrayList<>();
        for (Player p : game.getServer().getOnlinePlayers())
        {
            if (!force && p.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId()))
            {
                noTp.add(p.getName());
                continue;
            }
            Location target = player.getLocation();
            if (unsafe)
            {
                p.getPlayer().get().setLocation(target);
            }
            else if (!p.getPlayer().get().setLocationSafely(target))
            {
                noTp.add(p.getName());
            }
        }
        bc.broadcastTranslated(POSITIVE, "Teleporting everyone to {user}", player);
        if (!noTp.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Teleport a player directly to you.")
    @Restricted(value = Player.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphere(Player context, Player player, @Flag boolean force, @Flag boolean unsafe)
    {
        force = force && context.hasPermission(module.perms().COMMAND_TPHERE_FORCE.getId());
        if ( context.equals(player))
        {
            i18n.sendTranslated(context, NEUTRAL, "You found yourself!");
            return;
        }
        if (!force && player.hasPermission(module.perms().TELEPORT_PREVENT_TP.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport {user}!", player);
            return;
        }

        if (!unsafe || player.setLocationSafely(context.getLocation()))
        {
            if (unsafe)
            {
                player.setLocation(context.getLocation());
            }
            i18n.sendTranslated(context, POSITIVE, "You teleported {user} to you!", player);
            i18n.sendTranslated(player, POSITIVE, "You were teleported to {sender}", context);
        }
    }

    @Command(desc = "Teleport every player directly to you.")
    @Restricted(value = Player.class, msg = "{text:Pro Tip}: Teleport does not work IRL!")
    public void tphereall(Player context, @Flag boolean force, @Flag boolean unsafe)
    {
        force = force && context.hasPermission(module.perms().COMMAND_TPHEREALL_FORCE.getId());
        ArrayList<String> noTp = new ArrayList<>();
        Location<World> target = context.getLocation();
        for (Player p : game.getServer().getOnlinePlayers())
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
        i18n.sendTranslated(context, POSITIVE, "You teleported everyone to you!");
        bc.broadcastTranslated(POSITIVE, "Teleporting everyone to {sender}", context);
        if (!noTp.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "The following players were not teleported: \n{user#list}", StringUtils.implode(
                WHITE + "," + DARK_GREEN, noTp));
        }
    }

    @Command(desc = "Direct teleport to a coordinate.")
    public void tppos(CommandSource context, Integer x, Integer y, Integer z, // TODO optional y coord
                      @Default @Named({"world", "w"}) World world,
                      @Default @Named({"player", "p"}) Player player,
                      @Flag boolean unsafe)
    {
        Location<World> loc = new Location<>(world, x, y, z).add(0.5, 0, 0.5);
        unsafe = unsafe && context.hasPermission(module.perms().COMMAND_TPPOS_UNSAFE.getId());
        if (!unsafe || player.setLocationSafely(loc))
        {
            if (unsafe)
            {
                player.setLocation(loc);
            }
            i18n.sendTranslated(context, POSITIVE, "Teleported to {vector:x\\=:y\\=:z\\=} in {world}!", new BlockVector3(x, y, z), world);
        }
    }
}
