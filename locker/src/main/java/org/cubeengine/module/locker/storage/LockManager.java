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
package org.cubeengine.module.locker.storage;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.BlockUtil.CARDINAL_DIRECTIONS;
import static org.cubeengine.libcube.util.LocationUtil.getChunkKey;
import static org.cubeengine.libcube.util.LocationUtil.getLocationKey;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_ALL;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_FULL;
import static org.cubeengine.module.locker.storage.ProtectedType.getProtectedType;
import static org.cubeengine.module.locker.storage.TableAccessList.TABLE_ACCESSLIST;
import static org.cubeengine.module.locker.storage.TableLockLocations.TABLE_LOCK_LOCATIONS;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCKS;
import static org.spongepowered.api.block.BlockTypes.ACACIA_DOOR;
import static org.spongepowered.api.block.BlockTypes.BIRCH_DOOR;
import static org.spongepowered.api.block.BlockTypes.CHEST;
import static org.spongepowered.api.block.BlockTypes.DARK_OAK_DOOR;
import static org.spongepowered.api.block.BlockTypes.IRON_DOOR;
import static org.spongepowered.api.block.BlockTypes.JUNGLE_DOOR;
import static org.spongepowered.api.block.BlockTypes.SPRUCE_DOOR;
import static org.spongepowered.api.block.BlockTypes.TRAPPED_CHEST;
import static org.spongepowered.api.block.BlockTypes.WOODEN_DOOR;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;

import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.service.database.AsyncRecord;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.commands.CommandListener;
import org.cubeengine.module.locker.commands.PlayerAccess;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.jooq.Batch;
import org.jooq.Condition;
import org.jooq.Result;
import org.jooq.types.UInteger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.block.trait.EnumTraits;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

public class LockManager
{
    public static final int VIEWDISTANCE_DEFAULT = 10;
    protected final Locker module;
    private Database database;

    protected TaskManager tm;
    private I18n i18n;
    private final StringMatcher stringMatcher;
    protected Log logger;
    public final CommandListener commandListener;

    private final Map<UUID, Map<Long, Lock>> loadedLocks = new HashMap<>(); // World -> LocationKey -> Lock
    private final Map<UUID, Map<Long, Set<Lock>>> loadedLocksInChunk = new HashMap<>(); // World -> ChunkKey -> Lock
    private final Map<UUID, Lock> loadedEntityLocks = new HashMap<>(); // EntityID -> Lock
    private final Map<UInteger, Lock> locksById = new HashMap<>(); // All Locks - LockID -> Lock

    private final Map<UUID, Set<UInteger>> unlocked = new HashMap<>();

    public final MessageDigest messageDigest;

    private final Set<Long> loadedChunks = new CopyOnWriteArraySet<>();

    private boolean blockAsync = true;

    private final Queue<Chunk> loadChunks = new ConcurrentLinkedQueue<>();
    private final ExecutorService loadExecutor;
    private Future<?> loadFuture = null;

    private final Queue<Chunk> unloadChunks = new ConcurrentLinkedQueue<>();
    private final ExecutorService unloadExecutor;
    private Future<?> unloadFuture = null;

    @Inject
    public LockManager(Locker module, EventManager em, StringMatcher stringMatcher, Database database, TaskManager tm, I18n i18n)
    {
        this.stringMatcher = stringMatcher;
        this.database = database;
        this.tm = tm;
        this.i18n = i18n;
        logger = module.getProvided(Log.class);
        this.module = module;
        loadExecutor = Executors.newSingleThreadExecutor(module.getProvided(ThreadFactory.class));
        unloadExecutor = Executors.newSingleThreadExecutor(module.getProvided(ThreadFactory.class));
        try
        {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 hash algorithm not available!");
        }
        this.commandListener = new CommandListener(module, this, i18n);
        em.registerListener(Locker.class, this.commandListener);
        em.registerListener(Locker.class, this);

        // Start Timer
        tm.runTimer(Locker.class, this::doLoadChunks, 5, 5); // 5 Ticks
        tm.runTimer(Locker.class, this::doUnloadChunks, 100, 100); // 100 Ticks - 5 seconds
    }

