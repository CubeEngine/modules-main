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
package org.cubeengine.module.worlds;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_AQUA;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.RED;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.WorldArchetypes;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

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
 * TODO alias for worlds
 */
@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends ContainerCommand
{
    private I18n i18n;

    @Inject
    public WorldsCommands(CommandManager cm, I18n i18n)
    {
        super(cm, Worlds.class);
        this.i18n = i18n;
    }

    @Command(desc = "Creates a new world")
    public void create(CommandSource context,
                       String name,
                       @Default @Named({"dimension", "dim"}) DimensionType dimension,
                       @Named("seed") String seed,
                       @Default @Named({"type"}) GeneratorType type,
                       @Default @Label("generate") @Named({"structure", "struct"}) boolean generateStructures,
                       @Default @Named({"gamemode", "mode"}) GameMode gamemode,
                       @Default @Named({"difficulty", "diff"}) Difficulty difficulty,
                       @org.cubeengine.butler.parametric.Optional @Label("name") @Named({"generator","gen"}) WorldGeneratorModifier generator,
                       @Flag boolean recreate,
                       @Flag boolean noload,
                       @Flag boolean spawnInMemory)
    {
        Optional<World> world = Sponge.getServer().getWorld(name);
        if (world.isPresent())
        {
            if (recreate)
            {
                i18n.send(context, NEGATIVE, "You have to unload a world before recreating it!");
                return;
            }
            i18n.send(context, NEGATIVE, "A world named {world} already exists and is loaded!", world.get());
            return;
        }
        Optional<WorldProperties> worldProperties = Sponge.getServer().getWorldProperties(name);
        if (worldProperties.isPresent())
        {
            if (!recreate)
            {
                i18n.send(context, NEGATIVE, "A world named {name#world} already exists but is not loaded!", name);
                return;
            }
            worldProperties.get().setEnabled(false);
            String newName = name + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Sponge.getServer().renameWorld(worldProperties.get(), newName);
            i18n.send(context, POSITIVE, "Old world moved to {name#folder}", newName);
        }

        WorldArchetype.Builder builder = WorldArchetype.builder().from(WorldArchetypes.OVERWORLD);
        builder.keepsSpawnLoaded(spawnInMemory);
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
        if (generator != null)
        {
            builder.generatorModifiers(generator);
        }
        builder.difficulty(difficulty);
        try
        {
            WorldProperties properties = Sponge.getServer().createWorldProperties(name, builder.build("org.cubeengine.customworld:" + UUID.randomUUID().toString(), name));
            i18n.send(context, POSITIVE, "World {name} successfully created!", name);
            i18n.send(context, NEUTRAL, "This world is not yet loaded! Click {txt#here} to load.",
                    i18n.translate(context, TextFormat.NONE, "here").toBuilder().onClick(TextActions.runCommand("/worlds load " + name)).build());
        }
        catch (IOException e)
        {
            i18n.send(context, NEGATIVE, "Could not create world!");
            throw new IllegalStateException(e); // TODO handle exception better
        }
    }

    @Command(desc = "Renames a world")
    public void rename(CommandSource context, WorldProperties world, String newName)
    {
        Optional<World> theWorld = Sponge.getServer().getWorld(world.getUniqueId());
        if (theWorld.isPresent())
        {
            i18n.send(context, POSITIVE, "The world must be unloaded to rename.");
            return;
        }
        String oldName = world.getWorldName();
        Sponge.getServer().renameWorld(world, newName);
        i18n.send(context, POSITIVE, "The world {name} was renamed to {name}", oldName, newName);
    }

    @Command(desc = "Loads a world")
    public void load(CommandSource context, WorldProperties world, @Flag boolean enable)
    {
        Optional<World> w = Sponge.getServer().getWorld(world.getUniqueId());
        if (w.isPresent())
        {
            i18n.send(context, POSITIVE, "The world {world} is already loaded!", w.get());
            return;
        }

        if (enable)
        {
            world.setEnabled(true);
        }
        i18n.send(context, NEGATIVE, "Loading...", world);
        // TODO async me?
        Optional<World> loaded = Sponge.getServer().loadWorld(world);
        if (!loaded.isPresent())
        {
            i18n.send(context, NEGATIVE, "Could not load {name#world}", world);
            return;
        }
        i18n.send(context, POSITIVE, "World {world} loaded!", loaded.get());
    }

    @Command(desc = "Unload a loaded world")
    public void unload(CommandSource context, World world, @Flag boolean force)
    {
        if (Sponge.getServer().unloadWorld(world))
        {
            i18n.send(context, POSITIVE, "Unloaded the world {world}!", world);
            return;
        }

        if (!force)
        {
            Collection<Entity> entities = world.getEntities(input -> input instanceof Player);
            if (!entities.isEmpty())
            {
                int amount = entities.size();
                i18n.sendN(context, NEUTRAL, amount, "There is still one player on that map!",
                        "There are still {amount} players on that map!", amount);
                return;
            }
            i18n.send(context, NEGATIVE, "Could not unload {world}", world);
            return;
        }

        Optional<WorldProperties> defWorld = Sponge.getServer().getDefaultWorld();
        if (!defWorld.isPresent())
        {
            i18n.send(context, NEUTRAL, "Could not unload {world}. No default world to evacuate to.", world);
            return;
        }

        World evacuation = Sponge.getServer().getWorld(defWorld.get().getWorldName()).get();
        if (evacuation == world)
        {
            world.getEntities(entity -> entity instanceof Player).stream()
                    .map(Player.class::cast)
                    .forEach(p -> p.kick(i18n.translate(p, NEGATIVE, "Main world unloading. Flee!")));
        }
        else
        {
            world.getEntities(entity -> entity instanceof Player).stream()
                    .map(Player.class::cast)
                    .forEach(p -> p.setLocationSafely(evacuation.getSpawnLocation()));
        }

        i18n.send(context, POSITIVE, "Teleported all players out of {world}", world);
        if (Sponge.getServer().unloadWorld(world))
        {
            i18n.send(context, POSITIVE, "Unloaded the world {world}!", world);
            return;
        }
        i18n.send(context, NEGATIVE, "Could not unload {world}", world);
    }

    @Command(desc = "Remove a world", alias = "delete")
    public void remove(CommandSource context, WorldProperties world, @Flag @ParameterPermission(value = "remove-worldfolder", desc = "Allows deleting the world folder") boolean folder, @Flag boolean unload)
    {
        Optional<World> loadedWorld = Sponge.getServer().getWorld(world.getUniqueId());
        if (loadedWorld.isPresent())
        {
            if (!unload)
            {
                i18n.send(context, NEGATIVE, "You have to unload the world first!");
                return;
            }
            if (!Sponge.getServer().unloadWorld(loadedWorld.get()))
            {
                i18n.send(context, NEGATIVE, "Could not unload {world}", world);
                return;
            }
        }

        if (folder)
        {
            i18n.send(context, POSITIVE, "Deleting the world {world} from disk...", world);
            Sponge.getServer().deleteWorld(world).
                thenAccept(b -> i18n.send(context, POSITIVE, "Finished deleting the world {world} from disk", world));
            return;
        }
        world.setEnabled(false);
        i18n.send(context, POSITIVE, "The world {world} is now disabled and will not load by itself.", world);
    }

    @Alias("listworlds")
    @Command(desc = "Lists all worlds")
    public void list(CommandSource context)
    {
        i18n.send(context, POSITIVE, "The following worlds do exist:");
        String tNotLoaded = i18n.getTranslation(context.getLocale(), "not loaded");
        String tNotEnabled = i18n.getTranslation(context.getLocale(), "not enabled");
        Sponge.getServer().getAllWorldProperties().stream().sorted((o1, o2) -> o1.getWorldName().compareTo(o2.getWorldName())).forEach(prop ->
        {
            Text.Builder builder = Text.of(" - ", GOLD, prop.getWorldName(), " ", DARK_AQUA, prop.getDimensionType().getName()).toBuilder();

            Text infoText = Text.of(YELLOW, "(?)").toBuilder().onClick(TextActions.runCommand("/worlds info " + prop.getWorldName()))
                    .onHover(TextActions.showText(i18n.translate(context, TextFormat.NONE, "Click to show world info")))
                    .build();

            if (!Sponge.getServer().getWorld(prop.getWorldName()).isPresent())
            {
                builder.append(Text.of(" "));
                if (prop.isEnabled())
                {
                    builder.append(Text.of(RED, tNotLoaded)).append(Text.of(" "))
                            .append(Text.of(YELLOW, "(", i18n.translate(context, TextFormat.NONE, "load"), ")").toBuilder()
                                    .onClick(TextActions.runCommand("/worlds load " + prop.getWorldName())).build());
                }
                else
                {
                    builder.append(Text.of(RED, tNotEnabled));
                }
            }
            builder.append(Text.of(" ")).append(infoText);
            context.sendMessage(builder.build());
        });
    }

    @Command(desc = "Show info about a world")
    public void info(CommandSource context, @Default WorldProperties world, @Flag boolean showGameRules)
    {
        context.sendMessage(Text.EMPTY);
        i18n.send(context, POSITIVE, "World information for {world}:", world);

        if (!world.isEnabled())
        {
            i18n.send(context, NEUTRAL, "This world is disabled.");
        }
        else if (!Sponge.getServer().getWorld(world.getUniqueId()).isPresent())
        {
            Text load = Text.of("(", i18n.translate(context, TextFormat.NONE, "load"), ")").toBuilder()
                    .onClick(TextActions.runCommand("/worlds load " + world.getWorldName())).build();
            i18n.send(context, NEGATIVE, "This world is not loaded. {txt#load}", load);
        }
        if (!world.isInitialized())
        {
            i18n.send(context, NEUTRAL, "This world has not been initialized.");
        }
        i18n.send(context, NEUTRAL, "Gamemode: {input}", world.getGameMode().getTranslation());
        i18n.send(context, NEUTRAL, "DimensionType: {input}", world.getDimensionType().getName());
        if (world.usesMapFeatures())
        {
            i18n.send(context, NEUTRAL, "WorldType: {input} with structures", world.getGeneratorType().getName());
        }
        else
        {
            i18n.send(context, NEUTRAL, "WorldType: {input} no structures", world.getGeneratorType().getName());
        }

        i18n.send(context, NEUTRAL, "Difficulty {input}", world.getDifficulty().getTranslation());
        if (world.isHardcore())
        {
            i18n.send(context, NEUTRAL, "Hardcoremode active");
        }
        if (!world.isPVPEnabled())
        {
            i18n.send(context, NEUTRAL, "PVP disabled");
        }
        if (!world.areCommandsAllowed() && Sponge.getPlatform().getType() == Type.CLIENT)
        {
            i18n.send(context, NEUTRAL, "Commands are not allowed");
        }
        i18n.send(context, NEUTRAL, "Seed: {long}", world.getSeed());
        if (!world.getGeneratorModifiers().isEmpty())
        {
            i18n.send(context, NEUTRAL, "Generation is modified by:");
            for (WorldGeneratorModifier modifier : world.getGeneratorModifiers())
            {
                context.sendMessage(Text.of(YELLOW, " - ", GOLD, modifier.getName()).toBuilder().onHover(TextActions.showText(Text.of(GOLD, modifier.getId()))).build());
            }
        }
        if (!world.loadOnStartup())
        {
            i18n.send(context, NEUTRAL, "This world will not load automatically on startup!");
        }
        Vector3i spawn = world.getSpawnPosition();
        i18n.send(context, NEUTRAL, "This worlds spawn is at {vector}", new Vector3i(spawn.getX(), spawn.getY(), spawn.getZ()));
        if (showGameRules && !world.getGameRules().isEmpty()) // Show gamerules
        {
            i18n.send(context, NEUTRAL, "The following game-rules are set:");
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
            i18n.send(context, NEUTRAL, "There are no players in {world}", world);
            return;
        }
        i18n.send(context, POSITIVE, "The following players are in {world}", world);
        for (Entity player : players)
        {
            context.sendMessage(Text.of(YELLOW, " - ", DARK_GREEN, ((Player) player).getName()));
        }
    }
}
