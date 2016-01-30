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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.spongepowered.api.world.World;

public class TimeCommands
{
    @Command(desc = "Changes the time of a world")
    public void time(CommandSender context, @Optional String time,
                     @Named({ "w", "worlds", "in"}) String worlds, // TODO worldlist reader // TODO NParams static label reader
                     @Flag boolean lock)
    {
        Collection<World> worldList;
        if (worlds != null)
        {
            if ("*".equals(worlds))
            {
                worldList = wm.getWorlds();
            }
            else
            {
                worldList = worldMatcher.matchWorlds(worlds);
                for (World world : worldList)
                {
                    if (world == null)
                    {
                        i18n.sendTranslated(context, NEGATIVE, "Could not match all worlds! {input#worlds}", worlds);
                        return;
                    }
                }
            }
        }
        else
        {
            if (!(context instanceof User))
            {
                i18n.sendTranslated(context, NEGATIVE, "You have to specify a world when using this command from the console!");
                return;
            }
            worldList = Collections.singletonList(((User)context).getWorld());
        }
        if (time != null)
        {
            final Long lTime = timeMatcher.matchTimeValue(time);
            if (lTime == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "The time you entered is not valid!");
                return;
            }
            if (worldList.size() == 1)
            {
                i18n.sendTranslated(context, POSITIVE, "The time of {world} have been set to {input#time} ({input#neartime})!", worldList.get(0), timeMatcher.format(
                    lTime), timeMatcher.getNearTimeName(lTime));
            }
            else if ("*".equals(worlds))
            {
                i18n.sendTranslated(context, POSITIVE, "The time of all worlds have been set to {input#time} ({input#neartime})!", timeMatcher.format(lTime), timeMatcher.getNearTimeName(
                    lTime));            }
            else
            {
                i18n.sendTranslated(context, POSITIVE, "The time of {amount} worlds have been set to {input#time} ({input#neartime})!", worldList.size(), timeMatcher.format(
                    lTime), timeMatcher.getNearTimeName(lTime));
            }
            for (World world : worldList)
            {
                this.setTime(world, lTime);
                if (lock)
                {
                    if (this.lockTask.worlds.containsKey(world.getName()))
                    {
                        this.lockTask.remove(world);
                        i18n.sendTranslated(context, POSITIVE, "Time unlocked for {world}!", world);
                    }
                    else
                    {
                        this.lockTask.add(world);
                        i18n.sendTranslated(context, POSITIVE, "Time locked for {world}!", world);
                    }
                }
            }
            return;
        }
        if (lock)
        {
            for (World world : worldList)
            {
                if (this.lockTask.worlds.containsKey(world.getName()))
                {
                    this.lockTask.remove(world);
                    i18n.sendTranslated(context, POSITIVE, "Time unlocked for {world}!", world);
                }
                else
                {
                    this.lockTask.add(world);
                    i18n.sendTranslated(context, POSITIVE, "Time locked for {world}!", world);
                }
            }
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The current time is:");
        for (World world : worldList)
        {
            i18n.sendTranslated(context, NEUTRAL, "{input#time} ({input#neartime}) in {world}.", timeMatcher.format(
                world.getTime()), timeMatcher.getNearTimeName(world.getTime()), world);
        }
    }

    @Command(desc = "Changes the time for a player")
    public void ptime(CommandSender context, String time, @Default User player, @Flag boolean lock) // TODO staticValues = "reset"
    {
        Long lTime = 0L;
        boolean reset = false;
        if ("reset".equalsIgnoreCase(time))
        {
            reset = true;
        }
        else
        {
            lTime = timeMatcher.matchTimeValue(time);
            if (lTime == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "Invalid time format!");
                return;
            }
        }

        if (!context.equals(player) && !module.perms().COMMAND_PTIME_OTHER.isAuthorized(context))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the time of other players!");
            return;
        }
        if (reset)
        {
            player.resetPlayerTime();
            i18n.sendTranslated(context, POSITIVE, "Reseted the time for {user}!", player);
            if (context.equals(player))
            {
                player.sendTranslated(NEUTRAL, "Your time was reset!");
            }
            return;
        }
        String format = timeMatcher.format(lTime);
        String nearTime = timeMatcher.getNearTimeName(lTime);
        if (lock)
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime, false);
            i18n.sendTranslated(context, POSITIVE, "Time locked to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        else
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime - player.getWorld().getTime(), true);
            i18n.sendTranslated(context, POSITIVE, "Time set to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, POSITIVE, "Your time was set to {input#time} ({input#neartime})!", format, nearTime);
        }
    }

    private void setTime(World world, long time)
    {
        world.setTime(time);
    }


    private final class LockTask implements Runnable
    {

        private final Map<String, Long> worlds = new HashMap<>();
        private UUID taskid = null;

        public void add(World world)
        {
            this.worlds.put(world.getName(), world.getTime());
            if (this.taskid == null)
            {
                this.taskid = taskmgr.runTimer(module, this, 10, 10).get();
            }
        }

        public void remove(World world)
        {
            this.worlds.remove(world.getName());
            if (this.taskid != null && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = null;
            }
        }

        @Override
        public void run()
        {
            Iterator<Entry<String, Long>> iter = this.worlds.entrySet().iterator();

            Map.Entry<String, Long> entry;
            World world;
            while (iter.hasNext())
            {
                entry = iter.next();
                world = wm.getWorld(entry.getKey()).orNull();
                if (world != null)
                {
                    world.setTime(entry.getValue());
                }
                else
                {
                    iter.remove();
                }
            }
            if (this.taskid != null && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = null;
            }
        }
    }

}