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

import java.util.Collections;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Flag;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.command.parametric.Label;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.annotation.CommandPermission;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.BlockUtil;
import de.cubeisland.engine.core.util.LocationUtil;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.basics.command.teleport.TeleportCommands.teleport;
import static org.bukkit.Material.AIR;
import static org.bukkit.Material.GLASS;
import static org.bukkit.block.BlockFace.DOWN;
import static org.bukkit.block.BlockFace.UP;

/**
 * Contains commands for fast movement. /up /ascend /descend /jumpto /through
 * /thru /back /place /put /swap
 */
public class MovementCommands
{
    private final Basics module;

    public MovementCommands(Basics module)
    {
        this.module = module;
    }

    @Command(desc = "Teleports you X amount of blocks into the air and puts a glass block beneath you.")
    public void up(User context, Integer height)
    {
        if (height < 0)
        {
            context.sendTranslated(NEGATIVE, "Invalid height. The height has to be a whole number greater than 0!");
            return;
        }
        Location loc = context.getLocation();
        loc.add(0, height - 1, 0);
        if (loc.getBlockY() > loc.getWorld().getMaxHeight()) // Over highest loc
        {
            loc.setY(loc.getWorld().getMaxHeight());
        }
        Block block = loc.getWorld().getBlockAt(loc);
        if (!(block.getRelative(UP, 1).getType() == AIR && block.getRelative(UP, 2).getType() == AIR))
        {
            context.sendTranslated(NEGATIVE, "Your destination seems to be obstructed!");
            return;
        }
        loc = loc.getBlock().getLocation();
        loc.add(0.5, 1, 0.5);
        if (block.getType() == AIR)
        {
            block.setType(GLASS);
        }
        if (teleport(context, loc, true, false, true)) // is save anyway so we do not need to check again
        {
            context.sendTranslated(POSITIVE, "You have just been lifted!");
        }
    }

