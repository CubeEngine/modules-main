package org.cubeengine.module.multiverse;

import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.worldcontrol.WorldConfig;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.util.Iterator;

import static org.cubeengine.service.filesystem.FileExtensionFilter.PO;
import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static org.cubeengine.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class MultiverseCommands extends ContainerCommand
{
    private UniverseManager um;
    private I18n i18n;
    private Game game;

    public MultiverseCommands(Multiverse module, UniverseManager um, I18n i18n, Game game)
    {
        super(module);
        this.um = um;
        this.i18n = i18n;
        this.game = game;
    }


    @Command(desc = "Teleports to the spawn of the mainworld of a universe")
    @Restricted(value = Player.class)
    public void uSpawn(Player context, String universe)
    {
        UniverseConfig config = um.getUniverse(universe);
        if (config == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Universe {input} not found!", universe);
            return;
        }
        context.setLocationSafely(config.mainWorld.getWorld().getSpawnLocation());
        i18n.sendTranslated(context, POSITIVE, "You are now at the spawn of {world} (main world of the universe {name})", config.mainWorld.getWorld(), universe);
    }

    @Command(desc = "Creates a new universe")
    public void createuniverse(CommandSource context, String name)
    {
        um.createUniverse(name);
        i18n.sendTranslated(context, POSITIVE, "Empty universe {name} created", name);
    }
    // create nether & create end commands / auto link to world / only works for NORMAL Env worlds

    @Command(desc = "Sets the main world")
    public void setMainWorld(CommandSource context, World world)
    {
        UniverseConfig config = um.getUniverseFrom(world);
        config.mainWorld = new ConfigWorld(game, world);
        config.save();

        i18n.sendTranslated(context, POSITIVE, "{world} is now the main world of the universe {name}", world, config.getFile().getName()); // TODO correct name
    }
    // set main world (of universe) (of universes)
    // set main universe

    @Command(desc = "Moves a world into another universe")
    public void move(CommandSource context, World world, String universe)
    {
        // TODO prevent moving when players are on it
        UniverseConfig to = um.getUniverse(universe);
        if (to == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "Universe {input} not found!", universe);
            return;
        }
        UniverseConfig from = um.getUniverseFrom(world);
        if (from == to)
        {
            i18n.sendTranslated(context, NEGATIVE, "{world} is already in the universe {name}", world, u.getName());
            return;
        }
        if (from.mainWorld.getName().equals(world.getName()))
        {
            from.mainWorld = null;
        }

        for (Iterator<ConfigWorld> iterator = from.worlds.iterator(); iterator.hasNext(); )
        {
            if (iterator.next().getName().equals(world.getName()))
            {
                iterator.remove();
                break;
            }
        }

        from.save();
        to.worlds.add(new ConfigWorld(game, world.getName()));
        to.save();

        // TODO update maps in Manager accordingly

        i18n.sendTranslated(context, POSITIVE, "World successfully moved!");
    }
    // move to other universe

    @Command(desc = "Loads a player's state for their current world")
    public void loadPlayer(CommandSource context, Player player)
    {
        Universe universe = multiverse.getUniverseFrom(player.getWorld());
        universe.loadPlayer(player);
        i18n.sendTranslated(context, POSITIVE, "Loaded {user}'s data from file!", player);
    }

    @Command(desc = "Save a player's state for their current world")
    public void savePlayer(CommandSource context, Player player)
    {
        Universe universe = multiverse.getUniverseFrom(player.getWorld());
        universe.savePlayer(player, player.getWorld());
        i18n.sendTranslated(context, POSITIVE, "Saved {user}'s data to file!", player.getDisplayName());
    }
}
