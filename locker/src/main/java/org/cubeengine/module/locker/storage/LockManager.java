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
package org.cubeengine.module.locker.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.core.util.matcher.StringMatcher;
import org.cubeengine.module.locker.config.BlockLockConfig;
import org.cubeengine.module.locker.config.EntityLockConfig;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.commands.CommandListener;
import org.cubeengine.module.locker.commands.PlayerAccess;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.CachedUser;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.jooq.Batch;
import org.jooq.Condition;
import org.jooq.Result;
import org.jooq.types.UInteger;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.data.type.PortionType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.core.util.BlockUtil.CARDINAL_DIRECTIONS;
import static org.cubeengine.module.core.util.LocationUtil.getChunkKey;
import static org.cubeengine.module.core.util.LocationUtil.getLocationKey;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_ALL;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_FULL;
import static org.cubeengine.module.locker.storage.ProtectedType.getProtectedType;
import static org.cubeengine.module.locker.storage.TableAccessList.TABLE_ACCESS_LIST;
import static org.cubeengine.module.locker.storage.TableLockLocations.TABLE_LOCK_LOCATION;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCK;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.*;
import static org.spongepowered.api.data.type.PortionTypes.BOTTOM;
import static org.spongepowered.api.data.type.PortionTypes.TOP;

public class LockManager
{
    public static final int VIEWDISTANCE_DEFAULT = 10;
    protected final Locker module;
    private Database database;

    protected WorldManager wm;
    protected UserManager um;
    protected TaskManager tm;
    private I18n i18n;
    private org.spongepowered.api.Game game;
    private final StringMatcher stringMatcher;
    protected Log logger;
    public final CommandListener commandListener;

    private final Map<UInteger, Map<Long, Lock>> loadedLocks = new HashMap<>(); // World -> LocationKey -> Lock
    private final Map<UInteger, Map<Long, Set<Lock>>> loadedLocksInChunk = new HashMap<>(); // World -> ChunkKey -> Lock
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

    public LockManager(Locker module, EventManager em, StringMatcher stringMatcher, Database database, WorldManager wm, UserManager um, TaskManager tm, I18n i18n, org.spongepowered.api.Game game)
    {
        this.stringMatcher = stringMatcher;
        this.database = database;
        this.wm = wm;
        this.um = um;
        this.tm = tm;
        this.i18n = i18n;
        this.game = game;
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
        em.registerListener(module, this.commandListener);
        em.registerListener(module, this);

        onEnable();
    }

