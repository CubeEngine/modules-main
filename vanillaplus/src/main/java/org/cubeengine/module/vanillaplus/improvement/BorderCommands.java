package org.cubeengine.module.vanillaplus.improvement;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;
import org.spongepowered.api.world.WorldBorder.ChunkPreGenerate;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

// TODO border cmds
// TODO prevent tp out of border
// TODO prevent portal formation out of border
// TODO torus-world

@Command(name = "border", desc = "border commands")
public class BorderCommands extends ContainerCommand
{
    private I18n i18n;
    private PluginContainer plugin;

    public BorderCommands(I18n i18n, CommandManager cm, PluginContainer plugin)
    {
        super(cm, VanillaPlus.class);
        this.i18n = i18n;
        this.plugin = plugin;
    }

    private Task task;

    @Alias(value = "generateBorder")
    @Command(desc = "Generates the chunks located in the border")
    public void generate(CommandSource context, @Default World world, @Flag boolean fulltick)
    {
        if (task != null && !task.cancel())
        {
            i18n.sendTranslated(context, NEGATIVE, "Chunk generation is already running! Canceled.");
            return;
        }

        ChunkPreGenerate generate = world.getWorldBorder().newChunkPreGenerate(world);
        generate.owner(plugin.getInstance().get());
        if (fulltick)
        {
            generate.tickPercentLimit(1);
        }
        generate.logger(plugin.getLogger());
        i18n.sendTranslated(context, NEGATIVE, "Started Chunk generation for {world}. This may take a while.", world);
        this.task = generate.start();
    }

    @Command(desc = "Sets the center for the worldborder", alias = "center")
    public void setCenter(CommandSource context, @Optional Integer x, @Optional Integer z, @Default @Named("in") World world)
    {
        if (x == null || z == null)
        {
            if (context instanceof Player && x == null && z == null)
            {
                Vector3d position = ((Player)context).getLocation().getPosition();
                x = position.getFloorX();
                z = position.getFloorZ();
            }
            else
            {
                throw new TooFewArgumentsException();
            }
        }
        world.getWorldBorder().setCenter(x + 0.5, z + 0.5);
        i18n.sendTranslated(context, POSITIVE, "Set world border of {world} center to {}:{}", world, x, z);
    }

    @Command(desc = "Sets the diameter of the worldborder", alias = "set")
    public void setDiameter(CommandSource context, Integer size, @Optional Integer seconds, @Default @Named("in") World world)
    {
        if (seconds == null)
        {
            world.getWorldBorder().setDiameter(size);
            i18n.sendTranslated(context, POSITIVE, "Set world border of {world} to {} blocks wide", world, size);
        }
        else
        {
            int prevDiameter = (int) world.getWorldBorder().getDiameter();
            world.getWorldBorder().setDiameter(size, seconds * 1000);
            if (size < prevDiameter)
            {
                i18n.sendTranslated(context, POSITIVE, "Shrinking world border of {world} to {} blocks wide from {} over {} seconds",
                                    world, size, prevDiameter, seconds);
            }
            else
            {
                i18n.sendTranslated(context, POSITIVE, "Growing world border of {world} to {} blocks wide from {} over {} seconds",
                                    world, size, prevDiameter, seconds);
            }
        }
    }

    @Command(desc = "Sets the diameter of the worldborder")
    public void add(CommandSource context, Integer size, @Optional Integer time, @Default @Named("in") World world)
    {
        this.setDiameter(context, size + (int)world.getWorldBorder().getDiameter(), time, world);
    }

    @Command(desc = "Sets the warning time")
    public void warningTime(CommandSource context, Integer seconds, @Default @Named("in") World world)
    {
        world.getWorldBorder().setWarningTime(seconds);
        i18n.sendTranslated(context, POSITIVE, "Set world border of {world} warning to {} seconds away", world, seconds);
    }

    @Command(desc = "Sets the warning time")
    public void warningDistance(CommandSource context, Integer blocks, @Default @Named("in") World world)
    {
        world.getWorldBorder().setWarningDistance(blocks);
        i18n.sendTranslated(context, POSITIVE, "Set world border of {world} warning to {} blocks away", world, blocks);
    }

    @Command(desc = "Shows information about the world border", alias = "get")
    public void info(CommandSource context, @Default @Named("in") World world)
    {
        WorldBorder border = world.getWorldBorder();
        double diameter = border.getDiameter();
        i18n.sendTranslated(context, POSITIVE, "The world border in {world} is currently {} blocks wide", world,
                            diameter);
        long secondsRemaining = border.getTimeRemaining() / 1000;
        if (secondsRemaining != 0)
        {
            double newDiameter = border.getNewDiameter();
            if (newDiameter < diameter)
            {
                i18n.sendTranslated(context, POSITIVE, "Currently shrinking to {} blocks wide over {} seconds",
                                    newDiameter, secondsRemaining);
            }
            else
            {
                i18n.sendTranslated(context, POSITIVE, "Currently growing to {} blocks wide over {} seconds",
                                    newDiameter, secondsRemaining);
            }
        }
        i18n.sendTranslated(context, POSITIVE, "Warnings will show within {} seconds or {} blocks from the border", border.getWarningTime(), border.getWarningDistance());
        i18n.sendTranslated(context, POSITIVE, "When more than {} blocks outside the border players will take {} damage per block per second", border.getDamageThreshold(), border.getDamageAmount());
    }

    @Command(desc = "Sets the world border damage per second per block")
    public void damage(CommandSource context, Double damage, @Default @Named("in") World world)
    {
        world.getWorldBorder().setDamageAmount(damage);
        i18n.sendTranslated(context, POSITIVE, "Set world border of {world} damage to {} per block per second", world, damage);
    }

    @Command(desc = "Sets the world border damage buffer")
    public void damageBuffer(CommandSource context, Integer blocks, @Default @Named("in") World world)
    {
        world.getWorldBorder().setDamageThreshold(blocks);
        i18n.sendTranslated(context, POSITIVE, "Set world border of {world} damage buffer to {} block", world, blocks);
    }
}