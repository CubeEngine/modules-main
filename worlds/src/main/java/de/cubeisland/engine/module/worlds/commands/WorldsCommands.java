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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.parameter.IncorrectUsageException;
import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.Pair;
import de.cubeisland.engine.core.util.WorldLocation;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.world.ConfigWorld;
import de.cubeisland.engine.core.world.WorldManager;
import de.cubeisland.engine.module.worlds.Multiverse;
import de.cubeisland.engine.module.worlds.Universe;
import de.cubeisland.engine.module.worlds.Worlds;
import de.cubeisland.engine.module.worlds.config.WorldConfig;

import static de.cubeisland.engine.core.filesystem.FileExtensionFilter.YAML;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends CommandContainer
{
    private Worlds module;
    private final Multiverse multiverse;
    private final WorldManager wm;

    public WorldsCommands(Worlds module, Multiverse multiverse)
    {
        super(module);
        this.module = module;
        this.multiverse = multiverse;
        this.wm = module.getCore().getWorldManager();
    }

    @Command(desc = "Creates a new universe")
    @Params(positional = @Param(label = "name"))
    public void createuniverse(CommandContext context)
    {
        context.sendMessage("TODO");
        // TODO universe create cmd
    }

    @Command(desc = "Creates a new world")
    @Params(positional = {@Param(label = "name"),
                          @Param(req = false, label = "universe")},
            nonpositional = {
                @Param(names = {"environment","env"}, type = Environment.class),
                @Param(names = "seed"),
                @Param(names = {"worldtype","type"}, type = WorldType.class),
                @Param(names = {"structure","struct"}, label = "true|false", type = Boolean.class),
                @Param(names = {"generator","gen"})})
    @Flags({@Flag(longName = "recreate",name = "r"),
            @Flag(longName = "noload",name = "no")})
    public void create(CommandContext context)
    {
        World world = this.wm.getWorld(context.getString(0));
        if (world != null)
        {
            if (context.hasFlag("r"))
            {
                context.sendTranslated(NEGATIVE, "You have to unload a world before recreating it!");
            }
            else
            {
                context.sendTranslated(NEGATIVE, "A world named {world} already exists and is loaded!", world);
            }
            return;
        }
        Path path = Bukkit.getServer().getWorldContainer().toPath().resolve(context.getString(0));
        if (Files.exists(path))
        {
            if (context.hasFlag("r"))
            {
                try
                {
                    Path newPath = path.resolveSibling(context.get(0) + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
                    Files.move(path, newPath);
                    context.sendTranslated(POSITIVE, "Old world moved to {name#folder}", path.getFileName().toString());
                }
                catch (IOException e)
                {
                    context.sendTranslated(CRITICAL, "Could not backup old world folder! Aborting Worldcreation");
                    return;
                }
            }
            else
            {
                context.sendTranslated(NEGATIVE, "A world named {name#world} already exists but is not loaded!", context.get(
                    0));
                return;
            }
        }
        WorldConfig config = this.module.getCore().getConfigFactory().create(WorldConfig.class);
        Path dir;
        Universe universe;
        if (context.hasPositional(1))
        {
            universe = multiverse.getUniverse(context.getString(1));
            if (universe == null)
            {
                universe = multiverse.createUniverse(context.getString(1));
            }
            dir = universe.getDirectory();
        }
        else if (context.getSource() instanceof User)
        {
            universe = multiverse.getUniverseFrom(((User)context.getSource()).getWorld());
            dir = universe.getDirectory();
        }

        else
        {
            context.sendTranslated(NEGATIVE, "You have to provide a universe in which to create the world!");
            context.sendMessage(context.getCommand().getDescriptor().getUsage(context.getSource()));
            return;
        }
        config.setFile(dir.resolve(context.get(0) + YAML.getExtention()).toFile());
        if (context.hasNamed("env"))
        {
            config.generation.environment = context.get("env", Environment.NORMAL);
        }
        if (context.hasNamed("seed"))
        {
            config.generation.seed = context.getString("seed");
        }
        if (context.hasNamed("type"))
        {
            config.generation.worldType = context.get("type", WorldType.NORMAL);
        }
        if (context.hasNamed("struct"))
        {
            config.generation.generateStructures = context.get("struct", true);
        }
        if (context.hasNamed("gen"))
        {
            config.generation.customGenerator = context.getString("gen");
        }
        config.save();
        if (!context.hasFlag("no"))
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
    @Params(positional = {@Param(label = "world"),
                          @Param(req = false, label = "universe")})
    public void load(CommandContext context)
    {
        World world = this.wm.getWorld(context.getString(0));
        if (world != null)
        {
            context.sendTranslated(POSITIVE, "The world {world} is already loaded!", world);
            return;
        }
        if (multiverse.hasWorld(context.getString(0)) != null)
        {
            if (context.hasPositional(1))
            {
                throw new IncorrectUsageException("You've given too many arguments.");
            }
            world = multiverse.loadWorld(context.getString(0));
            if (world != null)
            {
                context.sendTranslated(POSITIVE, "World {world} loaded!", world);
            }
            else
            {
                context.sendTranslated(NEGATIVE, "Could not load {name#world}", context.get(0));
            }
        }
        else if (Files.exists(Bukkit.getServer().getWorldContainer().toPath().resolve(context.getString(0))))
        {

            Universe universe;
            if (context.hasPositional(1))
            {
                universe = this.multiverse.getUniverse(context.getString(1));
                if (universe == null)
                {
                    context.sendTranslated(NEGATIVE, "Universe {name} not found!", context.get(1));
                    return;
                }
            }
            else if (context.getSource() instanceof User)
            {
                universe = this.multiverse.getUniverseFrom(((User)context.getSource()).getWorld());
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You need to specify a universe to load the world into!");
                return;
            }
            world = this.wm.createWorld(new WorldCreator(context.getString(0)));
            Set<World> worldToAdd = new HashSet<>();
            worldToAdd.add(world);
            universe.addWorlds(worldToAdd);
            context.sendTranslated(POSITIVE, "World {world} loaded!", world);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", context.get(0));
        }
    }

    @Command(desc = "Unload a loaded world")
    @Params(positional = @Param(label = "world", type = World.class))
    @Flags(@Flag(longName = "force", name = "f"))
    public void unload(CommandContext context)
    {
        World world = context.get(0);
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
        if (context.hasFlag("f") && !world.getPlayers().isEmpty())
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
        if (this.wm.unloadWorld(world, true))
        {
            context.sendTranslated(POSITIVE, "Unloaded the world {world}!", world);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Could not unload {world}", world);
            if (!world.getPlayers().isEmpty())
            {
                int amount = world.getPlayers().size();
                context.sendTranslatedN(NEUTRAL, amount, "There is still one player on that map!",
                                        "There are still {amount} players on that map!", world.getPlayers().size());
            }
        }
    }

    @Command(desc = "Remove a world")
    @Params(positional = @Param(label = "world"))
    @Flags(@Flag(name = "f", longName = "folder"))
    public void remove(CommandContext context)
    {
        World world = this.wm.getWorld(context.getString(0));
        if (world != null)
        {
            context.sendTranslated(NEGATIVE, "You have to unload the world first!");
            return;
        }
        Universe universe = multiverse.hasWorld(context.getString(0));
        if (universe == null)
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", context.get(0));
            return;
        }
        universe.removeWorld(context.getString(0));
        if (context.hasFlag("f") && module.perms().REMOVE_WORLDFOLDER.isAuthorized(context.getSource()))
        {
            Path path = Bukkit.getServer().getWorldContainer().toPath().resolve(context.getString(0));
            try
            {
                Files.delete(path);
            }
            catch (IOException e)
            {
                module.getLog().error(e, "Error while deleting world folder!");
            }
            context.sendTranslated(NEGATIVE, "Configuration and folder for the world {name#world} removed!", context.get(
                0));
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Configuration for the world {name#world} removed!", context.get(0));
        }
    }

    @Command(desc = "Lists all worlds")
    public void list(CommandContext context)
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
    @Params(positional = @Param(label = "world"))
    public void info(CommandContext context)
    {
        WorldConfig wConfig = multiverse.getWorldConfig(context.getString(0));
        if (wConfig == null)
        {
            context.sendTranslated(NEGATIVE, "World {input} not found!", context.get(0));
            return;
        }
        context.sendTranslated(POSITIVE, "World information for {input#world}:", context.get(0));
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
    @Params(positional = @Param(label = "world", type = World.class))
    public void listplayers(CommandContext context)
    {
        World world = context.get(0);
        if (world.getPlayers().isEmpty())
        {
            context.sendTranslated(NEUTRAL, "There are no players in {world}", world);
        }
        else
        {
            context.sendTranslated(POSITIVE, "The following players are in {world}", world);
            String s = ChatFormat.YELLOW + "  -" + ChatFormat.GOLD;
            for (Player player : world.getPlayers())
            {
                context.sendMessage(s + player.getDisplayName());
            }
        }
    }

    // create nether & create end commands / auto link to world / only works for NORMAL Env worlds

    @Command(desc = "Sets the main world")
    @Params(positional = @Param(label = "world", type = World.class))
    public void setMainWorld(CommandContext context)
    {
        World world = context.get(0);
        Universe universe = multiverse.getUniverseFrom(world);
        universe.getConfig().mainWorld = new ConfigWorld(this.wm, world);
        context.sendTranslated(POSITIVE, "{world} is now the main world of the universe {name}", world, universe.getName());
    }
    // set main world (of universe) (of universes)
    // set main universe

    @Command(desc = "Moves a world into another universe")
    @Params(positional = {@Param(label = "world", type = World.class),
                          @Param(label = "universe")})
    public void move(CommandContext context)
    {
        World world = context.get(0);
        Universe universe = this.multiverse.getUniverse(context.getString(1));
        if (universe == null)
        {
            context.sendTranslated(NEGATIVE, "Universe {input} not found!", context.get(1));
            return;
        }
        if (universe.hasWorld(world.getName()))
        {
            context.sendTranslated(NEGATIVE, "{world} is already in the universe {name}", world, universe.getName());
            return;
        }
        Universe oldUniverse = multiverse.getUniverseFrom(world);
        WorldConfig worldConfig = multiverse.getWorldConfig(world.getName());
        try
        {
            oldUniverse.removeWorld(world.getName());
            oldUniverse.reload();

            worldConfig.setFile(universe.getDirectory().resolve(world.getName() + YAML.getExtention()).toFile());
            worldConfig.save();

            universe.reload();
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
            universe.loadPlayer(user);
        }
        context.sendTranslated(POSITIVE, "World successfully moved!");
    }
    // move to other universe

    @Command(desc = "Teleports to the spawn of a world")
    @Params(positional = @Param(label = "world", type = World.class))
    @Restricted(value = User.class)
    public void spawn(CommandContext context)
    {
        User user = (User)context.getSource();
        /* TODO spawn to universe
        if (name.startsWith("u:"))
        {
            name = name.substring(2);
            for (Universe universe : this.multiverse.getUniverses())
            {
                if (universe.getName().equalsIgnoreCase(name))
                {
                    World world = universe.getMainWorld();
                    WorldConfig worldConfig = universe.getWorldConfig(world);
                    if (user.safeTeleport(worldConfig.spawn.spawnLocation.getLocationIn(world), TeleportCause.COMMAND, false))
                    {
                        context.sendTranslated(POSITIVE, "You are now at the spawn of {world} (main world of the universe {name})", world, name);
                        return;
                    } // else tp failed
                    return;
                }
            }
            context.sendTranslated(NEGATIVE, "Universe {input} not found!", name);
            return;
        }
        */
        World world = context.get(0);
        WorldConfig worldConfig = this.multiverse.getUniverseFrom(world).getWorldConfig(world);
        if (user.safeTeleport(worldConfig.spawn.spawnLocation.getLocationIn(world), TeleportCause.COMMAND, false))
        {
            context.sendTranslated(POSITIVE, "You are now at the spawn of {world}!", world);
            return;
        } // else tp failed
    }

    @Command(desc = "Loads a player's state for their current world")
    @Params(positional = @Param(label = "player", type = User.class))
    public void loadPlayer(CommandContext context)
    {
        User user = context.get(0);
        Universe universe = multiverse.getUniverseFrom(user.getWorld());
        universe.loadPlayer(user);
        context.sendTranslated(POSITIVE, "Loaded {user}'s data from file!", user);
    }

    @Command(desc = "Save a player's state for their current world")
    @Params(positional = @Param(label = "player", type = User.class))
    public void savePlayer(CommandContext context)
    {
        User user = context.get(0);
        Universe universe = multiverse.getUniverseFrom(user.getWorld());
        universe.savePlayer(user, user.getWorld());
        context.sendTranslated(POSITIVE, "Saved {user}'s data to file!", user.getDisplayName());
    }
}
