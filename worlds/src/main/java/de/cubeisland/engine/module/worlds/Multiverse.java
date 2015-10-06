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
package de.cubeisland.engine.module.worlds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import com.flowpowered.math.vector.Vector3d;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.WorldLocation;
import org.cubeengine.service.world.ConfigWorld;
import org.cubeengine.service.world.WorldManager;
import org.cubeengine.service.world.WorldSetSpawnEvent;
import de.cubeisland.engine.module.worlds.config.WorldConfig;
import de.cubeisland.engine.module.worlds.config.WorldsConfig;
import de.cubeisland.engine.module.worlds.player.PlayerConfig;
import de.cubeisland.engine.module.worlds.player.PlayerDataConfig;
import de.cubeisland.engine.reflect.Reflector;

import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.world.DimensionTypes.END;
import static org.spongepowered.api.world.DimensionTypes.NETHER;
import static org.spongepowered.api.world.DimensionTypes.OVERWORLD;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.RespawnLocationData;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.EntityEnterPortalEvent;
import org.spongepowered.api.event.entity.EntityExitPortalEvent;
import org.spongepowered.api.event.entity.EntityTeleportEvent;
import org.spongepowered.api.event.entity.player.PlayerChangeWorldEvent;
import org.spongepowered.api.event.entity.player.PlayerDeathEvent;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.event.entity.player.PlayerLeaveBedEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.entity.player.PlayerRespawnEvent;
import org.spongepowered.api.event.world.WorldLoadEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * Holds multiple parallel universes
 */
public class Multiverse
{
    private final Worlds module;
    private final WorldManager wm;
    private WorldsConfig config;
    private Log logger;
    private Reflector reflector;

    private final Map<String, Universe> universes = new HashMap<>(); // universeName -> Universe
    private final Map<String, Universe> worlds = new HashMap<>(); // worldName -> belonging to Universe

    private Map<UUID, World> lastDeath = new HashMap<>();

    private final Path dirPlayers;
    private final Path dirUniverses;
    private final Path dirErrors;

    private final Permission universeRootPerm;

