package org.cubeengine.module.worlds;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.inject.Inject;

@Command(name = "modify", desc = "Worlds modify commands")
public class WorldsModifyCommands extends ContainerCommand
{
    private I18n i18n;

    @Inject
    public WorldsModifyCommands(CommandManager cm, I18n i18n)
    {
        super(cm, Worlds.class);
        this.i18n = i18n;
    }

    @Command(desc = "Sets the autoload behaviour")
    public void autoload(CommandSource context, WorldProperties world, @Optional Boolean set)
    {
        if (set == null)
        {
            set = !world.loadOnStartup();
        }
        world.setLoadOnStartup(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now autoload.", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer autoload.", world);
    }

    @Command(desc = "Sets whether structors generate")
    public void generateStructure(CommandSource context, WorldProperties world, @Optional Boolean set)
    {
        if (set == null)
        {
            set = !world.usesMapFeatures();
        }
        world.setMapFeaturesEnabled(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now generate structures", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer generate structures", world);
    }
}