    private void doUnloadChunks()
    {
        if (!blockAsync && !unloadChunks.isEmpty() && (unloadFuture == null || unloadFuture.isDone()))
        {
            unloadFuture = unloadExecutor.submit(this::unloadLocksInChunk);
        }
    }

    private void doLoadChunks()
    {
        if (!blockAsync && !loadChunks.isEmpty() && (loadFuture == null || loadFuture.isDone()))
        {
            loadFuture = loadExecutor.submit(this::loadLocksInChunks);
        }
    }

    @Listener
    public void onChunkLoad(LoadChunkEvent event)
    {
        Chunk chunk = event.getTargetChunk();
        queueChunk(chunk);
    }

    @Listener
    public void onServerStarted(GameStartingServerEvent event)
    {
        reloadLocks();
    }

    private void queueChunk(Chunk chunk)
    {
        if (loadedChunks.contains(getChunkKey(chunk.getPosition().getX(), chunk.getPosition().getZ())))
        {
            return; // Chunk already loaded
        }
        loadChunks.add(chunk);
        unloadChunks.remove(chunk);
    }

    @Listener
    public void onChunkUnload(UnloadChunkEvent event)
    {
        Chunk chunk = event.getTargetChunk();
        if (loadChunks.remove(chunk)) // remove from load queue
        {
            return; // if in load queue unloading is unnecessary
        }
        unloadChunks.add(chunk);
    }

    private void loadLocksInChunks()
    {
        if (loadChunks.isEmpty())
        {
            return;
        }

        Chunk chunk = loadChunks.poll();

        // TODO get view-distance if available / default to 10 ; DedicatedServer.getIntProperty("view-distance", 10)
        int viewDistance = 10;

        Vector3i position = chunk.getPosition();
        int chunkX = position.getX();
        int chunkZ = position.getZ();

        List<Integer> xPos = IntStream.range(chunkX - viewDistance, chunkX + viewDistance).mapToObj(Integer.class::cast).collect(toList());
        List<Integer> zPos = IntStream.range(chunkZ - viewDistance, chunkZ + viewDistance).mapToObj(Integer.class::cast).collect(toList());

        Condition condChunkX = TABLE_LOCK_LOCATIONS.CHUNKX.in(xPos);
        Condition condChunkZ = TABLE_LOCK_LOCATIONS.CHUNKZ.in(zPos);
        Condition condNotLoaded = TABLE_LOCK_LOCATIONS.LOCK_ID.notIn(locksById.keySet());

        Result<LockModel> models = this.database.getDSL().selectFrom(TABLE_LOCKS).where(TABLE_LOCKS.ID.in(
                this.database.getDSL().select(TABLE_LOCK_LOCATIONS.LOCK_ID).from(
                        TABLE_LOCK_LOCATIONS).where(TABLE_LOCK_LOCATIONS.WORLD_ID.eq(chunk.getWorld().getUniqueId()), condChunkX, condChunkZ, condNotLoaded))).fetch();
        Map<UInteger, Result<LockLocationModel>> locations = LockManager.
            this.database.getDSL().selectFrom(TABLE_LOCK_LOCATIONS).where(
            TABLE_LOCK_LOCATIONS.LOCK_ID.in(
            models.getValues(TABLE_LOCKS.ID))).fetch().intoGroups(TABLE_LOCK_LOCATIONS.LOCK_ID);
        for (LockModel model : models)
        {
            Result<LockLocationModel> lockLoc = locations.get(model.getValue(TABLE_LOCKS.ID));
            Lock lock = new Lock(this, model, i18n, lockLoc);
            addLoadedLocationLock(lock);
            // System.out.print("Lock loaded at: " + lock.getFirstLocation().getPosition() + "\n");
        }

        for (Integer xPo : xPos)
        {
            loadedChunks.addAll(zPos.stream().map(zPo -> getChunkKey(xPo, zPo)).collect(Collectors.toList()));
        }

        if (!blockAsync && !loadChunks.isEmpty())
        {
            loadFuture = loadExecutor.submit(this::loadLocksInChunks);
        }
    }