    public Multiverse(Worlds module, WorldsConfig config, WorldManager wm, Path modulePath, CommandManager cm, Log logger, EventManager em, Reflector reflector) throws IOException
    {
        this.module = module;
        this.wm = wm;
        this.config = config;
        this.logger = logger;
        this.reflector = reflector;
        this.universeRootPerm = module.getProvided(Permission.class).childWildcard("universe");

        this.dirPlayers = modulePath.resolve("players"); // config for last world
        Files.createDirectories(this.dirPlayers);

        this.dirErrors = modulePath.resolve("errors");
        Files.createDirectories(dirErrors);
        this.dirUniverses = modulePath.resolve("universes");

        if (Files.exists(dirUniverses))
        {
            for (Path universeDir : Files.newDirectoryStream(dirUniverses))
            {
                if (Files.isDirectory(universeDir))
                {
                    String universeName = universeDir.getFileName().toString();
                    if (this.config.mainUniverse == null)
                    {
                        this.config.mainUniverse = universeName;
                        this.config.save();
                    }
                    this.universes.put(universeName, Universe.load(this.module, this, universeDir));
                }
            }
            List<World> missingWorlds = this.wm.getWorlds();
            Map<String, Set<World>> found = new HashMap<>();
            for (Entry<String, Universe> entry : this.universes.entrySet())
            {
                found.put(entry.getKey(), new HashSet<>(entry.getValue().getWorlds()));
                missingWorlds.removeAll(entry.getValue().getWorlds());
            }
            if (!missingWorlds.isEmpty())
            {
                CommandSender sender = cm.getConsoleSender();
                sender.sendTranslated(NEUTRAL, "Discovering unknown worlds...");
                this.searchUniverses(found, missingWorlds, sender);
                sender.sendTranslated(NEUTRAL, "Finishing research...");
                for (Entry<String, Set<World>> entry : found.entrySet())
                {
                    Universe universe = this.universes.get(entry.getKey());
                    Set<World> foundWorlds = entry.getValue();
                    if (universe == null)
                    {
                        this.universes.put(entry.getKey(), Universe.create(this.module, this, dirUniverses.resolve(
                            entry.getKey()), foundWorlds));
                    }
                    else
                    {
                        foundWorlds.removeAll(universe.getWorlds());
                        if (foundWorlds.isEmpty())
                        {
                            continue;
                        }
                        universe.addWorlds(foundWorlds);
                    }
                    sender.sendTranslated(NEUTRAL, "Found {amount} new worlds in the universe {name#universe}!", foundWorlds.size(), entry.getKey());
                }
            }
        }
        else
        {
            logger.info("No previous Universes found! Initializing...");
            CommandSender sender = cm.getConsoleSender();
            sender.sendTranslated(NEUTRAL, "Scraping together Matter...");
            Map<String, Set<World>> found = new HashMap<>();
            this.searchUniverses(found, this.wm.getWorlds(), sender);
            sender.sendTranslated(NEUTRAL, "Finishing research...");
            for (Entry<String, Set<World>> entry : found.entrySet())
            {
                Path universeDir = dirUniverses.resolve(entry.getKey());
                Files.createDirectories(universeDir);
                this.universes.put(entry.getKey(), Universe.create(this.module, this, universeDir, entry.getValue()));
            }
            sender.sendTranslated(NEUTRAL, "Found {amount#universes} universes with {amount#worlds} worlds!", found.size(), this.wm.getWorlds().size());
        }
        for (Universe universe : this.universes.values())
        {
            for (World world : universe.getWorlds())
            {
                this.worlds.put(world.getName(), universe);
            }
        }
        if (this.config.mainUniverse == null || this.universes.get(this.config.mainUniverse) == null)
        {
            Universe universe = this.universes.get("world");
            if (universe == null)
            {
                universe = this.universes.values().iterator().next();
            }
            logger.warn("No main universe set. {} is now the main universe!", universe.getName());
            this.config.mainUniverse = universe.getName();
            this.config.save();
        }
        em.registerListener(this.module, this);
    }

    private void searchUniverses(Map<String, Set<World>> found, Collection<World> worldList, CommandSender sender)
    {
        for (World world : worldList)
        {
            String universeName;
            if (world.getName().contains("_"))
            {
                universeName = world.getName();
                universeName = universeName.substring(0, universeName.indexOf("_"));
            }
            else
            {
                universeName = world.getName();
            }
            Set<World> worlds = found.get(universeName);
            if (worlds == null)
            {
                sender.sendTranslated(NEUTRAL, "Discovered a new Universe! Heating up stars...");
                worlds = new HashSet<>();
                found.put(universeName, worlds);
            }
            worlds.add(world);
            DimensionType type = world.getProperties().getDimensionType();
            if (type == OVERWORLD)
            {
                sender.sendTranslated(NEUTRAL, "{world} gets formed by crushing rocks together in the universe {name#universe}", world, universeName);
            }
            else if (type == NETHER)
            {
                sender.sendTranslated(NEUTRAL, "Cooling plasma a bit to make {world} in the universe {name#universe}", world, universeName);
            }
            else if (type == END)
            {
                sender.sendTranslated(NEUTRAL, "Found a cold rock named {world} in the universe {name#universe}", world, universeName);
            }
        }
    }

    @Listener
    public void onWorldLoad(WorldLoadEvent event)
    {
        Universe from = this.getUniverseFrom(event.getWorld());
        if (from == null)
        {
            String name = event.getWorld().getName();
            while (this.getUniverse(name) != null)
            {
                name = name + "_";
            }
            Set<World> worlds = new HashSet<>();
            worlds.add(event.getWorld());
            Universe universe = this.createUniverse(name);
            this.universes.put(universe.getName(), universe);
            universe.addWorlds(worlds);
            WorldConfig wConfig = universe.getWorldConfig(event.getWorld());
            wConfig.autoLoad = false;
            wConfig.save();
        }
    }

