/*
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
package org.cubeengine.module.teleport.command;

import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.module.teleport.Teleport;
import org.cubeengine.module.teleport.TeleportListener;
import org.cubeengine.module.teleport.permission.TeleportPerm;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.AIR;
import static org.spongepowered.api.block.BlockTypes.GLASS;
import static org.spongepowered.api.util.Direction.DOWN;
import static org.spongepowered.api.util.Direction.UP;

/**
 * Contains commands for fast movement. /up /ascend /descend /jumpto /through
 * /thru /back /place /put /swap
 */
@Singleton
public class MovementCommands
{
    private final Teleport module;
    private TeleportListener tl;
    private I18n i18n;
    private PluginContainer plugin;
    private TeleportPerm perms;

    @Inject
    public MovementCommands(Teleport module, TeleportListener tl, I18n i18n, PluginContainer plugin, TeleportPerm perms)
    {
        this.module = module;
        this.tl = tl;
        this.i18n = i18n;
        this.plugin = plugin;
        this.perms = perms;
    }

    @Command(desc = "Teleports you X amount of blocks into the air and puts a glass block beneath you.")
    @Restricted
    public void up(ServerPlayer context, Integer height)
    {
        if (height < 0)
        {
            i18n.send(context, NEGATIVE, "Invalid height. The height has to be a whole number greater than 0!");
            return;
        }
        ServerLocation loc = context.getServerLocation().add(0, height - 1, 0);

        if (loc.getBlockY() > loc.getWorld().getMaximumHeight()) // Over highest loc
        {
            loc.add(0, loc.getWorld().getMaximumHeight() - loc.getY(), 0);
        }
        ServerLocation up1 = loc.relativeTo(UP);
        if (!(up1.getBlockType().isAnyOf(AIR) && up1.relativeTo(UP).getBlockType().isAnyOf(AIR)))
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "Your destination seems to be obstructed!");
            return;
        }
        if (loc.getBlockType().isAnyOf(AIR))
        {
            Sponge.getServer().getCauseStackManager().pushCause(context);
            loc.setBlock(GLASS.get().getDefaultState());
        }
        context.setLocation(loc.relativeTo(UP));
        i18n.send(ACTION_BAR, context, POSITIVE, "You have just been lifted!");
    }

    @Command(desc = "Teleports to the highest point at your position.")
    @Restricted(msg = "Pro Tip: Teleport does not work IRL!")
    public void top(ServerPlayer context)
    {
        final Vector3i pos = context.getWorld().getHighestPositionAt(context.getBlockPosition()).add(0.5, 1, 0.5);
        context.setLocation(context.getWorld().getLocation(pos));
        i18n.send(ACTION_BAR, context, POSITIVE, "You are now on top!");
    }

    @Command(desc = "Teleports you to the next safe spot upwards.")
    @Restricted(msg = "Pro Tip: Teleport does not work IRL!")
    public void ascend(ServerPlayer context)
    {
        ServerLocation loc = context.getServerLocation();
        ServerLocation curLoc = loc.add(0, 2, 0);
        final int maxHeight = curLoc.getWorld().getMaximumHeight();
        //go upwards until hitting solid blocks
        while (curLoc.getBlockType().isAnyOf(AIR) && curLoc.getY() < maxHeight)
        {
            ServerLocation rel = curLoc.relativeTo(UP);
            if (rel.getY() < loc.getBlockY())
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "You cannot ascend here");
                return;
            }
            curLoc = rel;
        }
        curLoc = curLoc.relativeTo(UP);
        // go upwards until hitting 2 airblocks again
        while (!(curLoc.getBlockType().isAnyOf(AIR) && curLoc.relativeTo(UP).getBlockType().isAnyOf(AIR)) && curLoc.getY() < maxHeight)
        {
            ServerLocation rel = curLoc.relativeTo(UP);
            if (rel.getY() == 0)
            {
                break;
            }
            curLoc = rel;
        }
        if (curLoc.getY() >= maxHeight)
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You cannot ascend here");
            return;
        }
        curLoc = curLoc.add(0, - (curLoc.getY() - curLoc.getBlockY()), 0);
        context.setLocation(curLoc);
        i18n.send(ACTION_BAR, context, POSITIVE, "Ascended a level!");
    }

    @Command(desc = "Teleports you to the next safe spot downwards.")
    @Restricted(msg = "Pro Tip: Teleport does not work IRL!")
    public void descend(ServerPlayer context)
    {
        ServerLocation curLoc = context.getServerLocation();
        //go downwards until hitting solid blocks
        while (curLoc.getBlockType().isAnyOf(AIR) && curLoc.getBlockY() > 0)
        {
            curLoc = curLoc.add(0, -1, 0);
        }
        // go downwards until hitting 2 airblocks & a solid block again
        while (!((curLoc.getBlockType().isAnyOf(AIR))
            && (curLoc.relativeTo(UP).getBlockType().isAnyOf(AIR))
            && (!curLoc.relativeTo(DOWN).getBlockType().isAnyOf(AIR)))
            && curLoc.getBlockY() > 0)
        {
            curLoc = curLoc.relativeTo(DOWN);
        }
        if (curLoc.getY() <= 1)
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You cannot descend here");
            return;
        }
        //reached new location
        curLoc = curLoc.add(0, - (curLoc.getY() - curLoc.getBlockY()), 0);
        context.setLocation(curLoc);
        i18n.send(ACTION_BAR, context, POSITIVE, "Descended a level!");
    }

    @Command(alias = {"jump", "j"}, desc = "Jumps to the position you are looking at.")
    @Restricted(msg = "Jumping in the console is not allowed! Go play outside!")
    public void jumpTo(ServerPlayer context)
    {
        // TODO this and compass jump can jump outside of worldborder
        // TODO this is broken atm. as blockrays were removed
        ServerLocation loc = LocationUtil.getBlockInSight(context);
        if (loc == null)
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "No block in sight!");
            return;
        }
        context.setLocation(LocationUtil.getLocationUp(loc).add(0.5, 0, 0.5));
        i18n.send(ACTION_BAR, context, POSITIVE, "You just jumped!");
    }

    @Command(alias = "thru", desc = "Jumps to the position you are looking at.")
    @Restricted(msg = "Passing through firewalls in the console is not allowed! Go play outside!")
    public void through(ServerPlayer context)
    {
        // TODO this is broken atm. as blockrays were removed
        Optional<ServerLocation> loc = LocationUtil.getBlockBehindWall(context, this.module.getConfig().navigation.thru.maxRange,
                                                               this.module.getConfig().navigation.thru.maxWallThickness);
        if (!loc.isPresent())
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "Nothing to pass through!");
            return;
        }
        context.setLocation(loc.get());
        i18n.send(ACTION_BAR, context, POSITIVE, "You just passed the wall!");
    }

    @Command(desc = "Teleports you to your last location")
