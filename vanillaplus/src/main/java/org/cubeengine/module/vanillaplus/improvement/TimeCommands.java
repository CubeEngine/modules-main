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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.TimeMatcher;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.util.MinecraftDayTime;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.gamerule.GameRules;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class TimeCommands extends PermissionContainer
{
    private final TaskManager tam;
    private final I18n i18n;
    private final TimeMatcher tm;

    @Inject
    public TimeCommands(PermissionManager pm, I18n i18n, TimeMatcher tm, TaskManager tam)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
        this.tm = tm;
        this.tam = tam;
    }

    // TODO completer
    @Command(desc = "Changes the time of a world")
    public void time(CommandCause context, @Option String time,
                     @Named("in") String world, // TODO worldlist reader // TODO NParams static label reader
                     @Flag boolean lock)
    {
        Collection<ServerWorld> worldList;
        if (world != null)
        {
            if ("*".equals(world))
            {
                worldList = Sponge.server().worldManager().worlds();
            }
            else
            {
                final Optional<ServerWorld> foundWorld = Sponge.server().worldManager().world(ResourceKey.resolve(world));
                if (!foundWorld.isPresent())
                {
                    i18n.send(context, NEGATIVE, "Could not find the world! {input#world}", world);
                    return;
                }
                worldList = Collections.singletonList(foundWorld.get());
            }
        }
        else
        {
            if (!(context.subject() instanceof ServerPlayer))
            {
                i18n.send(context, NEGATIVE, "You have to specify a world when using this command from the console!");
                return;
            }
            worldList = Collections.singletonList(((ServerPlayer)context.subject()).world());
        }
        if (time != null)
        {
            setTime(context, time, world, lock, worldList);
            return;
        }
        if (lock)
        {
            for (ServerWorld w : worldList)
            {
                toggleTimeLock(context, w, w.properties().dayTime().asTicks().ticks());
            }
            return;
        }
        i18n.send(context, POSITIVE, "The current time is:");
        for (ServerWorld w : worldList)
        {
            long worldTime = w.properties().dayTime().asTicks().ticks();
            i18n.send(context, NEUTRAL, "{input#time} ({input#neartime}) in {world}.", tm.format(worldTime), tm.matchTimeName(worldTime), w);
        }
    }

    private void setTime(CommandCause context, String time, String world, boolean lock, Collection<ServerWorld> worldList)
    {
        Long lTime = tm.matchTimeValue(time);
        if (lTime == null)
        {
            lTime = tm.parseTime(time);
            if (lTime == null)
            {
                i18n.send(context, NEGATIVE, "The time you entered is not valid!");
                return;
            }

        }
        String timeNumeric = tm.format(lTime);
        String timeName = tm.matchTimeName(lTime);
        if (worldList.size() == 1)
        {
            i18n.send(context, POSITIVE,
                      "The time of {world} have been set to {input#time} ({input#neartime})!",
                      worldList.iterator().next(), timeNumeric, timeName);
        }
        else if ("*".equals(world))
        {
            i18n.send(context, POSITIVE,
                      "The time of all worlds have been set to {input#time} ({input#neartime})!",
                      timeNumeric, timeName);
        }
        else
        {
            i18n.send(context, POSITIVE,
                      "The time of {amount} worlds have been set to {input#time} ({input#neartime})!",
                      worldList.size(), timeNumeric, timeName);
        }
        for (ServerWorld w : worldList)
        {
            this.setTime(w, lTime);
            if (lock)
            {
                toggleTimeLock(context, w, w.properties().dayTime().asTicks().ticks());
            }
        }
    }

    private void toggleTimeLock(CommandCause context, ServerWorld world, long worldTime)
    {
        final Boolean oldRule = world.properties().gameRule(GameRules.DO_DAYLIGHT_CYCLE.get());
        world.properties().setGameRule(GameRules.DO_DAYLIGHT_CYCLE.get(), !oldRule);
        if (oldRule)
        {
            i18n.send(context, POSITIVE, "Time locked for {world}!", world);
        }
        else
        {
            i18n.send(context, POSITIVE, "Time unlocked for {world}!", world);
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

    private void setTime(ServerWorld world, long time)
    {
        world.properties().setDayTime(MinecraftDayTime.of(Sponge.server(), Ticks.of(time)));
    }
}
