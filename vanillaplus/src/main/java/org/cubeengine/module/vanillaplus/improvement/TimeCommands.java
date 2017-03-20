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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.TimeMatcher;
import org.cubeengine.libcube.service.matcher.WorldMatcher;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class TimeCommands extends PermissionContainer
{
    private final TaskManager tam;
    private I18n i18n;
    private TimeMatcher tm;
    private final WorldMatcher worldMatcher;

    private Map<UUID, UUID> locked = new HashMap<>();

    public TimeCommands(PermissionManager pm, I18n i18n, TimeMatcher tm, WorldMatcher worldMatcher, TaskManager tam)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.tm = tm;
        this.worldMatcher = worldMatcher;
        this.tam = tam;
    }


    @Command(desc = "Changes the time of a world")
    public void time(CommandSource context, @Optional String time,
                     @Named("in") String worlds, // TODO worldlist reader // TODO NParams static label reader
                     @Flag boolean lock)
    {
        Collection<World> worldList;
        if (worlds != null)
        {
            if ("*".equals(worlds))
            {
                worldList = Sponge.getServer().getWorlds();
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
            if (!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "You have to specify a world when using this command from the console!");
                return;
            }
            worldList = Collections.singletonList(((Player)context).getWorld());
        }
        if (time != null)
        {
            Long lTime = tm.matchTimeValue(time);
            if (lTime == null)
            {
                lTime = tm.parseTime(time);
                if (lTime == null)
                {
                    i18n.sendTranslated(context, NEGATIVE, "The time you entered is not valid!");
                    return;
                }

            }
            String timeNumeric = tm.format(lTime);
            String timeName = tm.matchTimeName(lTime);
            if (worldList.size() == 1)
            {
                i18n.sendTranslated(context, POSITIVE,
                                    "The time of {world} have been set to {input#time} ({input#neartime})!",
                                    worldList.iterator().next(), timeNumeric, timeName);
            }
            else if ("*".equals(worlds))
            {
                i18n.sendTranslated(context, POSITIVE,
                                    "The time of all worlds have been set to {input#time} ({input#neartime})!",
                                    timeNumeric, timeName);
            }
            else
            {
                i18n.sendTranslated(context, POSITIVE,
                                    "The time of {amount} worlds have been set to {input#time} ({input#neartime})!",
                                    worldList.size(), timeNumeric, timeName);
            }
            for (World world : worldList)
            {
                this.setTime(world, lTime);
                if (lock)
                {
                    toggleTimeLock(context, world, world.getProperties().getWorldTime());
                }
            }
            return;
        }
        if (lock)
        {
            for (World world : worldList)
            {
                toggleTimeLock(context, world, world.getProperties().getWorldTime());
            }
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The current time is:");
        for (World world : worldList)
        {
            long worldTime = world.getProperties().getWorldTime();
            i18n.sendTranslated(context, NEUTRAL, "{input#time} ({input#neartime}) in {world}.", tm.format(worldTime), tm.matchTimeName(worldTime), world);
        }
    }

    private void toggleTimeLock(CommandSource context, World world, long worldTime)
    {
        if (locked.containsKey(world.getUniqueId()))
        {
            tam.cancelTask(VanillaPlus.class, locked.remove(world.getUniqueId()));
            i18n.sendTranslated(context, POSITIVE, "Time unlocked for {world}!", world);
        }
        else
        {
            locked.put(world.getUniqueId(), tam.runTimer(VanillaPlus.class, () -> setTime(world, worldTime), 0, 10));
            i18n.sendTranslated(context, POSITIVE, "Time locked for {world}!", world);
        }
    }

    // TODO public final Permission COMMAND_PTIME_OTHER = register("command.ptime.other", "", null);

    /* TODO wait for API https://github.com/SpongePowered/SpongeAPI/issues/393
    @Command(desc = "Changes the time for a player")
    public void ptime(CommandSource context, String time, @Default Player player, @Flag boolean lock) // TODO staticValues = "reset"
    {
        Long lTime = 0L;
        boolean reset = false;
        if ("reset".equalsIgnoreCase(time))
        {
            reset = true;
        }
        else
        {
            lTime = tm.matchTimeValue(time);
            if (lTime == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "Invalid time format!");
                return;
            }
        }

        if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_PTIME_OTHER.getId()))
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
                i18n.sendTranslated(player, NEUTRAL, "Your time was reset!");
            }
            return;
        }
        String format = tm.format(lTime);
        String nearTime = tm.matchTimeName(lTime);
        if (lock)
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime, false);
            i18n.sendTranslated(context, POSITIVE, "Time locked to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        else
        {
            player.resetPlayerTime();
            player.setPlayerTime(lTime - player.getWorld().getProperties().getWorldTime(), true);
            i18n.sendTranslated(context, POSITIVE, "Time set to {input#time} ({input#neartime}) for {user}!", format, nearTime, player);
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, POSITIVE, "Your time was set to {input#time} ({input#neartime})!", format, nearTime);
        }
    }
    //*/

    private void setTime(World world, long time)
    {
        world.getProperties().setWorldTime(time);
    }
}