    @Command(desc = "Teleports to the highest point at your position.")
    @Restricted(value = User.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void top(User context)
    {
        Location loc = context.getLocation();
        BlockUtil.getHighestBlockAt(loc).getLocation(loc);
        loc.add(.5, 0, .5);
        if (teleport(context, loc, true, false, true)) // is save anyway so we do not need to check again
        {
            context.sendTranslated(POSITIVE, "You are now on top!");
        }
    }

    @Command(desc = "Teleports you to the next safe spot upwards.")
    @Restricted(value = User.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void ascend(User context)
    {
        Location userLocation = context.getLocation();
        Block curBlock = userLocation.add(0,2,0).getBlock();
        final int maxHeight = curBlock.getWorld().getMaxHeight();
        //go upwards until hitting solid blocks
        while (curBlock.getType() == AIR && curBlock.getY() < maxHeight)
        {
            Block rel = curBlock.getRelative(UP);
            if (rel.getY() < userLocation.getBlockY())
            {
                context.sendTranslated(NEGATIVE, "You cannot ascend here");
                return;
            }
            curBlock = rel;
        }
        curBlock = curBlock.getRelative(UP);
        // go upwards until hitting 2 airblocks again
        while (!(curBlock.getType() == AIR && curBlock.getRelative(DOWN).getType() == AIR) && curBlock.getY() < maxHeight)
        {
            Block rel = curBlock.getRelative(UP);
            if (rel.getY() == 0)
            {
                break;
            }
            curBlock = rel;
        }
        if (curBlock.getY() >= maxHeight)
        {
            context.sendTranslated(NEGATIVE, "You cannot ascend here");
            return;
        }
        userLocation.setY(curBlock.getY() + 1);
        if (teleport(context, userLocation, true, false, true))
        {
            context.sendTranslated(POSITIVE, "Ascended a level!");
        }
    }

    @Command(desc = "Teleports you to the next safe spot downwards.")
    @Restricted(value = User.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void descend(User context)
    {
        final Location userLocation = context.getLocation();
        final Location currentLocation = userLocation.clone();
        //go downwards until hitting solid blocks
        while (currentLocation.getBlock().getType() == AIR && currentLocation.getBlockY() > 0)
        {
            currentLocation.add(0, -1, 0);
        }
        // go downwards until hitting 2 airblocks & a solid block again
        while (!((currentLocation.getBlock().getType() == AIR)
            && (currentLocation.getBlock().getRelative(UP).getType() == AIR)
            && (currentLocation.getBlock().getRelative(DOWN).getType() != AIR))
            && currentLocation.getBlockY() > 0)
        {
            currentLocation.add(0, -1, 0);
        }
        if (currentLocation.getY() <= 1)
        {
            context.sendTranslated(NEGATIVE, "You cannot descend here");
            return;
        }
        //reached new location
        if (teleport(context, currentLocation, true, false, true))
        {
            context.sendTranslated(POSITIVE, "Descended a level!");
        }
    }

    @Command(alias = {"jump", "j"}, desc = "Jumps to the position you are looking at.")
    @Restricted(value = User.class, msg = "Jumping in the console is not allowed! Go play outside!")
    public void jumpTo(User context)
    {
        Location loc = context.getTargetBlock(this.module.getConfiguration().navigation.jumpToMaxRange).getLocation();
        if (loc.getBlock().getType() == AIR)
        {
            context.sendTranslated(NEGATIVE, "No block in sight!");
            return;
        }
        loc.add(0.5, 1, 0.5);
        if (teleport(context, loc, true, false, true))
        {
            context.sendTranslated(POSITIVE, "You just jumped!");
        }
    }

    @Command(alias = "thru", desc = "Jumps to the position you are looking at.")
    @Restricted(value = User.class, msg = "Passing through firewalls in the console is not allowed! Go play outside!")
    public void through(User context)
    {
        Location loc = LocationUtil.getBlockBehindWall(context, this.module.getConfiguration().navigation.thru.maxRange,
                                                               this.module.getConfiguration().navigation.thru.maxWallThickness);
        if (loc == null)
        {
            context.sendTranslated(NEGATIVE, "Nothing to pass through!");
            return;
        }
        if (teleport(context, loc, true, false, true))
        {
            context.sendTranslated(POSITIVE, "You just passed the wall!");
        }
    }

    @Command(desc = "Teleports you to your last location")
    @CommandPermission(checkPermission = false)
    @Restricted(value = User.class, msg = "Unfortunately teleporting is still not implemented in the game {text:'Life'}!")
    public void back(User context, @Flag boolean unsafe)
    {
        boolean backPerm = module.perms().COMMAND_BACK_USE.isAuthorized(context);
        if (module.perms().COMMAND_BACK_ONDEATH.isAuthorized(context))
        {
            Location loc = context.get(BasicsAttachment.class).getDeathLocation();
            if (!backPerm && loc == null)
            {
                context.sendTranslated(NEGATIVE, "No death point found!");
                return;
            }
            if (loc != null)
            {
                if (teleport(context, loc, !unsafe, true, true))
                {
                    context.sendTranslated(POSITIVE, "Teleported to your death point!");
                }
                else
                {
                    context.get(BasicsAttachment.class).setDeathLocation(loc);
                }
                return;
            }
        }
        if (backPerm)
        {
            Location loc = context.get(BasicsAttachment.class).getLastLocation();
            if (loc == null)
            {
                context.sendTranslated(NEGATIVE, "You never teleported!");
                return;
            }
            if (teleport(context, loc, !unsafe, true, true))
            {
                context.sendTranslated(POSITIVE, "Teleported to your last location!");
            }
            return;
        }
        context.sendTranslated(NEGATIVE, "You are not allowed to teleport back!");
    }

    @Command(alias = "put", desc = "Places a player to the position you are looking at.")
    @Restricted(value = User.class)
    public void place(User context, User player)
    {
        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot move an offline player!");
            return;
        }
        Location loc = context.getTargetBlock(Collections.<Material>emptySet(), 350).getLocation();
        if (loc.getBlock().getType() == AIR)
        {
            context.sendTranslated(NEGATIVE, "No block in sight!");
            return;
        }
        loc.add(0.5, 1, 0.5);
        if (teleport(player, loc, true, false, true))
        {
            context.sendTranslated(POSITIVE, "You just placed {user} where you were looking!", player);
            player.sendTranslated(POSITIVE, "You were placed somewhere!");
        }
    }

    @Command(desc = "Swaps you and another players position")
    public void swap(CommandSender context, User player, @Default @Label("player") User sender)
    {
        if (!player.isOnline() || !sender.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot move an offline player!");
            return;
        }
        if (player.equals(context))
        {
            if (context instanceof Player)
            {
                context.sendTranslated(NEGATIVE, "Swapping positions with yourself!? Are you kidding me?");
                return;
            }
            context.sendTranslated(NEUTRAL, "Truly a hero! Trying to swap a users position with himself...");
            return;
        }
        Location userLoc = player.getLocation();
        if (teleport(player, sender.getLocation(), true, false, false))
        {
            if (teleport(sender, userLoc, true, false, false))
            {
                if (!context.equals(sender))
                {
                    context.sendTranslated(POSITIVE, "Swapped position of {user} and {user}!", player, sender);
                    return;
                }
                context.sendTranslated(POSITIVE, "Swapped position with {user}!", player);
                return;
            }
            teleport(player, userLoc, false, true, false);
        }
        context.sendTranslated(NEGATIVE, "Could not teleport both players!");
    }
}
