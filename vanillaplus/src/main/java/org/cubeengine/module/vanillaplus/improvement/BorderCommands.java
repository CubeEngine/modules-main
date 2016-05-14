package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
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
import org.spongepowered.api.world.WorldBorder.ChunkPreGenerate;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

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
    public void generate(CommandSource context, World world, @Flag boolean fulltick)
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

        this.task = generate.start();
    }
}