//    @CommandPermission(checkPermission = false) // TODO make the command usage make sense
    @Restricted(msg = "Unfortunately teleporting is still not implemented in the game {text:'Life'}!")
    public void back(ServerPlayer context, @Flag boolean unsafe)
    {
        boolean backPerm = context.hasPermission(perms.COMMAND_BACK_USE.getId());
        if (context.hasPermission(perms.COMMAND_BACK_ONDEATH.getId()))
        {
            ServerLocation loc = tl.getDeathLocation(context);
            if (!backPerm && loc == null)
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "No death point found!");
                return;
            }
            if (loc != null)
            {
                ServerLocation deathLoc = loc;
                ServerLocation safeDeathLoc = Sponge.getServer().getTeleportHelper().getSafeLocation(deathLoc, 5, 20).orElse(null);
                if (safeDeathLoc != null && deathLoc.getPosition().distance(safeDeathLoc.getPosition()) < 5 || unsafe)
                {
                    context.setLocation(unsafe ? deathLoc : safeDeathLoc);
                    i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to your death point!");
                    tl.setDeathLocation(context, null); // reset after back
//                    context.setRotation(loc.getRotation());
                }
                else
                {
                    i18n.send(context, NEGATIVE, "Your death point is unsafe! Use /back -unsafe if you are sure you want to go back there!");
                }
                return;

            }
        }
        if (backPerm)
        {
            ServerLocation trans = tl.getLastLocation(context);
            if (trans == null)
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "You never teleported!");
                return;
            }

            ServerLocation loc = trans;
            if (!unsafe)
            {
                loc = Sponge.getServer().getTeleportHelper().getSafeLocation(loc, 5, 20).orElse(null);
            }
            if (loc == null)
            {
                i18n.send(ACTION_BAR, context, POSITIVE, "Target is unsafe! Use the -unsafe flag to teleport anyways.");
                return;
            }
            context.setLocation(loc);
            i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to your last location!");
//            context.setRotation(trans.getRotation());
            return;
        }
        i18n.send(ACTION_BAR, context, NEGATIVE, "You are not allowed to teleport back!");
    }

    @Command(alias = "put", desc = "Places a player to the position you are looking at.")
    @Restricted
    public void place(ServerPlayer context, ServerPlayer player)
    {
        // TODO this is broken atm. as blockrays were removed
        ServerLocation block = LocationUtil.getBlockInSight(context);
        if (block == null)
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "No block in sight!");
            return;
        }

        player.setLocation(LocationUtil.getLocationUp(block).add(0.5, 0, 0.5));
        i18n.send(ACTION_BAR, context, POSITIVE, "You just placed {user} where you were looking!", player);
        i18n.send(ACTION_BAR, player, POSITIVE, "You were placed somewhere!");
    }

    @Command(desc = "Swaps you and another players position")
    public void swap(CommandCause context, ServerPlayer player, @Default @Label("player") ServerPlayer sender)
    {
        if (player.equals(context.getAudience()))
        {
            if (context.getAudience() instanceof ServerPlayer)
            {
                i18n.send(ACTION_BAR, context, NEGATIVE, "Swapping positions with yourself!? Are you kidding me?");
                return;
            }
            i18n.send(ACTION_BAR, context, NEUTRAL, "Truly a hero! Trying to swap a users position with himself...");
            return;
        }
        ServerLocation userLoc = player.getServerLocation();
        Vector3d userRot = player.getRotation();
        player.setLocation(sender.getServerLocation());
        player.setRotation(sender.getRotation());
        sender.setLocation(userLoc);
        sender.setRotation(userRot);
        if (!context.getAudience().equals(sender))
        {
            i18n.send(ACTION_BAR, context, POSITIVE, "Swapped position of {user} and {user}!", player, sender);
            return;
        }
        i18n.send(ACTION_BAR, context, POSITIVE, "Swapped position with {user}!", player);
    }
}
