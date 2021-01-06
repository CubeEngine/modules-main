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

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

// TODO border cmds
// TODO prevent tp out of border
// TODO prevent portal formation out of border
// TODO torus-world
@Singleton
@Command(name = "border", desc = "border commands")
public class BorderCommands extends DispatcherCommand
{
    private I18n i18n;
    private PluginContainer plugin;
    private VanillaPlus module;
    private long taskStart;

    @Inject
    public BorderCommands(I18n i18n, PluginContainer plugin, VanillaPlus module)
    {
        this.i18n = i18n;
        this.plugin = plugin;
        this.module = module;
    }

    // TODO generator task is gone in API8 atm. replace with issueing tickets to the chunkmanager
//    private ChunkPreGenerate task;
//
//    @Alias(value = "generateBorder")
//    @Command(desc = "Generates the chunks located in the border")
//    public void generate(CommandCause context, @Default ServerWorld world, @Flag boolean fulltick, @Flag boolean status)
//    {
//        if (status && task != null && !task.isCancelled())
//        {
//            long timePassed = System.currentTimeMillis() - taskStart;
//            long estimateTime = timePassed / (task.getTotalSkippedChunks() + task.getTotalGeneratedChunks()) * task.getTargetTotalChunks();
//
//            i18n.send(context, NEUTRAL, "Border generation for {name}:", task.getWorldProperties().getWorldName());
//            i18n.send(context, NEUTRAL, "Estimated remaining time: {}", TimeUtil.format(context.getLocale(), estimateTime - timePassed));
//            i18n.send(context, NEUTRAL, "Chunks generated {}", task.getTotalGeneratedChunks());
//            i18n.send(context, NEUTRAL, "Chunks skipped {}", task.getTotalSkippedChunks());
//            i18n.send(context, NEUTRAL, "Target count {} ({decimal:2}%)", task.getTargetTotalChunks(), 100 * (task.getTotalSkippedChunks() + task.getTotalGeneratedChunks()) / task.getTargetTotalChunks());
//            return;
//        }
//        if (status)
//        {
//            i18n.send(context, NEUTRAL, "No generation is running.");
//            return;
//        }
//
//        if (task != null && !task.isCancelled())
//        {
//            task.cancel();
//            task = null;
//            i18n.send(context, NEGATIVE, "Chunk generation is already running! Canceled.");
//            return;
//        }
//
//        if (world.getWorldBorder().getDiameter() > this.commandBorderMax)
//        {
//            i18n.send(context, NEUTRAL,
//                    "Generation will not run for WorldBorder diameter bigger than {number} blocks.");
//            i18n.send(context, POSITIVE, "You can change this value in the configuration");
//            return;
//        }
//        ChunkPreGenerate.Builder generate = world.getBorder().newChunkPreGenerate(world);
//        generate.owner(plugin.getInstance().get());
//        if (fulltick)
//        {
//            generate.tickPercentLimit(1);
//            generate.tickInterval(2);
//        }
//        generate.logger(plugin.getLogger());
//        i18n.send(context, NEGATIVE, "Started Chunk generation for {world}. This may take a while.", world);
//        this.task = generate.start();
//        this.taskStart = System.currentTimeMillis();
//    }

    @Command(desc = "Sets the center for the worldborder", alias = "center")
    public void setCenter(CommandCause context, @Option Vector2i pos, @Default @Named("in") ServerWorld world)
    {
        if (pos == null)
        {
            if (context.getSubject() instanceof ServerPlayer)
            {
                Vector3d position = ((ServerPlayer)context.getSubject()).getLocation().getPosition();
                pos = new Vector2i(position.getX(), position.getZ());
            }
            else
            {
                i18n.send(context, CRITICAL,"Too few arguments!");
                return;
            }
        }
        world.getBorder().setCenter(pos.getX() + 0.5, pos.getY() + 0.5);
        i18n.send(context, POSITIVE, "Set world border of {world} center to {vector}", world, pos);
    }

    @Command(desc = "Sets the diameter of the worldborder", alias = "set")
    public void setDiameter(CommandCause context, int size, @Option Integer seconds, @Default @Named("in") ServerWorld world)
    {
        if (seconds == null)
        {
            world.getBorder().setDiameter(size);
            i18n.send(context, POSITIVE, "Set world border of {world} to {} blocks wide", world, size);
            return;
        }
        int prevDiameter = (int) world.getBorder().getDiameter();
        world.getBorder().setDiameter(size, seconds, ChronoUnit.SECONDS);
        if (size < prevDiameter)
        {
            i18n.send(context, POSITIVE, "Shrinking world border of {world} to {} blocks wide from {} over {} seconds",
                                world, size, prevDiameter, seconds);
        }
        else
        {
            i18n.send(context, POSITIVE, "Growing world border of {world} to {} blocks wide from {} over {} seconds",
                                world, size, prevDiameter, seconds);
        }
    }

    @Command(desc = "Sets the diameter of the worldborder")
    public void add(CommandCause context, Integer size, @Option Integer time, @Default @Named("in") ServerWorld world)
    {
        this.setDiameter(context, size + (int)world.getBorder().getDiameter(), time, world);
    }

    @Command(desc = "Sets the warning time")
    public void warningTime(CommandCause context, int seconds, @Default ServerWorld world)
    {
        world.getBorder().setWarningTime(seconds, ChronoUnit.SECONDS);
        i18n.send(context, POSITIVE, "Set world border of {world} warning to {} seconds away", world, seconds);
    }

    @Command(desc = "Sets the warning time")
    public void warningDistance(CommandCause context, int blocks, @Default ServerWorld world)
    {
        world.getBorder().setWarningDistance(blocks);
        i18n.send(context, POSITIVE, "Set world border of {world} warning to {} blocks away", world, blocks);
    }

    @Command(desc = "Shows information about the world border", alias = "get")
    public void info(CommandCause context, @Default ServerWorld world)
    {
        WorldBorder border = world.getBorder();
        double diameter = border.getDiameter();
        i18n.send(context, POSITIVE, "The world border in {world} is currently {} blocks wide", world,
                            diameter);
        long secondsRemaining = border.getTimeRemaining().get(ChronoUnit.SECONDS);
        if (secondsRemaining != 0)
        {
            double newDiameter = border.getNewDiameter();
            if (newDiameter < diameter)
            {
                i18n.send(context, POSITIVE, "Currently shrinking to {} blocks wide over {} seconds", newDiameter, secondsRemaining);
            }
            else
            {
                i18n.send(context, POSITIVE, "Currently growing to {} blocks wide over {} seconds", newDiameter, secondsRemaining);
            }
        }
        i18n.send(context, POSITIVE, "Warnings will show within {} seconds or {} blocks from the border", border.getWarningTime().get(ChronoUnit.SECONDS), border.getWarningDistance());
        i18n.send(context, POSITIVE, "When more than {} blocks outside the border players will take {} damage per block per second", border.getDamageThreshold(), border.getDamageAmount());
    }

    @Command(desc = "Sets the world border damage per second per block")
    public void damage(CommandCause context, Double damage, @Default ServerWorld world)
    {
        world.getBorder().setDamageAmount(damage);
        i18n.send(context, POSITIVE, "Set world border of {world} damage to {} per block per second", world, damage);
    }

    @Command(desc = "Sets the world border damage buffer")
    public void damageBuffer(CommandCause context, Integer blocks, @Default ServerWorld world)
    {
        world.getBorder().setDamageThreshold(blocks);
        i18n.send(context, POSITIVE, "Set world border of {world} damage buffer to {} block", world, blocks);
    }
}
