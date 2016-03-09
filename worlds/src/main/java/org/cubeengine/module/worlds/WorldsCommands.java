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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Executors;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.*;
import static org.spongepowered.api.text.format.TextStyles.STRIKETHROUGH;

/**
 * WorldsCommands includes the following sub-commands:
 *
 * - create
 * - load
 * - unload
 * - remove
 * - list
 * - info
 * - listplayers
 *
 * TODO autosave?
 * TODO custom Generators
 */
@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends ContainerCommand
{
    private I18n i18n;

    public WorldsCommands(Worlds module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
    }

    @Command(desc = "Creates a new world")
    public void create(CommandSource context,
                       String name,
                       @Default @Named({"dimension", "dim"}) DimensionType dimension,
                       @Named("seed") String seed,
                       @Default @Named({"generatortype", "generator", "type"}) GeneratorType type,
                       @Default @Named({"structure", "struct"}) boolean generateStructures,
                       @Default @Named({"gamemode", "mode"}) GameMode gamemode,
                       @Default @Named({"difficulty", "diff"}) Difficulty difficulty,
                       @Flag boolean recreate,
                       @Flag boolean noload,
                       @Flag boolean spawnInMemory)
    {
        Optional<World> world = Sponge.getServer().getWorld(name);
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
        Optional<WorldProperties> worldProperties = Sponge.getServer().getWorldProperties(name);
        if (worldProperties.isPresent())
        {
            if (!recreate)
            {
                i18n.sendTranslated(context, NEGATIVE, "A world named {name#world} already exists but is not loaded!", name);
                return;
            }
            worldProperties.get().setEnabled(false);
            String newName = name + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Sponge.getServer().renameWorld(worldProperties.get(), newName);
            i18n.sendTranslated(context, POSITIVE, "Old world moved to {name#folder}", newName);
        }

        WorldCreationSettings.Builder builder = WorldCreationSettings.builder();
        builder.keepsSpawnLoaded(spawnInMemory);
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
        builder.usesMapFeatures(generateStructures);
        builder.gameMode(gamemode);
        Optional<WorldProperties> properties = Sponge.getServer().createWorldProperties(builder.build());
        if (properties.isPresent())
        {
            properties.get().setDifficulty(difficulty);
            i18n.sendTranslated(context, POSITIVE, "World successfully created!");
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Could not create world!");
    }

    @Command(desc = "Loads a world")
    public void load(CommandSource context, WorldProperties world, @Flag boolean enable)
    {
        Optional<World> w = Sponge.getServer().getWorld(world.getUniqueId());
        if (w.isPresent())
        {
            i18n.sendTranslated(context, POSITIVE, "The world {world} is already loaded!", w);
            return;
        }

        if (enable)
        {
            world.setEnabled(true);
        }

        Optional<World> loaded = Sponge.getServer().loadWorld(world);
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
        if (Sponge.getServer().unloadWorld(world))
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

        Optional<WorldProperties> defWorld = Sponge.getServer().getDefaultWorld();
        if (!defWorld.isPresent())
        {
            i18n.sendTranslated(context, NEUTRAL, "Could not unload {world}. No default world to evacuate to.", world);
            return;
        }

        World evacuation = Sponge.getServer().getWorld(defWorld.get().getWorldName()).get();
        if (evacuation == world)
        {
            world.getEntities(entity -> entity instanceof Player).stream()
                    .map(Player.class::cast)
                    .forEach(p -> p.kick(i18n.getTranslation(p, NEGATIVE, "Main world unloading. Flee!")));
        }
        else
        {
            world.getEntities(entity -> entity instanceof Player).stream()
                    .map(Player.class::cast)
                    .forEach(p -> p.setLocationSafely(evacuation.getSpawnLocation()));
        }

        i18n.sendTranslated(context, POSITIVE, "Teleported all players out of {world}", world);
        if (Sponge.getServer().unloadWorld(world))
        {
            i18n.sendTranslated(context, POSITIVE, "Unloaded the world {world}!", world);
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Could not unload {world}", world);
    }

    @Command(desc = "Remove a world", alias = "delete")
    public void remove(CommandSource context, WorldProperties world, @Flag @ParameterPermission(value = "remove-worldfolder", desc = "Allows deleting the world folder") boolean folder)
    {
        if (Sponge.getServer().getWorld(world.getUniqueId()).isPresent())
        {
            i18n.sendTranslated(context, NEGATIVE, "You have to unload the world first!");
            return;
        }

        if (folder)
        {
            i18n.sendTranslated(context, POSITIVE, "Deleting the world {name} from disk...", world);
            Sponge.getServer().deleteWorld(world).
                thenAccept(b -> i18n.sendTranslated(context, POSITIVE, "Finished deleting the world {name} from disk", world));
            return;
        }
        world.setEnabled(false);
        i18n.sendTranslated(context, POSITIVE, "The world {world} is now disabled and will not load by itself.", world);
    }

    @Command(desc = "Lists all worlds")
    public void list(CommandSource context)
    {
        i18n.sendTranslated(context, POSITIVE, "The following worlds do exist:");

        for (WorldProperties properties : Sponge.getServer().getAllWorldProperties())
        {
            if (Sponge.getServer().getWorld(properties.getWorldName()).isPresent())
            {
                i18n.sendTranslated(context, POSITIVE,
                        "{name#world} {input#environement:color=INDIGO}",
                        properties.getWorldName(), properties.getDimensionType().getName());
                return;
            }
            if (properties.isEnabled())
            {
                i18n.sendTranslated(context, POSITIVE,
                        "{name#world} {input#environement:color=INDIGO} {text:(not loaded):color=RED}",
                        properties.getWorldName(), properties.getDimensionType().getName());
                return;
            }
            i18n.sendTranslated(context, POSITIVE.style(STRIKETHROUGH),
                    "{name#world} {input#environement:color=INDIGO} {text:(not enabled):color=DARK_RED}",
                    properties.getWorldName(), properties.getDimensionType().getName());
        }
    }

    @Command(desc = "Show info about a world")
    public void info(CommandSource context, @Default WorldProperties world, @Flag boolean showGameRules)
    {
        context.sendMessage(Text.EMPTY);
        i18n.sendTranslated(context, POSITIVE, "World information for {world}:", world);
        if (!world.isEnabled())
        {
            i18n.sendTranslated(context, NEUTRAL, "This world is disabled.");
        }
        if (!world.isInitialized())
        {
            i18n.sendTranslated(context, NEUTRAL, "This world has not been initialized.");
        }
        i18n.sendTranslated(context, POSITIVE, "Gamemode: {input}", world.getGameMode().getTranslation());
        i18n.sendTranslated(context, POSITIVE, "DimensionType: {input}", world.getDimensionType().getName());
        if (world.usesMapFeatures())
        {
            i18n.sendTranslated(context, POSITIVE, "WorldType: {input} with structures", world.getGeneratorType().getName());
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "WorldType: {input} no structures", world.getGeneratorType().getName());
        }
        i18n.sendTranslated(context, POSITIVE, "Difficulty {input}", world.getDifficulty().getTranslation());
        if (world.isHardcore())
        {
            i18n.sendTranslated(context, POSITIVE, "Hardcoremode active");
        }
        if (!world.isPVPEnabled())
        {
            i18n.sendTranslated(context, POSITIVE, "PVP disabled");
        }
        if (!world.areCommandsAllowed() && Sponge.getPlatform().getType() == Type.CLIENT)
        {
            i18n.sendTranslated(context, POSITIVE, "Commands are not allowed");
        }
        i18n.sendTranslated(context, POSITIVE, "Seed: {long}", world.getSeed());
        if (!world.getGeneratorModifiers().isEmpty())
        {
            i18n.sendTranslated(context, POSITIVE, "Generation is modified by:");
            for (WorldGeneratorModifier modifier : world.getGeneratorModifiers())
            {
                context.sendMessage(Text.of(YELLOW, " - ", GOLD, modifier.getName()));
            }
        }
        if (!world.loadOnStartup())
        {
            i18n.sendTranslated(context, POSITIVE, "This world will not load automatically on startup!");
        }
        Vector3i spawn = world.getSpawnPosition();
        i18n.sendTranslated(context, POSITIVE, "This worlds spawn is at {vector}", new BlockVector3(spawn.getX(), spawn.getY(), spawn.getZ()));
        if (showGameRules && !world.getGameRules().isEmpty()) // Show gamerules
        {
            i18n.sendTranslated(context, POSITIVE, "The following game-rules are set:");
            for (Entry<String, String> entry : world.getGameRules().entrySet())
            {
                context.sendMessage(Text.of(YELLOW, entry.getKey(), ": ", GOLD, entry.getValue()));
            }
        }
    }

    @Command(desc = "Lists the players in a world")
    public void listplayers(CommandSource context, @Default World world)
    {
        Collection<Entity> players = world.getEntities(e -> e instanceof Player);
        if (players.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are no players in {world}", world);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following players are in {world}", world);
        for (Entity player : players)
        {
            context.sendMessage(Text.of(YELLOW, " - ", DARK_GREEN, ((Player) player).getName()));
        }
    }
}
