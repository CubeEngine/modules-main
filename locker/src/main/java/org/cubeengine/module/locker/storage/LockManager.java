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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;
import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.module.core.util.BlockUtil;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.core.util.matcher.StringMatcher;
import org.cubeengine.module.locker.BlockLockerConfiguration;
import org.cubeengine.module.locker.EntityLockerConfiguration;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.commands.CommandListener;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.CachedUser;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.service.world.WorldManager;
import org.jooq.Condition;
import org.jooq.Result;
import org.jooq.types.UInteger;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.data.type.PortionType;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
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
    private Game game;
    private final StringMatcher stringMatcher;
    protected Log logger;

    public final CommandListener commandListener;

    private final Map<UInteger, Map<Long, Lock>> loadedLocks = new HashMap<>();
    private final Map<UInteger, Map<Long, Set<Lock>>> loadedLocksInChunk = new HashMap<>();
    private final Map<UUID, Lock> loadedEntityLocks = new HashMap<>();
    private final Map<Long, Lock> locksById = new HashMap<>();

    private final Map<UUID, Set<Long>> unlocked = new HashMap<>();

    public final MessageDigest messageDigest;

    private final Queue<Chunk> queuedChunks = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;
    private Future<?> future = null;

    public LockManager(Locker module, EventManager em, StringMatcher stringMatcher, Database database, WorldManager wm, UserManager um, TaskManager tm, I18n i18n, Game game)
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
        executor = Executors.newSingleThreadExecutor(module.getProvided(ThreadFactory.class));
        try
        {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 hash algorithm not available!");
        }
        this.commandListener = new CommandListener(module, this, um, logger, this.stringMatcher);
        em.registerListener(module, this.commandListener);
        em.registerListener(module, this);
    }


    @Enable
    public void onEnable()
    {
        for (World world : wm.getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                this.queueChunk(chunk);
            }
        }
        this.loadLocksInChunks();
        logger.info("Finished loading locks");

        tm.runTimer(module, () -> {
            if (!queuedChunks.isEmpty() && (future == null || future.isDone()))
            {
                future = executor.submit(this::loadLocksInChunks);
            }
        }, 5, 5);
    }

    @Listener
    private void onChunkLoad(LoadChunkEvent event)
    {
        queueChunk(event.getTargetChunk());
    }

    private boolean queueChunk(Chunk chunk)
    {
        return this.queuedChunks.add(chunk);
    }

    private void loadLocksInChunks()
    {
        if (queuedChunks.isEmpty())
        {
            return;
        }
        Chunk chunk = queuedChunks.poll();
        UInteger world_id = this.wm.getWorldId(chunk.getWorld());

        Vector3i chunkSize = game.getServer().getChunkLayout().getChunkSize();

        // TODO get view-distance if available / default to 10
        int viewDistance = 10;

        Vector3i position = chunk.getPosition(
                                             );
        int chunkX = position.getX() / chunkSize.getX();
        int chunkZ = position.getZ() / chunkSize.getZ();

        Condition condChunkX = TABLE_LOCK_LOCATION.CHUNKX.in(IntStream.range(chunkX, chunkX + viewDistance).mapToObj(Integer.class::cast).collect(toList()));
        Condition condChunkZ = TABLE_LOCK_LOCATION.CHUNKZ.in(IntStream.range(chunkZ, chunkZ + viewDistance).mapToObj(Integer.class::cast).collect(toList()));

        Result<LockModel> models = this.database.getDSL().selectFrom(TABLE_LOCK).where(TABLE_LOCK.ID.in(
            this.database.getDSL().select(TABLE_LOCK_LOCATION.LOCK_ID).from(
                TABLE_LOCK_LOCATION).where(
                TABLE_LOCK_LOCATION.WORLD_ID.eq(world_id),
                condChunkX, condChunkZ))).fetch();
        Map<UInteger, Result<LockLocationModel>> locations = LockManager.
            this.database.getDSL().selectFrom(TABLE_LOCK_LOCATION).where(
            TABLE_LOCK_LOCATION.LOCK_ID.in(
            models.getValues(TABLE_LOCK.ID))).fetch().intoGroups(TABLE_LOCK_LOCATION.LOCK_ID);
        for (LockModel model : models)
        {
            Result<LockLocationModel> lockLoc = locations.get(model.getValue(TABLE_LOCK.ID));
            addLoadedLocationLock(new Lock(this, model, i18n, lockLoc, game));
        }
        if (!queuedChunks.isEmpty())
        {
            future = executor.submit(this::loadLocksInChunks);
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

    @Listener
    private void onChunkUnload(UnloadChunkEvent event)
    {
        Chunk chunk = event.getTargetChunk();
        queuedChunks.remove(chunk);

        Vector3i chunkSize = game.getServer().getChunkLayout().getChunkSize();

        // TODO get view-distance if available / default to 10
        int viewDistance = VIEWDISTANCE_DEFAULT;

        Vector3i position = chunk.getPosition();
        int chunkX = position.getX() / chunkSize.getX();
        int chunkZ = position.getZ() / chunkSize.getZ();

        IntStream xCoords = IntStream.range(chunkX, chunkX + viewDistance).map(x -> x * 16);
        IntStream zCoords = IntStream.range(chunkZ, chunkZ + viewDistance).map(z -> z * 16);

        boolean allChunksUnloaded = xCoords.mapToObj(Integer.class::cast)
               .flatMap(x -> zCoords.mapToObj(z -> chunk.getWorld().getChunk(x, 0, z).isPresent()))
               .noneMatch(e -> e);

        if (!allChunksUnloaded)
        {
            return;
        }

        UInteger worldId = wm.getWorldId(chunk.getWorld());
        Set<Lock> remove = this.getChunkLocksMap(worldId).remove(getChunkKey(chunkX / viewDistance,
                                                                             chunkZ / viewDistance));
        if (remove == null) return; // nothing to remove
        Map<Long, Lock> locLockMap = this.getLocLockMap(worldId);
        for (Lock lock : remove) // remove from chunks
        {
            Location firstLoc = lock.getFirstLocation();
            this.locksById.remove(lock.getId());
            Chunk c1 = ((World)firstLoc.getExtent()).getChunk(firstLoc.getBlockPosition()).get();
            for (Location location : lock.getLocations())
            {
                Chunk c2 = ((World)location.getExtent()).getChunk(location.getBlockPosition()).get();
                if (c2 != c1) // different chunks
                {
                    if ((!c1.isLoaded() && c2 == chunk)
                        ||(!c2.isLoaded() && c1 == chunk))
                    {
                        // Both chunks will be unloaded remove both loc
                        for (Location loc : lock.getLocations())
                        {
                            locLockMap.remove(getLocationKey(loc));
                        }
                        lock.model.updateAsync();
                    }
                    // else the other chunk is still loaded -> do not remove!
                    return;
                }
            }
            for (Location loc : lock.getLocations())
            {
                locLockMap.remove(getLocationKey(loc));
            }
            lock.model.updateAsync(); // updates if changed (last_access timestamp)
        }
    }

    /**
     * Returns the Lock at given location if the lock there is active
     *
     * @param location the location of the lock
     * @param user the user to get the lock for (can be null)
     * @return the lock or null if there is no lock OR the chunk is not loaded OR the lock is disabled
     */
    public Lock getLockAtLocation(Location<World> location, Player user)
    {
        return getLockAtLocation(location, user, true);
    }

    /**
     * Returns the Lock at given Location
     *
     * @param location the location of the lock
     * @param access whether to access the lock or just get information from it
     * @return the lock or null if there is no lock OR the chunk is not loaded
     */
    public Lock getLockAtLocation(Location<World> location, Player user, boolean access, boolean repairExpand)
    {
        UInteger worldId = wm.getWorldId(location.getExtent());
        Lock lock = this.getLocLockMap(worldId).get(getLocationKey(location));
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
                        if (this.getLockAtLocation(relative,null, false,false)== null)
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
        if (lock != null && access)
        {
            if (!lock.validateTypeAt(location))
            {
                lock.delete(user);
                if (user != null)
                {
                    i18n.sendTranslated(user, NEUTRAL, "Deleted invalid BlockProtection!");
                }
            }
            return this.handleLockAccess(lock, access);
        }
        return lock;
    }

    public Lock getLockAtLocation(Location location, Player user, boolean access)
    {
        return this.getLockAtLocation(location, user, access, true);
    }

    /**
     * Returns the Lock for given entityUID
     *
     * @param uniqueId the entities unique id
     * @return the entity-lock or null if there is no lock OR the lock is disabled
     */
    public Lock getLockForEntityUID(UUID uniqueId)
    {
        return this.getLockForEntityUID(uniqueId, true);
    }

    /**
     * Returns the Lock for given entityUID
     *
     * @param uniqueId the entities unique id
     * @param access whether to access the lock or just get information from it
     * @return the entity-lock or null if there is no lock
     */
    public Lock getLockForEntityUID(UUID uniqueId, boolean access)
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
        return this.handleLockAccess(lock, access);
    }

    private Lock handleLockAccess(Lock lock, boolean access)
    {
        if (lock != null && access)
        {
            if ((this.module.getConfig().protectWhenOnlyOffline && lock.getOwner().isOnline())
            || (this.module.getConfig().protectWhenOnlyOnline && !lock.getOwner().isOnline()))
            {
                return null;
            }
            lock.model.setValue(TABLE_LOCK.LAST_ACCESS, new Timestamp(System.currentTimeMillis()));
        }
        return lock;
    }

    /**
     * Extends a location lock onto an other location
     *
     * @param lock the lock to extend
     * @param location the location to extend to
     */
    public void extendLock(Lock lock, Location<World> location)
    {
        if (this.getLockAtLocation(location, null, false, false) != null)
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
     * @param material the material at given location (can missmatch if block is just getting placed)
     * @param block the location to create the lock for
     * @param user the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public CompletableFuture<Lock> createLock(BlockType material, Location<World> block, Player user, LockType lockType, String password, boolean createKeyBook)
    {

        LockModel model = database.getDSL().newRecord(TABLE_LOCK).newLock(module.getUserManager().getByUUID(user.getUniqueId()), lockType, getProtectedType(material));
        for (BlockLockerConfiguration blockProtection : this.module.getConfig().blockprotections)
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
            PortionType portion = block.get(Keys.PORTION_TYPE).get();
            Location<World> relative = null;
            if (portion == BOTTOM)
            {
                relative = block.getRelative(Direction.UP);
            }
            else if (portion == TOP)
            {
                relative = block.getRelative(Direction.DOWN);
            }
            if (relative != null && relative.getBlockType() == material)
            {
                locations.add(relative);

                Direction direction = block.get(Keys.DIRECTION).get();
                Hinge hinge = block.get(Keys.HINGE_POSITION).get();
                direction = BlockUtil.getOtherDoorDirection(direction, hinge);
                Location<World> blockOther = block.getRelative(direction);
                Location<World> relativeOther = relative.getRelative(direction);
                if (portion.equals(block.get(Keys.PORTION_TYPE).orElse(null))
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
            for (EntityLockerConfiguration entityProtection : this.module.getConfig().entityProtections)
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
        for (BlockLockerConfiguration blockprotection : this.module.getConfig().blockprotections)
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
        for (EntityLockerConfiguration entityProtection : this.module.getConfig().entityProtections)
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
        for (Lock lock : this.loadedEntityLocks.values())
        {
            lock.model.updateAsync();
        }
        for (Map<Long, Lock> lockMap : this.loadedLocks.values())
        {
            for (Lock lock : lockMap.values())
            {
                lock.model.updateAsync();
            }
        }
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
    public Lock getLockById(long lockID)
    {
        Lock lock = this.locksById.get(lockID);
        if (lock != null)
        {
            return lock;
        }
        LockModel lockModel = database.getDSL().selectFrom(TABLE_LOCK).where(TABLE_LOCK.ID.eq(UInteger.valueOf(lockID))).fetchOne();
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

    public void setGlobalAccess(Player sender, String string)
    {
        String[] explode = StringUtils.explode(",", string);
        for (String name : explode)
        {
            boolean add = true;
            boolean admin = false;
            if (name.startsWith("@"))
            {
                name = name.substring(1);
                admin = true;
            }
            if (name.startsWith("-"))
            {
                name = name.substring(1);
                add = false;
            }
            CachedUser modifyUser = um.getByUUID(um.getByName(name).get().getUniqueId());
            CachedUser senderUser = um.getByUUID(sender.getUniqueId());
            if (modifyUser == null) throw new IllegalArgumentException(); // This is prevented by checking first in the cmd execution
            short accessType = ACCESS_FULL;
            if (add && admin)
            {
                accessType = ACCESS_ALL; // with AdminAccess
            }
            AccessListModel accessListModel = database.getDSL().selectFrom(TABLE_ACCESS_LIST).where(
                TABLE_ACCESS_LIST.USER_ID.eq(modifyUser.getEntity().getId()),
                TABLE_ACCESS_LIST.OWNER_ID.eq(senderUser.getEntity().getId())).fetchOne();
            if (add)
            {
                if (accessListModel == null)
                {
                    accessListModel = database.getDSL().newRecord(TABLE_ACCESS_LIST).newGlobalAccess(senderUser, modifyUser, accessType);
                    accessListModel.insertAsync();
                    i18n.sendTranslated(sender, POSITIVE, "Global access for {user} set!", modifyUser);
                }
                else
                {
                    accessListModel.setValue(TABLE_ACCESS_LIST.LEVEL, accessType);
                    accessListModel.updateAsync();
                    i18n.sendTranslated(sender, POSITIVE, "Updated global access level for {user}!", modifyUser);
                }
            }
            else
            {
                if (accessListModel == null)
                {
                    i18n.sendTranslated(sender, NEUTRAL, "{user} had no global access!", modifyUser);
                }
                else
                {
                    accessListModel.deleteAsync();
                    i18n.sendTranslated(sender, POSITIVE, "Removed global access from {user}", modifyUser);
                }
            }
        }
    }

    public CompletableFuture<Integer> purgeLocksFrom(Player user)
    {
        return database.execute(database.getDSL().delete(TABLE_LOCK).where(TABLE_LOCK.OWNER_ID.eq(um.getByUUID(user.getUniqueId()).getEntityId())));
    }

    public Database getDB()
    {
        return database;
    }

    public boolean hasUnlocked(Player user, Lock lock)
    {
        return getUnlocks(user).contains(lock.getId());
    }

    private Set<Long> getUnlocks(Player user)
    {
        Set<Long> unlocks = unlocked.get(user.getUniqueId());
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
