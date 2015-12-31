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
package org.cubeengine.module.worlds;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.storage.WorldProperties;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends ContainerCommand
{
    private Worlds module;
    private I18n i18n;
    private Game game;
    private Server server;

    public WorldsCommands(Worlds module, I18n i18n, Game game)
    {
        super(module);
        this.module = module;
        this.i18n = i18n;
        this.game = game;
        this.server = game.getServer();
    }

    // TODO keep spawn in memory?
    // TODO autosave?
    // TODO custom Generators

    @Command(desc = "Creates a new world")
    public void create(CommandSource context,
                       String name,
                       @Default @Named({"dimension", "dim"}) DimensionType dimension,
                       @Named("seed") String seed,
                       @Default @Named({"generatortype", "generator", "type"}) GeneratorType type,
                       @Named({"structure", "struct"}) Boolean generateStructures,
                       @Default @Named({"gamemode", "mode"}) GameMode gamemode,
                       @Default @Named({"difficulty", "diff"}) Difficulty difficulty,
                       @Flag boolean recreate,
                       @Flag boolean noload)
    {
        Optional<World> world = server.getWorld(name);
        if (world.isPresent())
        {
            if (recreate)
            {
                i18n.sendTranslated(context, NEGATIVE, "You have to unload a world before recreating it!");
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "A world named {world} already exists and is loaded!", world.get());
            return;
        }
        Optional<WorldProperties> worldProperties = server.getWorldProperties(name);
        if (worldProperties.isPresent())
        {
            if (!recreate)
            {
                i18n.sendTranslated(context, NEGATIVE, "A world named {name#world} already exists but is not loaded!", name);
                return;
            }
            worldProperties.get().setEnabled(false);
            String newName = name + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            server.renameWorld(worldProperties.get(), newName);
            i18n.sendTranslated(context, POSITIVE, "Old world moved to {name#folder}", newName);
        }

        WorldCreationSettings.Builder builder = WorldCreationSettings.builder();
        // TODO check nulls

        builder.name(name);
        builder.loadsOnStartup(!noload);
        if (seed != null)
        {
            try
            {
                builder.seed(Long.parseLong(seed));
            }
            catch (NumberFormatException ignore)
            {
                builder.seed(seed.hashCode());
            }
        }

        builder.generator(type);
        builder.dimension(dimension);
        builder.usesMapFeatures(generateStructures != null && generateStructures);
        builder.gameMode(gamemode);
        Optional<WorldProperties> properties = game.getServer().createWorldProperties(builder.build());
        if (properties.isPresent())
        {
            properties.get().setDifficulty(difficulty);
            i18n.sendTranslated(context, POSITIVE, "World successfully created!");
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Could not create world!");
    }

    @Command(desc = "Loads a world")
    public void load(CommandSource context, String world)
    {
        Optional<World> w = this.game.getServer().getWorld(world);
        if (w.isPresent())
        {
            i18n.sendTranslated(context, POSITIVE, "The world {world} is already loaded!", w);
            return;
        }

        if (!server.getWorldProperties(world).isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "World {input} not found!", world);
            return;
        }

        Optional<World> loaded = server.loadWorld(world);
        if (!loaded.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "Could not load {name#world}", world);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "World {world} loaded!", loaded.get());
    }

    @Command(desc = "Unload a loaded world")
    public void unload(CommandSource context, World world, @Flag boolean force)
    {
        if (server.unloadWorld(world))
        {
            i18n.sendTranslated(context, POSITIVE, "Unloaded the world {world}!", world);
            return;
        }

        if (!force)
        {
            Collection<Entity> entities = world.getEntities(input -> input instanceof Player);
            if (!entities.isEmpty())
            {
                int amount = entities.size();
                i18n.sendTranslatedN(context, NEUTRAL, amount, "There is still one player on that map!",
                        "There are still {amount} players on that map!", amount);
                return;
            }
            i18n.sendTranslated(context, NEGATIVE, "Could not unload {world}", world);
            return;
        }

        Optional<WorldProperties> defWorld = server.getDefaultWorld();
        if (!defWorld.isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "Could not unload {world}. No default world to evacuate to.", world);
            return;
        }

        World evacuation = server.getWorld(defWorld.get().getWorldName()).get();
        if (evacuation == world)
        {
            world.getEntities(entity -> entity instanceof Player).stream().map(Player.class::cast).forEach(p -> {
                // TODO translation object before?
                Text reason = i18n.getTranslation(p, NEGATIVE, "Main world unloading. Flee!");

                p.kick(reason);
            });
        }
        else
        {
            world.getEntities(entity -> entity instanceof Player).stream().map(Player.class::cast)
                    .forEach(p -> p.setLocationSafely(evacuation.getSpawnLocation()));
        }

        i18n.sendTranslated(context, POSITIVE, "Teleported all players out of {world}", world);
        if (server.unloadWorld(world))
        {
            i18n.sendTranslated(context, POSITIVE, "Unloaded the world {world}!", world);
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Could not unload {world}", world);
    }

    @Command(desc = "Remove a world")
    public void remove(CommandSource context, String world, @Flag @ParameterPermission(value = "remove-worldfolder", desc = "Allows deleting the world folder") boolean folder)
    {
        Optional<World> w = server.getWorld(world);
        if (w.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "You have to unload the world first!");
            return;
        }

        Optional<WorldProperties> worldProperties = server.getWorldProperties(world);
        if (!worldProperties.isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "World {input} not found!", world);
            return;
        }

        if (folder)
        {
            i18n.sendTranslated(context, POSITIVE, "Deleting the world {name} from disk...", world);
            server.deleteWorld(worldProperties.get()).addListener(
                    () -> i18n.sendTranslated(context, POSITIVE, "Finished deleting the world {name} from disk", world),
                    Executors.newSingleThreadExecutor() // TODO choose an executor?
            );
        }
        else
        {
            worldProperties.get().setEnabled(false);
            i18n.sendTranslated(context, POSITIVE, "The world {world} is now disabled and will not load by itself.", world);
        }
        // TODO remove configs for the world
        // TODO remove configs
    }

    @Command(desc = "Lists all worlds")
    public void list(CommandSource context)
    {
        // TODO check world permissions for display?
        i18n.sendTranslated(context, POSITIVE, "The following worlds do exist:");

        for (WorldProperties properties : server.getAllWorldProperties())
        {
            Optional<World> world = server.getWorld(properties.getWorldName());
            if (world.isPresent())
            {
                i18n.sendTranslated(context, POSITIVE,
                        "{name#world} {input#environement:color=INDIGO}",
                        properties.getWorldName(), properties.getDimensionType().getName());
            }
            else if (properties.isEnabled())
            {
                i18n.sendTranslated(context, POSITIVE,
                        "{name#world} {input#environement:color=INDIGO} {text:(not loaded):color=RED}",
                        properties.getWorldName(), properties.getDimensionType().getName());
            }
            else
            {
                i18n.sendTranslated(context, POSITIVE,
                        "{name#world} {input#environement:color=INDIGO} {text:(not enabled):color=DARK_RED}",
                        properties.getWorldName(), properties.getDimensionType().getName());
            }
        }
    }
    // list / list worlds that you can enter

    @Command(desc = "Show info about a world")
    public void info(CommandSource context, String world)
    {
        WorldProperties properties = server.getWorldProperties(world).orElse(null);
        if (properties == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "World {input} not found!", world);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "World information for {input#world}:", world);
        i18n.sendTranslated(context, POSITIVE, "Gamemode: {input}", properties.getGameMode().getTranslation());
        i18n.sendTranslated(context, POSITIVE, "DimensionType: {input}", properties.getDimensionType().getName());
        if (properties.usesMapFeatures())
        {
            i18n.sendTranslated(context, POSITIVE, "WorldType: {input} with structures", properties.getGeneratorType().getName());
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "WorldType: {input}", properties.getGeneratorType().getName());
        }
        // TODO custom generator
        if (!properties.loadOnStartup())
        {
            i18n.sendTranslated(context, POSITIVE, "This world will not load automatically on startup!");
        }
        Vector3i spawn = properties.getSpawnPosition();
        i18n.sendTranslated(context, POSITIVE, "This worlds spawn is at {vector}", new BlockVector3(spawn.getX(), spawn.getY(), spawn.getZ()));
        // gamerules
        // TODO finish worlds info cmd
    }
    // info

    @Command(desc = "Lists the players in a world")
    public void listplayers(CommandSource context, World world)
    {
        Collection<Entity> players = world.getEntities(e -> e instanceof Player);
        if (players.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are no players in {world}", world);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following players are in {world}", world);
        Text s = Text.of(TextColors.YELLOW, " -", TextColors.GOLD);
        for (Entity player : players)
        {
            context.sendMessage(Text.of(s, ((Player) player).getName()));
        }
    }
}