    private void addLoadedLocationLock(Lock lock)
    {
        int viewDistance = VIEWDISTANCE_DEFAULT;
        this.locksById.put(lock.getId(), lock);
        for (Location loc : lock.getLocations())
        {
            Map<Long, Set<Lock>> locksInChunkMap = this.getChunkLocksMap(loc.getExtent().getUniqueId());
            long chunkKey = getChunkKey(loc.getBlockX() / viewDistance, loc.getBlockZ() / viewDistance);
            Set<Lock> locks = locksInChunkMap.get(chunkKey);
            if (locks == null)
            {
                locks = new HashSet<>();
                locksInChunkMap.put(chunkKey, locks);
            }
            locks.add(lock);
            this.getLocLockMap(loc.getExtent().getUniqueId()).put(getLocationKey(loc), lock);
        }
    }

    private Map<Long, Set<Lock>> getChunkLocksMap(UUID worldId)
    {
        Map<Long, Set<Lock>> locksInChunkMap = this.loadedLocksInChunk.get(worldId);
        if (locksInChunkMap == null)
        {
            locksInChunkMap = new HashMap<>();
            this.loadedLocksInChunk.put(worldId, locksInChunkMap);
        }
        return locksInChunkMap;
    }

    private Map<Long, Lock> getLocLockMap(UUID worldId)
    {
        Map<Long, Lock> locksAtLocMap = this.loadedLocks.get(worldId);
        if (locksAtLocMap == null)
        {
            locksAtLocMap = new HashMap<>();
            this.loadedLocks.put(worldId, locksAtLocMap);
        }
        return locksAtLocMap;
    }

    private void unloadLocksInChunk()
    {
        try
        {
            if (unloadChunks.isEmpty())
            {
                return;
            }
            Chunk chunk = unloadChunks.poll();
            if (chunk.isLoaded())  // its loaded?
            {
                return;
            }
            int x = chunk.getPosition().getX();
            int z = chunk.getPosition().getZ();
            World world = chunk.getWorld();
            if (world.getChunk(x - 1, 0, z - 1).isPresent() ||
                world.getChunk(x - 1, 0, z + 1).isPresent() ||
                world.getChunk(x + 1, 0, z - 1).isPresent() ||
                world.getChunk(x + 1, 0, z + 1).isPresent())
            {
                return; // Do not unload if any neighbour chunk is loaded
            }

            Set<Lock> removed = this.getChunkLocksMap(chunk.getWorld().getUniqueId()).remove(getChunkKey(x, z));
            if (removed == null) return; // nothing to remove
            Map<Long, Lock> locLockMap = this.getLocLockMap(chunk.getWorld().getUniqueId());
            for (Lock lock : removed) // remove from chunks
            {
                // System.out.print("Lock unloaded at: " + lock.getFirstLocation().getPosition() + "\n");
                this.locksById.remove(lock.getId());
                for (Location loc : lock.getLocations())
                {
                    locLockMap.remove(getLocationKey(loc));
                }
                if (lock.model.changed())
                {
                    lock.model.updateAsync(); // updates if changed (last_access timestamp)
                }
            }
        }
        finally
        {
            if (!blockAsync && !unloadChunks.isEmpty())
            {
                unloadFuture = unloadExecutor.submit(this::unloadLocksInChunk);
            }
        }
    }

    /**
     * Returns the Lock at given Location
     *
     * @param location the location of the lock
     * @return the lock or null if there is no lock OR the chunk is not loaded
     */
    public Lock getLock(Location<World> location)
    {
        return this.getLocLockMap(location.getExtent().getUniqueId()).get(getLocationKey(location));
    }

