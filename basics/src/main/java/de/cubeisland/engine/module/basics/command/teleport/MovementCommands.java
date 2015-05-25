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

import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import de.cubeisland.engine.module.core.util.BlockUtil;
import de.cubeisland.engine.module.core.util.LocationUtil;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.annotation.CommandPermission;
import de.cubeisland.engine.module.service.user.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.basics.command.teleport.TeleportCommands.teleport;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;

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
        Location loc = context.getLocation().add(0, height - 1, 0);
        if (loc.getBlockY() > ((World)loc.getExtent()).getBuildHeight()) // Over highest loc
        {
            loc.setY(((World)loc.getExtent()).getBuildHeight());
        }
        Location up1 = loc.getRelative(UP);
        if (!(up1.getType() == AIR && up1.getRelative(UP).getType() == AIR))
        {
            context.sendTranslated(NEGATIVE, "Your destination seems to be obstructed!");
            return;
        }
        if (loc.getType() == AIR)
        {
            loc.getExtent().setBlockType(loc.getBlockPosition(), GLASS);
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
        Location loc = BlockUtil.getHighestBlockAt(context.getLocation()).add(.5, 0, .5);
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
        Location curLoc = userLocation.add(0, 2, 0);
        final int maxHeight = ((World)curLoc.getExtent()).getBuildHeight();
        //go upwards until hitting solid blocks
        while (curLoc.getType() == AIR && curLoc.getY() < maxHeight)
        {
            Location rel = curLoc.getRelative(UP);
            if (rel.getY() < userLocation.getBlockY())
            {
                context.sendTranslated(NEGATIVE, "You cannot ascend here");
                return;
            }
            curLoc = rel;
        }
        curLoc = curLoc.getRelative(UP);
        // go upwards until hitting 2 airblocks again
        while (!(curLoc.getType() == AIR && curLoc.getRelative(DOWN).getType() == AIR) && curLoc.getY() < maxHeight)
        {
            Location rel = curLoc.getRelative(UP);
            if (rel.getY() == 0)
            {
                break;
            }
            curLoc = rel;
        }
        if (curLoc.getY() >= maxHeight)
        {
            context.sendTranslated(NEGATIVE, "You cannot ascend here");
            return;
        }
        userLocation.setY(curLoc.getY() + 1);
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
        Location curLoc = userLocation;
        //go downwards until hitting solid blocks
        while (curLoc.getType() == AIR && curLoc.getBlockY() > 0)
        {
            curLoc = curLoc.add(0, -1, 0);
        }
        // go downwards until hitting 2 airblocks & a solid block again
        while (!((curLoc.getType() == AIR)
            && (curLoc.getRelative(UP).getType() == AIR)
            && (curLoc.getRelative(DOWN).getType() != AIR))
            && curLoc.getBlockY() > 0)
        {
            curLoc = curLoc.add(0, -1, 0);
        }
        if (curLoc.getY() <= 1)
        {
            context.sendTranslated(NEGATIVE, "You cannot descend here");
            return;
        }
        //reached new location
        if (teleport(context, curLoc, true, false, true))
        {
            context.sendTranslated(POSITIVE, "Descended a level!");
        }
    }

    @Command(alias = {"jump", "j"}, desc = "Jumps to the position you are looking at.")
    @Restricted(value = User.class, msg = "Jumping in the console is not allowed! Go play outside!")
    public void jumpTo(User context)
    {
        Location loc = context.getTargetBlock(this.module.getConfiguration().navigation.jumpToMaxRange);
        if (loc.getType() == AIR)
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
        Location loc = context.getTargetBlock(350, AIR);
        if (loc.getType() == AIR)
        {
            context.sendTranslated(NEGATIVE, "No block in sight!");
            return;
        }
        loc = loc.add(0.5, 1, 0.5);
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
            if (context instanceof User)
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