    @Listener
    public void onSetSpawn(WorldSetSpawnEvent event)
    {
        WorldConfig worldConfig = this.getWorldConfig(event.getWorld());
        worldConfig.spawn.spawnLocation = new WorldLocation(event.getNewLocation(), event.getRotation());
        worldConfig.updateInheritance();
        worldConfig.save();
    }

    @Listener
    public void onWorldChange(PlayerChangeWorldEvent event)
    {
        Player player = event.getUser();
        World fromWorld = event.getFromWorld();
        try
        {
            Universe oldUniverse = this.getUniverseFrom(fromWorld);
            Universe newUniverse = this.getUniverseFrom(event.getToWorld());
            if (oldUniverse != newUniverse)
            {
                player.closeInventory();
                oldUniverse.savePlayer(player, fromWorld);
                newUniverse.loadPlayer(player);
            }
            // TODO else need to change gamemode?
            this.savePlayer(player);
        }
        catch (UniverseCreationException e)
        {
            Path errorFile = dirErrors.resolve(fromWorld.getName() + "_" + player.getName() + ".dat");
            int i = 1;
            while (Files.exists(errorFile))
            {
                errorFile = dirErrors.resolve(fromWorld.getName() + "_" + player.getName() + "_" + i++ + ".dat");
            }
            logger.warn("The Player {} teleported into a universe that couldn't get created! "
                            + "The overwritten Inventory is saved under /errors/{}", player.getName(),
                        errorFile.getFileName().toString());
            PlayerDataConfig pdc = reflector.create(PlayerDataConfig.class);
            pdc.setHead(new SimpleDateFormat().format(new Date()) + " " +
                            player.getName() + "(" + player.getUniqueId() + ") ported to " + player.getWorld().getName() +
                            " but couldn't create the universe",
                        "This are the items the player had previously. They got overwritten!");
            pdc.setFile(errorFile.toFile());
            pdc.applyFromPlayer(player);
            pdc.save();

            new PlayerDataConfig().applyToPlayer(player);
        }
    }

    @Listener
    public void onTeleport(EntityTeleportEvent event)
    {
        if (!(event.getEntity() instanceof Player))
        {
            return;
        }
        Location to = event.getNewLocation();
        if (event.getOldLocation().getExtent() == to.getExtent())
        {
            return;
        }
        Player player = ((Player)event.getEntity());
        Universe universe = this.getUniverseFrom((World)to.getExtent());
        if (!universe.checkPlayerAccess(player, ((World)to.getExtent())))
        {
            event.setCancelled(true); // TODO check if player has access to the world he is currently in
            User user = um.getExactUser(player.getUniqueId());
            user.sendTranslated(NEGATIVE, "You are not allowed to enter the universe {name#universe}!",
                                universe.getName());
        }
    }


    // TODO handle different Netherportal / Endportal behaviour
    @Listener
    public void onPortalEnter(EntityEnterPortalEvent event)
    {
        Universe universe = getUniverseFrom(event.getEntity().getWorld());
        //universe.handleNetherTarget()
        //universe.handleEndTarget()
    }

    @Listener
    public void onPortalExit(EntityExitPortalEvent event)
    {
    }