    public Lock getValidLock(Location<World> location, Player player)
    {
        Lock lock = getLock(location);
        if (lock == null)
        {
            return null;
        }
        if (!lock.validateTypeAt(location))
        {
            lock.delete(player);
            if (player != null)
            {
                i18n.send(player, NEUTRAL, "Deleted invalid BlockProtection!");
            }
        }
        return lock;
    }


    /**
     * Returns the Lock for given entityUID
     *
     * @param uniqueId the entities unique id
     * @return the entity-lock or null if there is no lock
     */
    public Lock getLockForEntityUID(UUID uniqueId)
    {
        Lock lock = this.loadedEntityLocks.get(uniqueId);
        if (lock == null)
        {
            LockModel model = database.getDSL().selectFrom(TABLE_LOCKS).where(TABLE_LOCKS.ENTITY_UUID.eq(uniqueId)).fetchOne();
            if (model != null)
            {
                lock = new Lock(this, model, i18n);
                this.loadedEntityLocks.put(uniqueId, lock);
            }
        }
        return lock;
    }

    /*
    private Lock handleLockAccess(Lock lock, boolean access)
    {
        if (lock == null)
        {
            return lock;
        }
        // TODO move protection logic
        if ((this.module.getConfig().protectWhenOnlyOffline && lock.getOwner().isOnline())
        || (this.module.getConfig().protectWhenOnlyOnline && !lock.getOwner().isOnline()))
        {
            return null;
        }
        // TODO move timestamp logic -> Lock.updateAccess
        lock.model.setValue(TABLE_LOCK.LAST_ACCESS, new Timestamp(System.currentTimeMillis()));
        return lock;
    }
    */

    /**
     * Extends a location lock onto an other location
     *
     * @param lock the lock to extend
     * @param location the location to extend to
     */
    public void extendLock(Lock lock, Location<World> location)
    {
        if (this.getLock(location) != null)
        {
            throw new IllegalStateException("Cannot extend Lock onto another!");
        }
        lock.locations.add(location);
        LockLocationModel model = database.getDSL().newRecord(TABLE_LOCK_LOCATIONS).newLocation(lock.model, location);
        model.insertAsync();
        this.getLocLockMap(location.getExtent().getUniqueId()).put(getLocationKey(location), lock);
    }

    /**
     * Removes a Lock if th
     * e user is authorized or the lock destroyed
     *
     * @param lock the lock to remove
     * @param user the user removing the lock (can be null)
     * @param destroyed true if the Lock is already destroyed
     */
    public void removeLock(Lock lock, Player user, boolean destroyed)
    {
        if (destroyed || lock.isOwner(user) || user.hasPermission(module.perms().CMD_REMOVE_OTHER.getId()))
        {
            this.locksById.remove(lock.getId());
            lock.model.deleteAsync();
            if (lock.isBlockLock())
            {
                for (Location location : lock.getLocations())
                {
                    long chunkKey = getChunkKey(location);
                    this.getLocLockMap(location.getExtent().getUniqueId()).remove(getLocationKey(location));
                    Set<Lock> locks = this.getChunkLocksMap(location.getExtent().getUniqueId()).get(chunkKey);
                    if (locks != null)
                    {
                        locks.remove(lock);
                    }
                }
            }
            else
            {
                this.loadedEntityLocks.remove(lock.model.getUUID());
            }
            if (user != null)
            {
                i18n.send(ACTION_BAR, user, POSITIVE, "Removed Lock!");
            }
            return;
        }
        i18n.send(ACTION_BAR, user, NEGATIVE, "This protection is not yours!");
    }

