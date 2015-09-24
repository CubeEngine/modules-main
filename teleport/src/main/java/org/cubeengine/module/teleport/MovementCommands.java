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

import com.google.common.base.Optional;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Label;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.core.util.LocationUtil;
import org.cubeengine.service.command.annotation.CommandPermission;
import org.cubeengine.service.user.MultilingualCommandSource;
import org.cubeengine.service.user.MultilingualPlayer;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

/**
 * Contains commands for fast movement. /up /ascend /descend /jumpto /through
 * /thru /back /place /put /swap
 */
public class MovementCommands
{
    private final Teleport module;
    private TeleportListener tl;

    public MovementCommands(Teleport module, TeleportListener tl)
    {
        this.module = module;
        this.tl = tl;
    }

    @Command(desc = "Teleports you X amount of blocks into the air and puts a glass block beneath you.")
    public void up(MultilingualPlayer context, Integer height)
    {
        if (height < 0)
        {
            context.sendTranslated(NEGATIVE, "Invalid height. The height has to be a whole number greater than 0!");
            return;
        }
        Location<World> loc = context.original().getLocation().add(0, height - 1, 0);
        if (loc.getBlockY() > loc.getExtent().getDimension().getBuildHeight()) // Over highest loc
        {
            loc.add(0, loc.getExtent().getDimension().getBuildHeight() - loc.getY(), 0);
        }
        Location up1 = loc.getRelative(UP);
        if (!(up1.getBlockType() == AIR && up1.getRelative(UP).getBlockType() == AIR))
        {
            context.sendTranslated(NEGATIVE, "Your destination seems to be obstructed!");
            return;
        }
        if (loc.getBlockType() == AIR)
        {
            loc.getExtent().setBlockType(loc.getBlockPosition(), GLASS);
        }
        context.original().setLocation(loc);
        context.sendTranslated(POSITIVE, "You have just been lifted!");
    }

