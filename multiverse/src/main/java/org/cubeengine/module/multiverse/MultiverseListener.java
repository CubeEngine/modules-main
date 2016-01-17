package org.cubeengine.module.multiverse;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.service.world.WorldLocation;
import org.cubeengine.module.multiverse.player.PlayerConfig;
import org.cubeengine.module.multiverse.player.PlayerDataConfig;
import org.cubeengine.service.world.ConfigWorld;
import org.cubeengine.service.world.WorldSetSpawnEvent;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.RespawnLocationData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.SleepingEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.cubeengine.service.filesystem.FileExtensionFilter.DAT;
import static org.cubeengine.service.filesystem.FileExtensionFilter.YAML;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class MultiverseListener
{
    @Listener
    public void onWorldLoad(LoadWorldEvent event)
    {
        // TODO add world to universe if not yet added
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
    public void onTeleport(DisplaceEntityEvent.Teleport event)
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

    @Listener
    public void onUniverseChange(DisplaceEntityEvent.Teleport event) // TODO instead PortalEvent?
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
    public void onJoin(ClientConnectionEvent.Join event)
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
    public void onQuit(ClientConnectionEvent.Disconnect event)
    {
        Universe universe = this.getUniverseFrom(event.getUser().getWorld());
        universe.savePlayer(event.getUser(), event.getUser().getWorld());
        this.savePlayer(event.getUser());
    }

    @Listener
    public void onRespawn(RespawnPlayerEvent event)
    {
        Universe universe = this.getUniverseFrom(world);
        event.setNewRespawnLocation(universe.getRespawnLocation(world, event.isBedSpawn(), event.getRespawnLocation()));
    }

    @Listener
    public void onBedLeave(final SleepingEvent.Post event)
    {
        if (!event.isSpawnSet())
        {
            return;
        }
        if (!this.getUniverseFrom((World)event.getBed().getExtent()).getWorldConfig((World)event.getBed().getExtent()).spawn.allowBedRespawn)
        {
            Vector3d vector3d = event.getUser().get(Keys.RESPAWN_LOCATIONS).get().get(event.getEntity().getWorld().getUniqueId());
            event.setSpawnLocation();
            // Wait until spawn is set & reset it
            final Location spawnLocation = event.getUser().getOrCreate(RespawnLocationData.class).get().getRespawnLocation();
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

    public void savePlayer(final Player player, World world)
    {
        final PlayerDataConfig config = this.module.getCore().getConfigFactory().create(PlayerDataConfig.class);
        config.applyFromPlayer(player);

        this.module.getCore().getTaskManager().runAsynchronousTask(this.module, () -> {
            config.setFile(dirPlayers.resolve(player.getUniqueId() + DAT.getExtention()).toFile());
            config.save();
        });
        this.logger.debug("PlayerData for {} in {} ({}) saved", player.getDisplayName(), world.getName(), this.getName());
    }

    public void loadPlayer(Player player)
    {
        Path path = dirPlayers.resolve(player.getUniqueId() + DAT.getExtention());
        if (Files.exists(path))
        {
            PlayerDataConfig load = this.module.getCore().getConfigFactory().load(PlayerDataConfig.class, path.toFile());
            load.applyToPlayer(player);
        }
        else
        {
            this.logger.debug("Created PlayerDataConfig for {} in the {} universe", player.getDisplayName(), this.getName());
            PlayerDataConfig save = this.module.getCore().getConfigFactory().create(PlayerDataConfig.class);
            save.setTarget(path.toFile());
            save.lastName = player.getName();
            save.applyToPlayer(player);
            save.applyFromPlayer(player);
            save.save();
            this.savePlayer(player, player.getWorld());
        }
        if (!(this.universeConfig.keepFlyMode || module.perms().KEEP_FLYMODE.isAuthorized(player)))
        {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        if (!module.perms().KEEP_GAMEMODE.isAuthorized(player))
        {
            if (this.universeConfig.enforceGameMode != null)
            {
                player.setGameMode(this.universeConfig.enforceGameMode);
            }
            else if (!(this.universeConfig.keepGameMode))
            {
                player.setGameMode(this.worldConfigs.get(player.getWorld().getName()).gameMode);
            }
        }
        this.logger.debug("PlayerData for {} in {} ({}) applied", player.getDisplayName(), player.getWorld().getName(), this.getName());
    }

}