    /**
     * Creates a new Lock at given Location
     *
     * @param block the location to create the lock for
     * @param player the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public CompletableFuture<Lock> createLock(Location<World> block, Player player, LockType lockType, String password, boolean createKeyBook)
    {
        if (getLock(block) != null)
        {
            i18n.send(ACTION_BAR, player, NEUTRAL, "There is already protection here!");
            return null;
        }

        BlockType material = block.getBlockType();
        LockModel model = database.getDSL().newRecord(TABLE_LOCKS).newLock(player, lockType, getProtectedType(material));
        for (BlockLockConfig blockProtection : this.module.getConfig().blockprotections)
        {
            if (blockProtection.isType(material))
            {
                short flags = blockProtection.getFlags();
                if (flags != 0)
                {
                    model.setValue(TABLE_LOCKS.FLAGS, (short)(model.getValue(TABLE_LOCKS.FLAGS) | flags));
                }
                break;
            }
        }

        List<Location<World>> locations = new ArrayList<>();
        // Handle MultiBlock Protections
        if (material == CHEST || material == TRAPPED_CHEST)
        {
            locations.add(block);
            for (Direction direction : CARDINAL_DIRECTIONS)
            {
                Location<World> relative = block.getRelative(direction);
                if (relative.getBlockType() == material)
                {
                    locations.add(relative);
                }
            }
        }
        else if (material == WOODEN_DOOR
            || material == IRON_DOOR
            || material == ACACIA_DOOR
            || material == BIRCH_DOOR
            || material == DARK_OAK_DOOR
            || material == JUNGLE_DOOR
            || material == SPRUCE_DOOR)
        {
            locations.add(block); // Original Block
            // Find upper/lower door part

            Optional<? extends Enum<?>> halfTrait = block.getBlock().getTraitValue(EnumTraits.WOODEN_DOOR_HALF);
            boolean upperHalf = halfTrait.isPresent() && halfTrait.get().name().equals("UPPER");

            // if (placed.get(Keys.PORTION_TYPE).get().equals(TOP)) // TODO use once implemented

            Location<World> relative = block.getRelative(upperHalf ? Direction.DOWN : Direction.UP);
            if (relative.getBlockType() != material) // try other half?
            {
                relative = block.getRelative(Direction.UP);
            }
            if (relative.getBlockType() != material)
            {
                throw new IllegalStateException("Other door half is missing");
            }

            if (relative.getBlockType() == material)
            {
                locations.add(relative);

                Direction direction = block.get(Keys.DIRECTION).get();
                Hinge hinge = block.get(Keys.HINGE_POSITION).get();
                Direction otherDoor = BlockUtil.getOtherDoorDirection(direction, hinge);
                Location<World> blockOther = block.getRelative(otherDoor);
                Location<World> relativeOther = relative.getRelative(otherDoor);
                if (blockOther.getBlockType() == material && blockOther.getBlockType().equals(relativeOther.getBlockType()) && // Same DoorMaterial
                    blockOther.get(Keys.DIRECTION).get() == direction && blockOther.get(Keys.HINGE_POSITION).get() != hinge) // Door-Pair
                {
                    locations.add(blockOther);
                    locations.add(relativeOther);
                }
            }
        }
        if (locations.isEmpty())
        {
            locations.add(block);
        }

        return insertLock(player, password, createKeyBook, model, locations);
    }

    private CompletableFuture<Lock> insertLock(Player user, String password, boolean createKeyBook, LockModel model, List<Location<World>> locations)
    {
        model.createPassword(this, password);
        CompletableFuture<Integer> future = model.insertAsync();
        future.exceptionally(throwable -> {
            throwable.printStackTrace();
            return 0;
        });

        return future.thenCompose(m -> insertLockLocs(user, createKeyBook, model, locations));
    }

    private CompletableFuture<Lock> insertLockLocs(Player user, boolean createKeyBook, LockModel model, List<Location<World>> locations)
    {
        return allOf(locations.parallelStream()
                              .map(loc -> database.getDSL().newRecord(TABLE_LOCK_LOCATIONS).newLocation(model, loc))
                              .map(AsyncRecord::insertAsync).toArray(CompletableFuture[]::new))
              .thenApply((v) -> {
            Lock lock = new Lock(this, model, i18n, locations);
            this.addLoadedLocationLock(lock);
            lock.showCreatedMessage(user);
            lock.attemptCreatingKeyBook(user, createKeyBook);
            return lock;
        });
    }

    /**
     * Creates a new Lock for given Entity
     *
     * @param entity the entity to protect
     * @param user user the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public CompletableFuture<Lock> createLock(Entity entity, Player user, LockType lockType, String password, boolean createKeyBook)
    {

        LockModel model = database.getDSL().newRecord(TABLE_LOCKS).newLock(user, lockType, getProtectedType(entity.getType()), entity.getUniqueId());
        model.createPassword(this, password);
        return model.insertAsync().thenApply(m -> {
            Lock lock = new Lock(this, model, i18n);
            this.loadedEntityLocks.put(entity.getUniqueId(), lock);
            this.locksById.put(lock.getId(), lock);
            lock.showCreatedMessage(user);
            lock.attemptCreatingKeyBook(user, createKeyBook);
            for (EntityLockConfig entityProtection : this.module.getConfig().entityProtections)
            {
                if (entityProtection.isType(entity.getType()))
                {
                    short flags = entityProtection.getFlags();
                    if (flags != 0)
                    {
                        lock.setFlags((short)(lock.getFlags() | flags));
                        lock.model.updateAsync();
                    }
                    break;
                }
            }
            return lock;
        });
    }

    public boolean canProtect(BlockType type)
    {
        for (BlockLockConfig blockprotection : this.module.getConfig().blockprotections)
        {
            if (blockprotection.isType(type))
            {
                return true;
            }
        }
        return false;
    }

    public boolean canProtect(EntityType type)
    {
        for (EntityLockConfig entityProtection : this.module.getConfig().entityProtections)
        {
            if (entityProtection.isType(type))
            {
                return true;
            }
        }
        return false;
    }

    public void saveAll()
    {
        List<LockModel> collect = Stream.concat(loadedEntityLocks.values().stream(),
                loadedLocks.values().stream().flatMap(longLockMap -> longLockMap.values().stream()))
                .map(l -> l.model)
                .filter(m -> m.changed())
                .collect(toList());

        Batch batch = database.getDSL().batchStore(collect);

        batch.execute(); // TODO async me
    }

    /**
     * Returns the lock for given inventory it exists
     *
     * @param inventory
     * @return the lock for given inventory
     */
    public Lock getLockOfInventory(CarriedInventory<?> inventory)
    {
        Carrier holder = inventory.getCarrier().orElse(null);
        if (holder instanceof Entity)
        {
            return this.getLockForEntityUID(((Entity)holder).getUniqueId());
        }
        if (holder instanceof TileEntityCarrier)
        {
            return getValidLock(((TileEntityCarrier)holder).getLocation(), null);
        }
        return null;
    }

