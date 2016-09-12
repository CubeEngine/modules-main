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

import java.util.Optional;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.libcube.service.command.annotation.CommandPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
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
    private final Teleport module;
    private TeleportListener tl;
    private I18n i18n;

    public MovementCommands(Teleport module, TeleportListener tl, I18n i18n)
    {
        this.module = module;
        this.tl = tl;
        this.i18n = i18n;
    }

    @Command(desc = "Teleports you X amount of blocks into the air and puts a glass block beneath you.")
    public void up(Player context, Integer height)
    {
        if (height < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "Invalid height. The height has to be a whole number greater than 0!");
            return;
        }
        Location<World> loc = context.getLocation().add(0, height - 1, 0);
        if (loc.getBlockY() > loc.getExtent().getDimension().getBuildHeight()) // Over highest loc
        {
            loc.add(0, loc.getExtent().getDimension().getBuildHeight() - loc.getY(), 0);
        }
        Location up1 = loc.getRelative(UP);
        if (!(up1.getBlockType() == AIR && up1.getRelative(UP).getBlockType() == AIR))
        {
            i18n.sendTranslated(context, NEGATIVE, "Your destination seems to be obstructed!");
            return;
        }
        if (loc.getBlockType() == AIR)
        {
            loc.getExtent().setBlockType(loc.getBlockPosition(), GLASS, Cause.of(NamedCause.source(context))); // TODO plugin must be root of cause
        }
        context.setLocation(loc);
        i18n.sendTranslated(context, POSITIVE, "You have just been lifted!");
    }

    @Command(desc = "Teleports to the highest point at your position.")
    @Restricted(value = Player.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void top(Player context)
    {
        Location<World> loc = BlockUtil.getHighestBlockAt(context.getLocation()).add(.5, 0, .5);
        context.setLocation(loc);
        i18n.sendTranslated(context, POSITIVE, "You are now on top!");
    }

    @Command(desc = "Teleports you to the next safe spot upwards.")
    @Restricted(value = Player.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void ascend(Player context)
    {
        Location loc = context.getLocation();
        Location curLoc = loc.add(0, 2, 0);
        final int maxHeight = ((World)curLoc.getExtent()).getDimension().getBuildHeight();
        //go upwards until hitting solid blocks
        while (curLoc.getBlockType() == AIR && curLoc.getY() < maxHeight)
        {
            Location rel = curLoc.getRelative(UP);
            if (rel.getY() < loc.getBlockY())
            {
                i18n.sendTranslated(context, NEGATIVE, "You cannot ascend here");
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
            i18n.sendTranslated(context, NEGATIVE, "You cannot ascend here");
            return;
        }
        loc = loc.add(0, ((World)loc.getExtent()).getDimension().getBuildHeight() - loc.getY() + 1, 0);
        context.setLocation(loc);
        i18n.sendTranslated(context, POSITIVE, "Ascended a level!");
    }

    @Command(desc = "Teleports you to the next safe spot downwards.")
    @Restricted(value = Player.class, msg = "Pro Tip: Teleport does not work IRL!")
    public void descend(Player context)
    {
        final Location<World> userLocation = context.getLocation();
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
            i18n.sendTranslated(context, NEGATIVE, "You cannot descend here");
            return;
        }
        //reached new location
        context.setLocation(curLoc);
        i18n.sendTranslated(context, POSITIVE, "Descended a level!");
    }

    @Command(alias = {"jump", "j"}, desc = "Jumps to the position you are looking at.")
    @Restricted(value = Player.class, msg = "Jumping in the console is not allowed! Go play outside!")
    public void jumpTo(Player context)
    {
        Optional<BlockRayHit<World>> end = BlockRay.from(context).end();
        if (!end.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "No block in sight!");
            return;
        }
        Location<World> loc = end.get().getLocation().add(0.5, 1, 0.5);
        context.setLocation(loc);
        i18n.sendTranslated(context, POSITIVE, "You just jumped!");
    }

    @Command(alias = "thru", desc = "Jumps to the position you are looking at.")
    @Restricted(value = Player.class, msg = "Passing through firewalls in the console is not allowed! Go play outside!")
    public void through(Player context)
    {
        Optional<Location<World>> loc = LocationUtil.getBlockBehindWall(context, this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
        if (!loc.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "Nothing to pass through!");
            return;
        }
        context.setLocation(loc.get());
        i18n.sendTranslated(context, POSITIVE, "You just passed the wall!");
    }

    @Command(desc = "Teleports you to your last location")
    @CommandPermission(checkPermission = false)
    @Restricted(value = Player.class, msg = "Unfortunately teleporting is still not implemented in the game {text:'Life'}!")
    public void back(Player context, @Flag boolean unsafe)
    {
        boolean backPerm = context.hasPermission(module.perms().COMMAND_BACK_USE.getId());
        if (context.hasPermission(module.perms().COMMAND_BACK_ONDEATH.getId()))
        {
            Transform<World> loc = tl.getDeathLocation(context);
            if (!backPerm && loc == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "No death point found!");
                return;
            }
            if (loc != null)
            {
                if (!unsafe || context.setLocationSafely(loc.getLocation()))
                {
                    if (unsafe)
                    {
                        context.setLocation(loc.getLocation());
                    }
                    i18n.sendTranslated(context, POSITIVE, "Teleported to your death point!");
                    tl.setDeathLocation(context, null); // reset after back
                }
                context.setRotation(loc.getRotation());
                return;
            }
        }
        if (backPerm)
        {
            Transform<World> loc = tl.getLastLocation(context);
            if (loc == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "You never teleported!");
                return;
            }

            if (!unsafe || context.setLocationSafely(loc.getLocation()))
            {
                if (unsafe)
                {
                    context.setLocation(loc.getLocation());
                }
                i18n.sendTranslated(context, POSITIVE, "Teleported to your last location!");
            }
            context.setRotation(loc.getRotation());
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "You are not allowed to teleport back!");
    }

    @Command(alias = "put", desc = "Places a player to the position you are looking at.")
    @Restricted(value = Player.class)
    public void place(Player context, Player player)
    {
        Optional<BlockRayHit<World>> end = BlockRay.from(context).end();
        if (!end.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "No block in sight!");
            return;
        }
        player.setLocation(end.get().getLocation().add(0.5, 1, 0.5));
        i18n.sendTranslated(context, POSITIVE, "You just placed {user} where you were looking!", player);
        i18n.sendTranslated(player, POSITIVE, "You were placed somewhere!");
    }

    @Command(desc = "Swaps you and another players position")
    public void swap(CommandSource context, Player player, @Default @Label("player") Player sender)
    {
        if (player.equals(context))
        {
            if (context instanceof Player)
            {
                i18n.sendTranslated(context, NEGATIVE, "Swapping positions with yourself!? Are you kidding me?");
                return;
            }
            i18n.sendTranslated(context, NEUTRAL, "Truly a hero! Trying to swap a users position with himself...");
            return;
        }
        Location<World> userLoc = player.getLocation();
        player.setLocation(sender.getLocation());
        sender.setLocation(userLoc);
        if (!context.equals(sender))
        {
            i18n.sendTranslated(context, POSITIVE, "Swapped position of {user} and {user}!", player, sender);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Swapped position with {user}!", player);
    }
}