    private void onEnable()
    {
        reloadLocks();

        tm.runTimer(module, this::doLoadChunks, 5, 5); // 5 Ticks
        tm.runTimer(module, this::doUnloadChunks, 100, 100); // 100 Ticks - 5 seconds
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
        UInteger world_id = this.wm.getWorldId(chunk.getWorld());

        // TODO get view-distance if available / default to 10
        int viewDistance = 10;

        Vector3i position = chunk.getPosition();
        int chunkX = position.getX();
        int chunkZ = position.getZ();

        List<Integer> xPos = IntStream.range(chunkX - viewDistance, chunkX + viewDistance).mapToObj(Integer.class::cast).collect(toList());
        List<Integer> zPos = IntStream.range(chunkZ - viewDistance, chunkZ + viewDistance).mapToObj(Integer.class::cast).collect(toList());

        Condition condChunkX = TABLE_LOCK_LOCATION.CHUNKX.in(xPos);
        Condition condChunkZ = TABLE_LOCK_LOCATION.CHUNKZ.in(zPos);
        Condition condNotLoaded = TABLE_LOCK_LOCATION.LOCK_ID.notIn(locksById.keySet());

        Result<LockModel> models = this.database.getDSL().selectFrom(TABLE_LOCK).where(TABLE_LOCK.ID.in(
                this.database.getDSL().select(TABLE_LOCK_LOCATION.LOCK_ID).from(
                        TABLE_LOCK_LOCATION).where(TABLE_LOCK_LOCATION.WORLD_ID.eq(world_id), condChunkX, condChunkZ, condNotLoaded))).fetch();
        Map<UInteger, Result<LockLocationModel>> locations = LockManager.
            this.database.getDSL().selectFrom(TABLE_LOCK_LOCATION).where(
            TABLE_LOCK_LOCATION.LOCK_ID.in(
            models.getValues(TABLE_LOCK.ID))).fetch().intoGroups(TABLE_LOCK_LOCATION.LOCK_ID);
        for (LockModel model : models)
        {
            Result<LockLocationModel> lockLoc = locations.get(model.getValue(TABLE_LOCK.ID));
            Lock lock = new Lock(this, model, i18n, lockLoc, game);
            addLoadedLocationLock(lock);
            System.out.print("Lock loaded at: " + lock.getFirstLocation().getPosition() + "\n");
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
        UInteger worldId = null;
        for (Location loc : lock.getLocations())
        {
            if (worldId == null)
            {
                worldId = wm.getWorldId((World)loc.getExtent());
            }
            Map<Long, Set<Lock>> locksInChunkMap = this.getChunkLocksMap(worldId);
            long chunkKey = getChunkKey(loc.getBlockX() / viewDistance, loc.getBlockZ() / viewDistance);
            Set<Lock> locks = locksInChunkMap.get(chunkKey);
            if (locks == null)
            {
                locks = new HashSet<>();
                locksInChunkMap.put(chunkKey, locks);
            }
            locks.add(lock);
            this.getLocLockMap(worldId).put(getLocationKey(loc), lock);
        }
    }

    private Map<Long, Set<Lock>> getChunkLocksMap(UInteger worldId)
    {
        Map<Long, Set<Lock>> locksInChunkMap = this.loadedLocksInChunk.get(worldId);
        if (locksInChunkMap == null)
        {
            locksInChunkMap = new HashMap<>();
            this.loadedLocksInChunk.put(worldId, locksInChunkMap);
        }
        return locksInChunkMap;
    }

    private Map<Long, Lock> getLocLockMap(UInteger worldId)
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

            UInteger worldId = wm.getWorldId(chunk.getWorld());
            Set<Lock> removed = this.getChunkLocksMap(worldId).remove(getChunkKey(x, z));
            if (removed == null) return; // nothing to remove
            Map<Long, Lock> locLockMap = this.getLocLockMap(worldId);
            for (Lock lock : removed) // remove from chunks
            {
                System.out.print("Lock unloaded at: " + lock.getFirstLocation().getPosition() + "\n");
                this.locksById.remove(lock.getId());
                for (Location loc : lock.getLocations())
                {
                    locLockMap.remove(getLocationKey(loc));
                }
                lock.model.updateAsync(); // updates if changed (last_access timestamp)
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
    public Lock getLockAtLocation(Location<World> location, Player user)
    {
        UInteger worldId = wm.getWorldId(location.getExtent());
        Lock lock = this.getLocLockMap(worldId).get(getLocationKey(location));
        // TODO repairing Locks still needed?
        /*
        if (repairExpand && lock != null && lock.isSingleBlockLock())
        {
            Location block = lock.getFirstLocation();
            if (block.getBlockType() == CHEST || block.getBlockType() == TRAPPED_CHEST)
            {
                for (Direction cardinalDirection : CARDINAL_DIRECTIONS)
                {
                    Location relative = block.getRelative(cardinalDirection);
                    if (relative.getBlockType() == block.getBlockType())
                    {
                        if (this.getLockAtLocation(relative, null) == null)
                        {
                            this.extendLock(lock, relative);
                            if (user != null)
                            {
                                i18n.sendTranslated(user, POSITIVE, "Protection repaired & expanded!");
                            }
                        }
                        else
                        {
                            if (user != null)
                            {
                                i18n.sendTranslated(user, CRITICAL,
                                                    "Broken protection detected! Try /cremove on nearby blocks!");
                                i18n.sendTranslated(user, NEUTRAL,
                                                    "If this message keeps coming please contact an administrator!");
                            }
                        }
                        break;
                    }
                }
            }
        }
        */
        if (lock == null)
        {
            return null;
        }
        if (!lock.validateTypeAt(location)) // TODO don't validate each time maybe?
        {
            lock.delete(user);
            if (user != null)
            {
                i18n.sendTranslated(user, NEUTRAL, "Deleted invalid BlockProtection!");
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
            LockModel model = database.getDSL().selectFrom(TABLE_LOCK).where(TABLE_LOCK.ENTITY_UID_LEAST.eq(uniqueId.getLeastSignificantBits()),
                                                                      TABLE_LOCK.ENTITY_UID_MOST.eq(uniqueId.getMostSignificantBits())).fetchOne();
            if (model != null)
            {
                lock = new Lock(this, model, i18n, game);
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
        if (this.getLockAtLocation(location, null) != null)
        {
            throw new IllegalStateException("Cannot extend Lock onto another!");
        }
        lock.locations.add(location);
        LockLocationModel model = database.getDSL().newRecord(TABLE_LOCK_LOCATION).newLocation(lock.model, location, wm);
        model.insertAsync();
        UInteger worldId = wm.getWorldId(location.getExtent());
        this.getLocLockMap(worldId).put(getLocationKey(location), lock);
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
                    UInteger worldId = wm.getWorldId(((World)location.getExtent()));
                    this.getLocLockMap(worldId).remove(getLocationKey(location));
                    Set<Lock> locks = this.getChunkLocksMap(worldId).get(chunkKey);
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
                i18n.sendTranslated(user, POSITIVE, "Removed Lock!");
            }
            return;
        }
        i18n.sendTranslated(user, NEGATIVE, "This protection is not yours!");
    }

    /**
     * Creates a new Lock at given Location
     *
     * @param block the location to create the lock for
     * @param user the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public CompletableFuture<Lock> createLock(Location<World> block, Player user, LockType lockType, String password, boolean createKeyBook)
    {
        BlockType material = block.getBlockType();
        LockModel model = database.getDSL().newRecord(TABLE_LOCK).newLock(module.getUserManager().getByUUID(user.getUniqueId()), lockType, getProtectedType(material));
        for (BlockLockConfig blockProtection : this.module.getConfig().blockprotections)
        {
            if (blockProtection.isType(material))
            {
                short flags = blockProtection.getFlags();
                if (flags != 0)
                {
                    model.setValue(TABLE_LOCK.FLAGS, (short)(model.getValue(TABLE_LOCK.FLAGS) | flags));
                }
                break;
            }
        }

        List<Location<World>> locations = new ArrayList<>();
        // Handle MultiBlock Protections
        if (material == CHEST || material == TRAPPED_CHEST)
        {
            for (Direction direction : CARDINAL_DIRECTIONS)
            {
                Location relative = block.getRelative(direction);
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
            PortionType portion = block.get(Keys.PORTION_TYPE).orElse(null);
            Location<World> relative = null;
            if (portion == BOTTOM)
            {
                relative = block.getRelative(Direction.UP);
            }
            else if (portion == TOP)
            {
                relative = block.getRelative(Direction.DOWN);
            }
            else // TODO PortionType is not working!?
            {
                relative = block.getRelative(Direction.DOWN);
                if (relative.getBlockType() != block.getBlockType())
                {
                    relative = block.getRelative(Direction.UP);
                    if (relative.getBlockType() != block.getBlockType())
                    {
                        throw new IllegalStateException("Other door half is missing");
                    }
                }
            }
            if (relative != null && relative.getBlockType() == material)
            {
                locations.add(relative);

                Direction direction = block.get(Keys.DIRECTION).get();
                Hinge hinge = block.get(Keys.HINGE_POSITION).get();
                direction = BlockUtil.getOtherDoorDirection(direction, hinge);
                Location<World> blockOther = block.getRelative(direction);
                Location<World> relativeOther = relative.getRelative(direction);
                if (portion != null && portion.equals(block.get(Keys.PORTION_TYPE).orElse(null)) // TODO null portion
                    && blockOther.getBlockType().equals(relativeOther.getBlockType()))
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

        return model.createPassword(this, password).insertAsync()
                    .thenCompose(m -> allOf(locations.parallelStream().map(loc -> database.getDSL().newRecord(
                        TABLE_LOCK_LOCATION).newLocation(model, loc, wm)).map(AsyncRecord::insertAsync).toArray(
                        CompletableFuture[]::new)).thenApply((v) -> {
                        Lock lock = new Lock(this, model, i18n, locations, game);
                        this.addLoadedLocationLock(lock);
                        lock.showCreatedMessage(user);
                        lock.attemptCreatingKeyBook(user, createKeyBook);
                        return lock;
                    }));
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

        LockModel model = database.getDSL().newRecord(TABLE_LOCK).newLock(module.getUserManager().getByUUID(user.getUniqueId()), lockType, getProtectedType(entity.getType()), entity.getUniqueId());
        model.createPassword(this, password);
        return model.insertAsync().thenApply(m -> {
            Lock lock = new Lock(this, model, i18n, game);
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
            return getLockAtLocation(((TileEntityCarrier)holder).getLocation(), null);
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
        LockModel lockModel = database.getDSL().selectFrom(TABLE_LOCK).where(TABLE_LOCK.ID.eq(lockID)).fetchOne();
        if (lockModel != null)
        {
            Result<LockLocationModel> fetch = database.getDSL().selectFrom(TABLE_LOCK_LOCATION)
                                                      .where(TABLE_LOCK_LOCATION.LOCK_ID.eq(lockModel.getValue(TABLE_LOCK.ID)))
                                                      .fetch();
            if (fetch.isEmpty())
            {
                return new Lock(this, lockModel, i18n, game);
            }
            return new Lock(this, lockModel, i18n, fetch, game);
        }
        return null;
    }

    public void setGlobalAccess(Player sender, List<PlayerAccess> list)
    {
        CachedUser senderUser = um.getByUUID(sender.getUniqueId());
        for (PlayerAccess access : list)
        {
            CachedUser accessUser = um.getByUUID(access.user.getUniqueId());

            short accessType = ACCESS_FULL;
            if (access.add && access.admin)
            {
                accessType = ACCESS_ALL; // with AdminAccess
            }
            AccessListModel accessListModel = database.getDSL().selectFrom(TABLE_ACCESS_LIST).where(
                    TABLE_ACCESS_LIST.USER_ID.eq(accessUser.getEntity().getId()),
                    TABLE_ACCESS_LIST.OWNER_ID.eq(senderUser.getEntity().getId())).fetchOne();
            if (access.add)
            {
                if (accessListModel == null)
                {
                    accessListModel = database.getDSL().newRecord(TABLE_ACCESS_LIST).newGlobalAccess(senderUser, accessUser, accessType);
                    accessListModel.insertAsync();
                    i18n.sendTranslated(sender, POSITIVE, "Global access for {user} set!", access.user);
                }
                else
                {
                    accessListModel.setValue(TABLE_ACCESS_LIST.LEVEL, accessType);
                    accessListModel.updateAsync();
                    i18n.sendTranslated(sender, POSITIVE, "Updated global access level for {user}!", access.user);
                }
            }
            else if (accessListModel == null)
            {
                i18n.sendTranslated(sender, NEUTRAL, "{user} had no global access!", access.user);
            }
            else
            {
                accessListModel.deleteAsync();
                i18n.sendTranslated(sender, POSITIVE, "Removed global access from {user}", access.user);
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

        for (World world : game.getServer().getWorlds())
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
        CompletableFuture<Integer> future = database.execute(database.getDSL().delete(TABLE_LOCK).where(TABLE_LOCK.OWNER_ID.eq(um.getByUUID(user.getUniqueId()).getEntityId())));
        future.thenAccept(integer -> {
            if (integer != 0)
            {
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