    /**
     * The returned Lock should not be saved for later use!
     *
     * @param lockID the locks id
     * @return a copy of the Lock with given id
     */
    public Lock getLockById(UInteger lockID)
    {
        Lock lock = this.locksById.get(lockID);
        if (lock != null)
        {
            return lock;
        }
        LockModel lockModel = database.getDSL().selectFrom(TABLE_LOCKS).where(TABLE_LOCKS.ID.eq(lockID)).fetchOne();
        if (lockModel != null)
        {
            Result<LockLocationModel> fetch = database.getDSL().selectFrom(TABLE_LOCK_LOCATIONS)
                                                      .where(TABLE_LOCK_LOCATIONS.LOCK_ID.eq(lockModel.getValue(TABLE_LOCKS.ID)))
                                                      .fetch();
            if (fetch.isEmpty())
            {
                return new Lock(this, lockModel, i18n);
            }
            return new Lock(this, lockModel, i18n, fetch);
        }
        return null;
    }

    public void setGlobalAccess(Player sender, List<PlayerAccess> list)
    {
        for (PlayerAccess access : list)
        {
            short accessType = ACCESS_FULL;
            if (access.add && access.admin)
            {
                accessType = ACCESS_ALL; // with AdminAccess
            }
            AccessListModel accessListModel = database.getDSL().selectFrom(TABLE_ACCESSLIST).where(
                    TABLE_ACCESSLIST.USER_ID.eq(access.user.getUniqueId()),
                    TABLE_ACCESSLIST.OWNER_ID.eq(sender.getUniqueId())).fetchOne();
            if (access.add)
            {
                if (accessListModel == null)
                {
                    accessListModel = database.getDSL().newRecord(TABLE_ACCESSLIST).newGlobalAccess(sender, access.user, accessType);
                    accessListModel.insertAsync();
                    i18n.send(ACTION_BAR, sender, POSITIVE, "Global access for {user} set!", access.user);
                }
                else
                {
                    accessListModel.setValue(TABLE_ACCESSLIST.LEVEL, accessType);
                    accessListModel.updateAsync();
                    i18n.send(ACTION_BAR, sender, POSITIVE, "Updated global access level for {user}!", access.user);
                }
            }
            else if (accessListModel == null)
            {
                i18n.send(ACTION_BAR, sender, NEUTRAL, "{user} had no global access!", access.user);
            }
            else
            {
                accessListModel.deleteAsync();
                i18n.send(ACTION_BAR, sender, POSITIVE, "Removed global access from {user}", access.user);
            }

        }
    }

