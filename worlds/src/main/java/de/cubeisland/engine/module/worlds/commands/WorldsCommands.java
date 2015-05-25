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
package de.cubeisland.engine.module.worlds.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.butler.parameter.IncorrectUsageException;
import de.cubeisland.engine.module.service.command.ContainerCommand;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.Pair;
import de.cubeisland.engine.module.core.util.WorldLocation;

import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.module.service.world.ConfigWorld;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.module.worlds.Multiverse;
import de.cubeisland.engine.module.worlds.Universe;
import de.cubeisland.engine.module.worlds.Worlds;
import de.cubeisland.engine.module.worlds.config.WorldConfig;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.filesystem.FileExtensionFilter.YAML;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;

@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends ContainerCommand
{
    private Worlds module;
    private final Multiverse multiverse;
    private final WorldManager wm;

    public WorldsCommands(Worlds module, Multiverse multiverse, WorldManager wm)
    {
        super(module);
        this.module = module;
        this.multiverse = multiverse;
        this.wm = wm;
    }

    @Command(desc = "Creates a new universe")
    public void createuniverse(CommandContext context, String name)
    {
        context.sendMessage("TODO");
        // TODO universe create cmd
    }

    @Command(desc = "Creates a new world")
    public void create(CommandSender context, String name, @Optional String universeName,
                       @Named({"environment","env"}) DimensionType environment,
                       @Named("seed") String seed,
                       @Named({"worldtype","type"}) GeneratorType type,
                       @Named({"structure","struct"}) Boolean generateStructures,
                       @Named({"generator", "gen"}) String generator,
                       @Flag boolean recreate,
                       @Flag boolean noload)
    {
        com.google.common.base.Optional<World> world = this.wm.getWorld(name);
        if (world.isPresent())
        {
            if (recreate)
            {
                context.sendTranslated(NEGATIVE, "You have to unload a world before recreating it!");
                return;
            }
            context.sendTranslated(NEGATIVE, "A world named {world} already exists and is loaded!", world.get());
            return;
        }
        Path path = Bukkit.getServer().getWorldContainer().toPath().resolve(name);
        if (Files.exists(path))
        {
            if (!recreate)
            {
                context.sendTranslated(NEGATIVE, "A world named {name#world} already exists but is not loaded!", name);
                return;
            }
            try
            {
                Path newPath = path.resolveSibling(name + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
                Files.move(path, newPath);
                context.sendTranslated(POSITIVE, "Old world moved to {name#folder}", path.getFileName().toString());
            }
            catch (IOException e)
            {
                context.sendTranslated(CRITICAL, "Could not backup old world folder! Aborting Worldcreation");
                return;
            }
        }
        WorldConfig config = this.module.getCore().getConfigFactory().create(WorldConfig.class);
        Path dir;
        Universe universe;
        if (universeName != null)
        {
            universe = multiverse.getUniverse(universeName);
            if (universe == null)
            {
                universe = multiverse.createUniverse(universeName);
            }
            dir = universe.getDirectory();
        }
        else if (context instanceof User)
        {
            universe = multiverse.getUniverseFrom(((User)context).getWorld());
            dir = universe.getDirectory();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You have to provide a universe in which to create the world!");
            // TODO show usage
            return;
        }
        config.setFile(dir.resolve(name + YAML.getExtention()).toFile());
        if (environment != null)
        {
            config.generation.environment = environment;
        }
        if (seed != null)
        {
            config.generation.seed = seed;
        }
        if (type != null)
        {
            config.generation.worldType = type;
        }
        if (generateStructures != null)
        {
            config.generation.generateStructures = generateStructures;
        }
        if (generator != null)
        {
            config.generation.customGenerator = generator;
        }
        config.save();
        if (!noload)
        {
            try
            {
                universe.reload();
            }
            catch (IOException e)
            {
                context.sendTranslated(CRITICAL, "A critical Error occured while creating the world!");
                this.module.getLog().error(e, e.getLocalizedMessage());
            }
        }
    }

    @Command(desc = "Loads a world from configuration")
    public void load(CommandSender context, String world, @Optional String universe)
    {
        World w = this.wm.getWorld(world);
        if (w != null)
        {
            context.sendTranslated(POSITIVE, "The world {world} is already loaded!", w);
            return;
        }
        if (multiverse.hasWorld(world) != null)
        {
            if (universe != null)
            {
                throw new IncorrectUsageException("You've given too many arguments.");
            }
            w = multiverse.loadWorld(world);
            if (w == null)
            {
                context.sendTranslated(NEGATIVE, "Could not load {name#world}", world);
                return;
            }
            context.sendTranslated(POSITIVE, "World {world} loaded!", w);
            return;
        }
        if (!Files.exists(Bukkit.getServer().getWorldContainer().toPath().resolve(world)))
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", world);
            return;
        }
        Universe u;
        if (universe != null)
        {
            u = this.multiverse.getUniverse(universe);
            if (u == null)
            {
                context.sendTranslated(NEGATIVE, "Universe {name} not found!", universe);
                return;
            }
        }
        else if (context instanceof User)
        {
            u = this.multiverse.getUniverseFrom(((User)context).getWorld());
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You need to specify a universe to load the world into!");
            return;
        }
        w = this.wm.createWorld(new WorldCreator(world));
        Set<World> worldToAdd = new HashSet<>();
        worldToAdd.add(w);
        u.addWorlds(worldToAdd);
        context.sendTranslated(POSITIVE, "World {world} loaded!", w);
    }

    @Command(desc = "Unload a loaded world")
    public void unload(CommandSender context, World world, @Flag boolean force)
    {
        World tpWorld = this.multiverse.getUniverseFrom(world).getMainWorld();
        if (tpWorld == world)
        {
            tpWorld = this.multiverse.getMainWorld();
            if (tpWorld == world)
            {
                context.sendTranslated(NEGATIVE, "Cannot unload main world of main universe!");
                context.sendTranslated(NEUTRAL, "/worlds setmainworld <world>");
                return;
            }
        }
        if (force && !world.getPlayers().isEmpty())
        {
            Location spawnLocation = tpWorld.getSpawnLocation();
            spawnLocation.setX(spawnLocation.getX() + 0.5);
            spawnLocation.setZ(spawnLocation.getZ() + 0.5);
            for (Player player : world.getPlayers())
            {
                if (!player.teleport(spawnLocation))
                {
                    context.sendTranslated(NEGATIVE, "Could not teleport every player out of the world to unload!");
                    return;
                }
            }
            context.sendTranslated(POSITIVE, "Teleported all players out of {world}", world);
        }
        if (this.wm.unloadWorld(world))
        {
            context.sendTranslated(POSITIVE, "Unloaded the world {world}!", world);
            return;
        }
        context.sendTranslated(NEGATIVE, "Could not unload {world}", world);
        if (!world.getPlayers().isEmpty())
        {
            int amount = world.getPlayers().size();
            context.sendTranslatedN(NEUTRAL, amount, "There is still one player on that map!",
                                    "There are still {amount} players on that map!", world.getPlayers().size());
        }
    }

    @Command(desc = "Remove a world")
    public void remove(CommandSender context, String world, @Flag boolean folder)
    {
        World w = this.wm.getWorld(world);
        if (w != null)
        {
            context.sendTranslated(NEGATIVE, "You have to unload the world first!");
            return;
        }
        Universe universe = multiverse.hasWorld(world);
        if (universe == null)
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", world);
            return;
        }
        universe.removeWorld(world);
        if (!folder || !module.perms().REMOVE_WORLDFOLDER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "Configuration for the world {name#world} removed!", world);
            return;
        }
        Path path = Bukkit.getServer().getWorldContainer().toPath().resolve(world);
        try
        {
            Files.delete(path);
        }
        catch (IOException e)
        {
            module.getLog().error(e, "Error while deleting world folder!");
        }
        context.sendTranslated(NEGATIVE, "Configuration and folder for the world {name#world} removed!", world);
    }

    @Command(desc = "Lists all worlds")
    public void list(CommandSender context)
    {
        context.sendTranslated(POSITIVE, "The following worlds do exist:");
        for (Universe universe : this.multiverse.getUniverses())
        {
            for (Pair<String, WorldConfig> pair : universe.getAllWorlds())
            {
                World world = this.wm.getWorld(pair.getLeft());
                if (world == null)
                {
                    context.sendTranslated(POSITIVE, "{name#world} {input#environement:color=INDIGO} {text:(not loaded):color=RED} in the universe {name}", pair.getLeft(), pair.getRight().generation.environment.name(), universe.getName());
                }
                else
                {
                    context.sendTranslated(POSITIVE, "{name#world} {input#environement:color=INDIGO} in the universe {name}", pair.getLeft(), pair.getRight().generation.environment.name(), universe.getName());
                }
            }
        }
    }
    // list / list worlds that you can enter

    @Command(desc = "Show info about a world")
    public void info(CommandSender context, String world)
    {
        WorldConfig wConfig = multiverse.getWorldConfig(world);
        if (wConfig == null)
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", world);
            return;
        }
        context.sendTranslated(POSITIVE, "World information for {input#world}:", world);
        context.sendTranslated(POSITIVE, "Gamemode: {input}", wConfig.gameMode.name());
        context.sendTranslated(POSITIVE, "Environment: {input}", wConfig.generation.environment.name());
        if (wConfig.generation.generateStructures)
        {
            context.sendTranslated(POSITIVE, "WorldType: {input} with structures", wConfig.generation.worldType.name());
        }
        else
        {
            context.sendTranslated(POSITIVE, "WorldType: {input}", wConfig.generation.worldType.name());
        }
        if (wConfig.generation.customGenerator != null)
        {
            context.sendTranslated(POSITIVE, "Using custom Generator: {input}", wConfig.generation.customGenerator);
        }
        if (!wConfig.autoLoad)
        {
            context.sendTranslated(POSITIVE, "This world will not load automatically!");
        }
        context.sendTranslated(POSITIVE, "World scale: {}", wConfig.spawn);
        if (wConfig.spawn.allowBedRespawn)
        {
            context.sendTranslated(POSITIVE, "The respawnworld is {world} and beds are allowed", wConfig.spawn.respawnWorld.getWorld());
        }
        else
        {
            context.sendTranslated(POSITIVE, "The respawnworld is {world} and beds do not set spawn", wConfig.spawn.respawnWorld.getWorld());
        }
        WorldLocation spawn = wConfig.spawn.spawnLocation;
        context.sendTranslated(POSITIVE, "This worlds spawn is at {vector}", new BlockVector3((int)spawn.x, (int)spawn.y, (int)spawn.z));
        if (!wConfig.access.free)
        {
            context.sendTranslated(POSITIVE, "Players need a permission to enter this world");
        }
        // spawning ?
        // gamerules
        // pvp
        // gamemode

        if (wConfig.netherTarget != null)
        {
            context.sendTranslated(POSITIVE, "Nether portals lead to {input#world}", wConfig.netherTarget);
        }
        if (wConfig.endTarget != null)
        {
            context.sendTranslated(POSITIVE, "End portals lead to {input#world}", wConfig.endTarget);
        }
        // TODO finish worlds info cmd
    }
    // info

    @Command(desc = "Lists the players in a world")
    public void listplayers(CommandSender context, World world)
    {
        if (world.getPlayers().isEmpty())
        {
            context.sendTranslated(NEUTRAL, "There are no players in {world}", world);
            return;
        }
        context.sendTranslated(POSITIVE, "The following players are in {world}", world);
        String s = ChatFormat.YELLOW + "  -" + ChatFormat.GOLD;
        for (Player player : world.getPlayers())
        {
            context.sendMessage(s + player.getDisplayName());
        }
    }

    // create nether & create end commands / auto link to world / only works for NORMAL Env worlds

    @Command(desc = "Sets the main world")
    public void setMainWorld(CommandSender context, World world)
    {
        Universe universe = multiverse.getUniverseFrom(world);
        universe.getConfig().mainWorld = new ConfigWorld(this.wm, world);
        context.sendTranslated(POSITIVE, "{world} is now the main world of the universe {name}", world, universe.getName());
    }
    // set main world (of universe) (of universes)
    // set main universe

    @Command(desc = "Moves a world into another universe")
    public void move(CommandSender context, World world, String universe)
    {
        Universe u = this.multiverse.getUniverse(universe);
        if (u == null)
        {
            context.sendTranslated(NEGATIVE, "Universe {input} not found!", universe);
            return;
        }
        if (u.hasWorld(world.getName()))
        {
            context.sendTranslated(NEGATIVE, "{world} is already in the universe {name}", world, u.getName());
            return;
        }
        Universe oldUniverse = multiverse.getUniverseFrom(world);
        WorldConfig worldConfig = multiverse.getWorldConfig(world.getName());
        try
        {
            oldUniverse.removeWorld(world.getName());
            oldUniverse.reload();

            worldConfig.setFile(u.getDirectory().resolve(world.getName() + YAML.getExtention()).toFile());
            worldConfig.save();

            u.reload();
        }
        catch (IOException e)
        {
            context.sendTranslated(CRITICAL, "Could not reload the universes");
            this.module.getLog().error(e, "Error while reloading after moving world to universe");
            return;
        }
        for (Player player : world.getPlayers())
        {
            User user = this.module.getCore().getUserManager().getExactUser(player.getUniqueId());
            user.sendTranslated(POSITIVE, "The world you are in got moved into an other universe!");
            oldUniverse.savePlayer(user, world);
            u.loadPlayer(user);
        }
        context.sendTranslated(POSITIVE, "World successfully moved!");
    }
    // move to other universe

    @Command(desc = "Teleports to the spawn of a world")
    @Restricted(value = User.class)
    public void spawn(User context, World world)
    {
        WorldConfig worldConfig = this.multiverse.getUniverseFrom(world).getWorldConfig(world);
        if (context.safeTeleport(worldConfig.spawn.spawnLocation.getLocationIn(world), COMMAND, false)) // TODO rotation
        {
            context.sendTranslated(POSITIVE, "You are now at the spawn of {world}!", world);
            return;
        } // else tp failed
        context.sendTranslated(NEGATIVE, "Teleport failed!");
    }

    @Command(desc = "Teleports to the spawn of the mainworld of a universe")
    @Restricted(value = User.class)
    public void uSpawn(User context, String universe)
    {
        for (Universe u : this.multiverse.getUniverses()) // TODO get Universe case insensitive
        {
            if (u.getName().equalsIgnoreCase(universe))
            {
                World world = u.getMainWorld();
                WorldConfig worldConfig = u.getWorldConfig(world);
                if (context.safeTeleport(worldConfig.spawn.spawnLocation.getLocationIn(world), COMMAND, false)) // TODO rotation
                {
                    context.sendTranslated(POSITIVE, "You are now at the spawn of {world} (main world of the universe {name})", world, universe);
                    return;
                } // else tp failed
                context.sendTranslated(NEGATIVE, "Teleport failed!");
                return;
            }
        }
        context.sendTranslated(NEGATIVE, "Universe {input} not found!", universe);
    }

    @Command(desc = "Loads a player's state for their current world")
    public void loadPlayer(CommandSender context, User player)
    {
        Universe universe = multiverse.getUniverseFrom(player.getWorld());
        universe.loadPlayer(player);
        context.sendTranslated(POSITIVE, "Loaded {user}'s data from file!", player);
    }

    @Command(desc = "Save a player's state for their current world")
    public void savePlayer(CommandSender context, User player)
    {
        Universe universe = multiverse.getUniverseFrom(player.getWorld());
        universe.savePlayer(player, player.getWorld());
        context.sendTranslated(POSITIVE, "Saved {user}'s data to file!", player.getDisplayName());
    }
}