    @Listener
    public void onUniverseChange(EntityTeleportEvent event) // TODO instead PortalEvent?
    {
        Location oldLoc = event.getOldLocation();
        Location newLoc = event.getNewLocation();
        if (oldLoc.getExtent().equals(newLoc.getExtent()))
        {
            return;
        }
        if (oldLoc.getExtent() instanceof World && newLoc.getExtent() instanceof World)
        {
            Universe oldUniverse = getUniverseFrom(((World)oldLoc.getExtent()));
            Universe newUniverse = getUniverseFrom(((World)newLoc.getExtent()));
            if (oldUniverse == newUniverse)
            {
                return;
            }

            if (!(event.getEntity() instanceof Player))
            {
                // Can Entities teleport?
                if (oldUniverse.getConfig().entityTp.enable && newUniverse.getConfig().entityTp.enable)
                {
                    event.setCancelled(true);
                }
                if (event.getEntity() instanceof Carrier)
                {
                    // Can Carriers (InventoryHolders) teleport?
                    if (oldUniverse.getConfig().entityTp.inventory && newUniverse.getConfig().entityTp.inventory)
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onJoin(PlayerJoinEvent event)
    {
        if (this.config.adjustFirstSpawn && !event.getUser().get(Keys.FIRST_DATE_PLAYED).isPresent())
        {
            Universe universe = this.universes.get(this.config.mainUniverse);
            World world = universe.getMainWorld();
            WorldConfig worldConfig = universe.getWorldConfig(world);
            event.getEntity().setLocation(worldConfig.spawn.spawnLocation.getLocationIn(world));
            event.getEntity().setRotation(worldConfig.spawn.spawnLocation.getRotation());
        }
        this.checkForExpectedWorld(event.getEntity());
    }


    @Listener
    public void onQuit(PlayerQuitEvent event)
    {
        Universe universe = this.getUniverseFrom(event.getUser().getWorld());
        universe.savePlayer(event.getUser(), event.getUser().getWorld());
        this.savePlayer(event.getUser());
    }

    @Listener
    public void onRespawn(PlayerRespawnEvent event)
    {
        World world = lastDeath.remove(event.getUser().getUniqueId());
        if (world == null)
        {
            world = event.getUser().getWorld();
        }
        Universe universe = this.getUniverseFrom(world);
        event.setNewRespawnLocation(universe.getRespawnLocation(world, event.isBedSpawn(), event.getRespawnLocation()));
    }

    @Listener
    public void onDeath(PlayerDeathEvent event)
    {
        lastDeath.put(event.getEntity().getUniqueId(), event.getEntity().getWorld());
    }

    @Listener
    public void onBedLeave(final PlayerLeaveBedEvent event)
    {
        if (!event.wasSpawnSet())
        {
            return;
        }
        if (!this.getUniverseFrom((World)event.getBed().getExtent()).getWorldConfig((World)event.getBed().getExtent()).spawn.allowBedRespawn)
        {
            Vector3d vector3d = event.getUser().get(Keys.RESPAWN_LOCATIONS).get().get(event.getEntity().getWorld().getUniqueId());
            event.setSpawnLocation();
            // Wait until spawn is set & reset it
            final Location spawnLocation = event.getUser().getOrCreate(RespawnLocationData.class).get().getRespawnLocation()
            tm.runTaskDelayed(module, () -> event.getUser().offer(event.getUser().getOrCreate(RespawnLocationData.class).get().setRespawnLocation(spawnLocation)), 1);
        }
    }

    private void checkForExpectedWorld(Player player)
    {
        Path path = dirPlayers.resolve(player.getUniqueId() + YAML.getExtention());
        PlayerConfig config;
        if (Files.exists(path))
        {
            config = reflector.load(PlayerConfig.class, path.toFile(), false);
            if (config.lastWorld != null)
            {
                Universe universe = this.getUniverseFrom(player.getWorld());
                Universe expected = this.getUniverseFrom(config.lastWorld.getWorld());
                if (universe != expected)
                {
                    // expectedworld-actualworld_playername.yml
                    Path errorFile = dirErrors.resolve(player.getWorld().getName() + "_" + player.getName() + ".dat");
                    int i = 1;
                    while (Files.exists(errorFile))
                    {
                        errorFile = dirErrors.resolve(player.getWorld().getName() + "_" + player.getName() + "_" + i++ + ".dat");
                    }
                    logger.warn(
                        "The Player {} was not in the expected world! Overwritten Inventory is saved under /errors/{}",
                        player.getName(), errorFile.getFileName().toString());
                    PlayerDataConfig pdc = this.module.getCore().getConfigFactory().create(PlayerDataConfig.class);
                    pdc.setHead(new SimpleDateFormat().format(new Date()) + " " +
                                    player.getDisplayName() + "(" + player.getUniqueId() + ") did not spawn in " + config.lastWorld.getName() +
                                    " but instead in " + player.getWorld().getName(),
                                "These are the items the player had when spawning. They were overwritten!");
                    pdc.setFile(errorFile.toFile());
                    pdc.applyFromPlayer(player);
                    pdc.save();
                }
                if (config.lastWorld.getWorld() == player.getWorld())
                {
                    return; // everything is ok
                }
                logger.debug("{} was not in expected world {} instead of {}", player.getDisplayName(),
                             player.getWorld().getName(), config.lastWorld.getName());
                universe.loadPlayer(player);
                // else save new world (strange that player changed world but nvm
            }
            // else no last-world saved. why does this file exist?
        }
        else
        {
            logger.debug("Created PlayerConfig for {}", player.getDisplayName());
            config = reflector.create(PlayerConfig.class);
        }
        config.lastName = player.getName();
        config.lastWorld = new ConfigWorld(this.wm, player.getWorld()); // update last world
        config.setFile(path.toFile());
        config.save();
    }

    private void savePlayer(Player player)
    {
        Path path = this.dirPlayers.resolve(player.getUniqueId() + YAML.getExtention());
        PlayerConfig config = this.module.getCore().getConfigFactory().load(PlayerConfig.class, path.toFile());
        config.lastWorld = new ConfigWorld(this.wm, player.getWorld());
        config.save();
        logger.debug("{} is now in the world: {} ({})", player.getDisplayName(), player.getWorld().getName(),
                     this.getUniverseFrom(player.getWorld()).getName());
    }

    private WorldConfig getWorldConfig(World world)
    {
        return this.getUniverseFrom(world).getWorldConfig(world);
    }


    public WorldConfig getWorldConfig(String name)
    {
        for (Universe universe : this.universes.values())
        {
            if (universe.hasWorld(name))
            {
                return universe.getWorldConfig(name);
            }
        }
        return null;
    }

    public Permission getUniverseRootPerm()
    {
        return universeRootPerm;
    }

    public Universe getUniverseFrom(World world)
    {
        if (world == null)
        {
            return null;
        }
        Universe universe = this.worlds.get(world.getName());
        if (universe == null)
        {
            HashSet<World> set = new HashSet<>();
            set.add(world);
            String universeName = world.getName();
            if (world.getName().contains("_"))
            {
                universeName = world.getName().substring(0, world.getName().indexOf("_"));
                if (this.universes.containsKey(universeName))
                {
                    logger.info("Added world {} to universe {}", world.getName(), universeName);
                    universe = universes.get(universeName);
                    universe.addWorlds(set);
                    return universe;
                }
            }
            logger.info("Created new universe {} containing the world {}", universeName, world.getName());
            Path dirUniverse = dirUniverses.resolve(universeName);
            try
            {
                Files.createDirectories(dirUniverse);
                universe = Universe.create(module, this, dirUniverse, set);
            }
            catch (IOException e)
            {
                throw new UniverseCreationException(e);
            }
        }
        return universe;
    }

    public Universe createUniverse(String name)
    {
        Universe universe = universes.get(name);
        if (universe == null)
        {
            try
            {
                return Universe.create(module, this, dirUniverses.resolve(name), new HashSet<World>());
            }
            catch (IOException e)
            {
                throw new UniverseCreationException(e);
            }
        }
        return universe;
    }

    public World loadWorld(String name)
    {
        Universe universe = this.hasWorld(name);
        return universe.loadWorld(name);
    }

    public Universe hasWorld(String name)
    {
        for (Universe universe : this.universes.values())
        {
            if (universe.hasWorld(name))
            {
                return universe;
            }
        }
        return null;
    }

    public Collection<Universe> getUniverses()
    {
        return this.universes.values();
    }


    public World getMainWorld()
    {
        return this.getMainUniverse().getMainWorld();
    }

    public Universe getUniverse(String name)
    {
        return this.universes.get(name);
    }

    public Universe getMainUniverse()
    {
        return this.universes.get(this.config.mainUniverse);
    }
}