    public void reloadLocks()
    {
        blockAsync = true;

        loadedLocks.clear();
        loadedLocksInChunk.clear();
        loadedEntityLocks.clear();
        locksById.clear();

        unlocked.clear();
        loadChunks.clear();
        unloadChunks.clear();

        loadedChunks.clear();

        for (World world : Sponge.getServer().getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                this.queueChunk(chunk);
            }
        }

        blockAsync = false;
        this.loadLocksInChunks();
        logger.info("Finished loading locks");
    }

    public CompletableFuture<Integer> purgeLocksFrom(User user)
    {
        logger.info("Purging Locks from {}", user.getName());
        CompletableFuture<Integer> future = database.execute(database.getDSL().delete(TABLE_LOCKS)
            .where(TABLE_LOCKS.OWNER_ID.eq(user.getUniqueId())));
        future.thenAccept(integer -> {
            if (integer != 0)
            {
                logger.info("{} Locks purged", integer);
                reloadLocks();
            }
        });
        return future;
    }

    public CompletableFuture<Integer> purgeOldLocks()
    {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis() - module.getConfig().cleanupAge * 24 * 60 * 60 * 1000);
        logger.info("Purging old Locks from {}", new Date(timestamp.getTime()));
        CompletableFuture<Integer> future = database.execute(database.getDSL().delete(TABLE_LOCKS)
            .where(TABLE_LOCKS.LAST_ACCESS.lessThan(timestamp)));
        future.thenAccept(i -> {
            if (i != 0)
            {
                logger.info("{} Locks purged", i);
                reloadLocks();
            }
        });
        return future;
    }

    public Database getDB()
    {
        return database;
    }

    public boolean hasUnlocked(Player user, Lock lock)
    {
        return getUnlocks(user).contains(lock.getId());
    }

    private Set<UInteger> getUnlocks(Player user)
    {
        Set<UInteger> unlocks = unlocked.get(user.getUniqueId());
        if (unlocks == null)
        {
            unlocks = new HashSet<>();
            unlocked.put(user.getUniqueId(), unlocks);
        }
        return unlocks;
    }

    public void addUnlock(Player user, Lock lock)
    {
        getUnlocks(user).add(lock.getId());
    }
}