    @Command(desc = "Teleports to the highest point at your position.")
    @Restricted(value = MultilingualPlayer.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void top(MultilingualPlayer context)
    {
        Location<World> loc = BlockUtil.getHighestBlockAt(context.original().getLocation()).add(.5, 0, .5);
        context.original().setLocation(loc);
        context.sendTranslated(POSITIVE, "You are now on top!");
    }

    @Command(desc = "Teleports you to the next safe spot upwards.")
    @Restricted(value = MultilingualPlayer.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void ascend(MultilingualPlayer context)
    {
        Location loc = context.original().getLocation();
        Location curLoc = loc.add(0, 2, 0);
        final int maxHeight = ((World)curLoc.getExtent()).getDimension().getBuildHeight();
        //go upwards until hitting solid blocks
        while (curLoc.getBlockType() == AIR && curLoc.getY() < maxHeight)
        {
            Location rel = curLoc.getRelative(UP);
            if (rel.getY() < loc.getBlockY())
            {
                context.sendTranslated(NEGATIVE, "You cannot ascend here");
                return;
            }
            curLoc = rel;
        }
        curLoc = curLoc.getRelative(UP);
        // go upwards until hitting 2 airblocks again
        while (!(curLoc.getBlockType() == AIR && curLoc.getRelative(DOWN).getBlockType() == AIR) && curLoc.getY() < maxHeight)
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
        loc = loc.add(0, ((World)loc.getExtent()).getDimension().getBuildHeight() - loc.getY() + 1, 0);
        context.original().setLocation(loc);
        context.sendTranslated(POSITIVE, "Ascended a level!");
    }

    @Command(desc = "Teleports you to the next safe spot downwards.")
    @Restricted(value = MultilingualPlayer.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void descend(MultilingualPlayer context)
    {
        final Location userLocation = context.original().getLocation();
        Location curLoc = userLocation;
        //go downwards until hitting solid blocks
        while (curLoc.getBlockType() == AIR && curLoc.getBlockY() > 0)
        {
            curLoc = curLoc.add(0, -1, 0);
        }
        // go downwards until hitting 2 airblocks & a solid block again
        while (!((curLoc.getBlockType() == AIR)
            && (curLoc.getRelative(UP).getBlockType() == AIR)
            && (curLoc.getRelative(DOWN).getBlockType() != AIR))
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
        context.original().setLocation(curLoc);
        context.sendTranslated(POSITIVE, "Descended a level!");
    }

    @Command(alias = {"jump", "j"}, desc = "Jumps to the position you are looking at.")
    @Restricted(value = MultilingualPlayer.class, msg = "Jumping in the console is not allowed! Go play outside!")
    public void jumpTo(MultilingualPlayer context)
    {
        Optional<BlockRayHit<World>> end = BlockRay.from(context.original()).end();
        if (!end.isPresent())
        {
            context.sendTranslated(NEGATIVE, "No block in sight!");
            return;
        }
        Location<World> loc = end.get().getLocation().add(0.5, 1, 0.5);
        context.original().setLocation(loc);
        context.sendTranslated(POSITIVE, "You just jumped!");
    }

    @Command(alias = "thru", desc = "Jumps to the position you are looking at.")
    @Restricted(value = MultilingualPlayer.class, msg = "Passing through firewalls in the console is not allowed! Go play outside!")
    public void through(MultilingualPlayer context)
    {
        Location<World> loc = LocationUtil.getBlockBehindWall(context.original(), this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
        if (loc == null)
        {
            context.sendTranslated(NEGATIVE, "Nothing to pass through!");
            return;
        }
        context.original().setLocation(loc);
        context.sendTranslated(POSITIVE, "You just passed the wall!");
    }

    @Command(desc = "Teleports you to your last location")
    @CommandPermission(checkPermission = false)
    @Restricted(value = MultilingualPlayer.class, msg = "Unfortunately teleporting is still not implemented in the game {text:'Life'}!")
    public void back(MultilingualPlayer context, @Flag boolean unsafe)
    {
        boolean backPerm = context.hasPermission(module.perms().COMMAND_BACK_USE.getId());
        if (context.hasPermission(module.perms().COMMAND_BACK_ONDEATH.getId()))
        {
            Transform<World> loc = tl.getDeathLocation(context.original());
            if (!backPerm && loc == null)
            {
                context.sendTranslated(NEGATIVE, "No death point found!");
                return;
            }
            if (loc != null)
            {
                if (!unsafe || context.original().setLocationSafely(loc.getLocation()))
                {
                    if (unsafe)
                    {
                        context.original().setLocation(loc.getLocation());
                    }
                    context.sendTranslated(POSITIVE, "Teleported to your death point!");
                    tl.setDeathLocation(context.original(), null); // reset after back
                }
                context.original().setRotation(loc.getRotation());
                return;
            }
        }
        if (backPerm)
        {
            Transform<World> loc = tl.getLastLocation(context.original());
            if (loc == null)
            {
                context.sendTranslated(NEGATIVE, "You never teleported!");
                return;
            }

            if (!unsafe || context.original().setLocationSafely(loc.getLocation()))
            {
                if (unsafe)
                {
                    context.original().setLocation(loc.getLocation());
                }
                context.sendTranslated(POSITIVE, "Teleported to your last location!");
            }
            context.original().setRotation(loc.getRotation());
            return;
        }
        context.sendTranslated(NEGATIVE, "You are not allowed to teleport back!");
    }

    @Command(alias = "put", desc = "Places a player to the position you are looking at.")
    @Restricted(value = MultilingualPlayer.class)
    public void place(MultilingualPlayer context, MultilingualPlayer player)
    {
        Optional<BlockRayHit<World>> end = BlockRay.from(context.original()).end();
        if (!end.isPresent())
        {
            context.sendTranslated(NEGATIVE, "No block in sight!");
            return;
        }
        player.original().setLocation(end.get().getLocation().add(0.5, 1, 0.5));
        context.sendTranslated(POSITIVE, "You just placed {user} where you were looking!", player);
        player.sendTranslated(POSITIVE, "You were placed somewhere!");
    }

    @Command(desc = "Swaps you and another players position")
    public void swap(MultilingualCommandSource context, MultilingualPlayer player, @Default @Label("player") MultilingualPlayer sender)
    {
        if (player.equals(context))
        {
            if (context instanceof MultilingualPlayer)
            {
                context.sendTranslated(NEGATIVE, "Swapping positions with yourself!? Are you kidding me?");
                return;
            }
            context.sendTranslated(NEUTRAL, "Truly a hero! Trying to swap a users position with himself...");
            return;
        }
        Location<World> userLoc = player.original().getLocation();
        player.original().setLocation(sender.original().getLocation());
        sender.original().setLocation(userLoc);
        if (!context.equals(sender))
        {
            context.sendTranslated(POSITIVE, "Swapped position of {user} and {user}!", player, sender);
            return;
        }
        context.sendTranslated(POSITIVE, "Swapped position with {user}!", player);
    }
}
