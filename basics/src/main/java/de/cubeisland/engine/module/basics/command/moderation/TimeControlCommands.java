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
package de.cubeisland.engine.module.basics.command.moderation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.parameter.IncorrectUsageException;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.task.TaskManager;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * Commands changing time. /time /ptime
 */
public class TimeControlCommands
{

    private final Basics module;
    private final TaskManager taskmgr;
    private final LockTask lockTask;

    public TimeControlCommands(Basics module)
    {
        this.module = module;
        this.taskmgr = module.getCore().getTaskManager();
        this.lockTask = new LockTask();
    }

    @Command(desc = "Changes the time of a world")
    @Params(positional = @Param(label = "time", req = false),
            nonpositional = @Param(names = { "w", "worlds", "in"})) // TODO worldlist reader // TODO NParams static label reader
    @Flags(@Flag(longName = "lock", name = "l"))
    public void time(CommandContext context)
    {
        List<World> worlds;
        if (context.hasNamed("w"))
        {
            if (context.getString("w").equals("*"))
            {
                worlds = Bukkit.getWorlds();
            }
            else
            {
                worlds = Match.worlds().matchWorlds(context.getString("w"));
                for (World world : worlds)
                {
                    if (world == null)
                    {
                        context.sendTranslated(NEGATIVE, "Could not match all worlds! {input#worlds}", context.getString("w"));
                        return;
                    }
                }
            }
        }
        else
        {
            if (context.getSource() instanceof User)
            {
                worlds = Arrays.asList(((User)context.getSource()).getWorld());
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You have to specify a world when using this command from the console!");
                return;
            }
        }
        if (context.hasPositional(0))
        {
            final Long time = Match.time().matchTimeValue(context.getString(0));
            if (time == null)
            {
                context.sendTranslated(NEGATIVE, "The time you entered is not valid!");
                return;
            }
            if (worlds.size() == 1)
            {
                context.sendTranslated(POSITIVE, "The time of {world} have been set to {input#time} ({input#neartime})!", worlds.get(0), Match.time().format(time), Match.time().getNearTimeName(time));
            }
            else if ("*".equals(context.getString("w")))
            {
                context.sendTranslated(POSITIVE, "The time of all worlds have been set to {input#time} ({input#neartime})!", Match.time().format(time), Match.time().getNearTimeName(time));
            }
            else
            {
                context.sendTranslated(POSITIVE, "The time of {amount} worlds have been set to {input#time} ({input#neartime})!", worlds.size(), Match.time().format(time), Match.time().getNearTimeName(time));
            }
            for (World world : worlds)
            {
                this.setTime(world, time);
                if (context.hasFlag("l"))
                {
                    if (this.lockTask.worlds.containsKey(world.getName()))
                    {
                        this.lockTask.remove(world);
                        context.sendTranslated(POSITIVE, "Time unlocked for {world}!", world);
                    }
                    else
                    {
                        this.lockTask.add(world);
                        context.sendTranslated(POSITIVE, "Time locked for {world}!", world);
                    }
                }
            }
        }
        else
        {
            if (context.hasFlag("l"))
            {
                for (World world : worlds)
                {
                    if (this.lockTask.worlds.containsKey(world.getName()))
                    {
                        this.lockTask.remove(world);
                        context.sendTranslated(POSITIVE, "Time unlocked for {world}!", world);
                    }
                    else
                    {
                        this.lockTask.add(world);
                        context.sendTranslated(POSITIVE, "Time locked for {world}!", world);
                    }
                }
            }
            else
            {
                context.sendTranslated(POSITIVE, "The current time is:");
                for (World world : worlds)
                {
                    context.sendTranslated(NEUTRAL, "{input#time} ({input#neartime}) in {world}.", Match.time().format(world.getTime()), Match.time().getNearTimeName(world.getTime()), world);
                }
            }
        }
    }

    @Command(desc = "Changes the time for a player")
    @Params(positional = {@Param(label = "time"), // TODO staticValues = "reset"
                          @Param(req = false, label = "player", type = User.class)})
    @Flags(@Flag(longName = "lock", name = "l"))
    public void ptime(CommandContext context)
    {
        Long time = 0L;
        boolean other = false;
        boolean reset = false;
        String timeString = context.get(0);
        if (timeString.equalsIgnoreCase("reset"))
        {
            reset = true;
        }
        else
        {
            time = Match.time().matchTimeValue(timeString);
            if (time == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid time format!");
                return;
            }
        }

        User user = null;
        if (context.getSource() instanceof User)
        {
            user = (User)context.getSource();
        }
        if (context.hasPositional(1))
        {
            user = context.get(1);
            if (!module.perms().COMMAND_PTIME_OTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to change the time of other players!");
                return;
            }
            other = true;
        }
        else if (user == null)
        {
            context.sendTranslated(NEGATIVE, "You need to define a player!");
            return;
        }
        if (reset)
        {
            user.resetPlayerTime();
            context.sendTranslated(POSITIVE, "Reseted the time for {user}!", user);
            if (other)
            {
                user.sendTranslated(NEUTRAL, "Your time was reset!");
            }
        }
        else
        {
            if (context.hasFlag("l"))
            {
                user.resetPlayerTime();
                user.setPlayerTime(time, false);
                context.sendTranslated(POSITIVE, "Time locked to {input#time} ({input#neartime}) for {user}!", Match.time().format(time), Match.time().getNearTimeName(time), user);
            }
            else
            {
                user.resetPlayerTime();
                user.setPlayerTime(time - user.getWorld().getTime(), true);
                context.sendTranslated(POSITIVE, "Time set to {input#time} ({input#neartime}) for {user}!", Match.time().format(time), Match.time().getNearTimeName(time), user);
            }
            if (other)
            {
                context.sendTranslated(POSITIVE, "Your time was set to {input#time} ({input#neartime})!", Match.time().format(time), Match.time().getNearTimeName(time));
            }
        }
    }

    private void setTime(World world, long time)
    {
        world.setTime(time);
    }

    private final class LockTask implements Runnable
    {

        private final Map<String, Long> worlds = new HashMap<>();
        private int taskid = -1;

        public void add(World world)
        {
            this.worlds.put(world.getName(), world.getTime());
            if (this.taskid == -1)
            {
                this.taskid = taskmgr.runTimer(module, this, 10, 10);
            }
        }

        public void remove(World world)
        {
            this.worlds.remove(world.getName());
            if (this.taskid != -1 && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = -1;
            }
        }

        @Override
        public void run()
        {
            Iterator<Map.Entry<String, Long>> iter = this.worlds.entrySet().iterator();

            Map.Entry<String, Long> entry;
            World world;
            while (iter.hasNext())
            {
                entry = iter.next();
                world = Bukkit.getWorld(entry.getKey());
                if (world != null)
                {
                    world.setTime(entry.getValue());
                }
                else
                {
                    iter.remove();
                }
            }
            if (this.taskid != -1 && this.worlds.isEmpty())
            {
                taskmgr.cancelTask(module, this.taskid);
                this.taskid = -1;
            }
        }
    }
}
