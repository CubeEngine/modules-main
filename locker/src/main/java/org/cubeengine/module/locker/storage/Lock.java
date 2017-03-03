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

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_ADMIN;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_ALL;
import static org.cubeengine.module.locker.storage.AccessListModel.ACCESS_FULL;
import static org.cubeengine.module.locker.storage.LockType.PUBLIC;
import static org.cubeengine.module.locker.storage.TableAccessList.TABLE_ACCESS_LIST;
import static org.cubeengine.module.locker.storage.TableLockLocations.TABLE_LOCK_LOCATION;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCK;
import static org.spongepowered.api.block.BlockTypes.IRON_DOOR;
import static org.spongepowered.api.item.ItemTypes.ENCHANTED_BOOK;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.GREEN;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import com.flowpowered.math.vector.Vector3i;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.util.CauseUtil;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.commands.PlayerAccess;
import org.cubeengine.module.locker.data.LockerData;
import org.jooq.Result;
import org.jooq.types.UInteger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Lock
{
    private final Locker module;
    private final LockManager manager;
    protected final LockModel model;
    protected final ArrayList<Location<World>> locations = new ArrayList<>();
    private final Database db;

    private UUID taskId = null; // for autoclosing doors
    private I18n i18n;

    /**
     * EntityLock
     *
     * @param manager
     * @param model
     */
    public Lock(LockManager manager, LockModel model, I18n i18n)
    {
        this.i18n = i18n;
        this.db = manager.getDB();
        this.module = manager.module;
        this.manager = manager;
        this.model = model;
        this.checkLockType();
    }

    /**
     * BlockLock
     *
     * @param manager
     * @param model
     * @param lockLocs
     */
    public Lock(LockManager manager, LockModel model, I18n i18n, Result<LockLocationModel> lockLocs)
    {
        this(manager, model, i18n);
        this.locations.addAll(lockLocs.stream().map(this::getLocation).collect(toList()));
        this.isValidType = false;
    }

    public Lock(LockManager manager, LockModel model, I18n i18n, List<Location<World>> locations)
    {
        this(manager, model, i18n);
        this.locations.addAll(locations);
        this.isValidType = false;
    }

    public void invalidateKeyBooks()
    {
        this.model.createPassword(this.manager, null);
        this.model.updateAsync();
    }


    public void showCreatedMessage(Player user)
    {
        int size = getLocations().size();
        size = size == 0 ? 1 : size;
        switch (this.getLockType())
        {
        case PRIVATE:
            i18n.sendTranslatedN(user, POSITIVE, size, "Private Lock created!", "Private Lock created! ({amount} blocks)", size);
            break;
        case PUBLIC:
            i18n.sendTranslatedN(user, POSITIVE, size, "Public Lock created!", "Public Lock created! ({amount} blocks)", size);
            break;
        case GUARDED:
            i18n.sendTranslatedN(user, POSITIVE, size, "Guarded Lock created!", "Guarded Lock created! ({amount} blocks)", size);
            break;
        case DONATION:
            i18n.sendTranslatedN(user, POSITIVE, size, "Donation Lock created!", "Donation Lock created! ({amount} blocks)", size);
            break;
        case FREE:
            i18n.sendTranslatedN(user, POSITIVE, size, "Free Lock created!", "Free Lock created! ({amount} blocks)", size);
            break;
        }
    }

    public boolean handleAccess(Player user, Location soundLocation, Cancellable event)
    {
        Boolean keyBookUsed = this.checkForKeyBook(user, soundLocation);
        if (this.isOwner(user)) return true;
        if (keyBookUsed != null && !keyBookUsed)
        {
            event.setCancelled(true);
            return false;
        }
        return keyBookUsed != null || this.checkForUnlocked(user) || user.hasPermission(
            module.perms().ACCESS_OTHER.getId());
    }

    public boolean checkForUnlocked(Player user)
    {
        return manager.hasUnlocked(user, this);
    }

    public void attemptCreatingKeyBook(Player player, Boolean third)
    {
        if (this.getLockType() == PUBLIC) return; // ignore
        if (!this.manager.module.getConfig().allowKeyBooks)
        {
            i18n.sendTranslated(player, POSITIVE, "KeyBooks are not enabled!");
            return;
        }
        if (!third)
        {
            return;
        }
        ItemStack itemStack = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        if (itemStack != null && itemStack.getItem() == ItemTypes.BOOK)
        {
            itemStack.setQuantity(itemStack.getQuantity() - 1);
        }
        if (player.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getItem).orElse(null) != ItemTypes.BOOK)
        {
            // TODO allow creative with empty hand?
            i18n.sendTranslated(ACTION_BAR, player, NEGATIVE, "You need to hold a book in your hand in order to create a KeyBook!");
            return;
        }
        ItemStack item = Sponge.getRegistry().createBuilder(ItemStack.Builder.class).itemType(ENCHANTED_BOOK).quantity(1).build();
        item.offer(Keys.DISPLAY_NAME, KeyBook.TITLE.toBuilder().append(Text.of(TextColors.DARK_GRAY, getId())).build());
        item.offer(Keys.ITEM_LORE, Arrays.asList(i18n.getTranslation(player, NEUTRAL, "This book can"),
                                                 i18n.getTranslation(player, NEUTRAL, "unlock a magically"),
                                                 i18n.getTranslation(player, NEUTRAL, "locked protection")));
        item.offer(new LockerData(getId().longValue(), model.getValue(TABLE_LOCK.PASSWORD), Sponge.getRegistry().getValueFactory()));
        player.setItemInHand(HandTypes.MAIN_HAND, item);
        if (itemStack != null && itemStack.getQuantity() != 0)
        {
            InventoryTransactionResult result = player.getInventory().offer(itemStack);// TODO check response
            if (itemStack.getQuantity() != 0)
            {
                Location<World> loc = player.getLocation();
                Entity entity = loc.getExtent().createEntity(EntityTypes.ITEM, loc.getPosition());
                entity.offer(Keys.REPRESENTED_ITEM, itemStack.createSnapshot());
                loc.getExtent().spawnEntity(entity, CauseUtil.spawnCause(player));
            }
        }
    }

    /**
     * Sets the AccessLevel for given user
     *
     * @param modifyUser the user set set the accesslevel for
     * @param add whether to add to remove the access
     * @param level the accesslevel
     * @return false when updating or not deleting <p>true when inserting or deleting
     */
    public boolean setAccess(User modifyUser, boolean add, short level)
    {
        AccessListModel model = this.getAccess(modifyUser);
        if (add)
        {
            if (model == null)
            {
                model = db.getDSL().newRecord(TABLE_ACCESS_LIST).newAccess(this.model, modifyUser);
                model.setValue(TABLE_ACCESS_LIST.LEVEL, level);
                model.insertAsync();
            }
            else
            {
                model.setValue(TABLE_ACCESS_LIST.LEVEL, level);
                model.updateAsync();
                return false;
            }
        }
        else // remove lock
        {
            if (model == null) return false;
            model.deleteAsync();
        }
        return true;
    }

    /**
     * Sets multiple access-levels
     *
     * @param user the user modifying
     * @param list
     */
    public void modifyLock(Player user, List<PlayerAccess> list)
    {
        if (!this.isOwner(user) && !this.hasAdmin(user) && !user.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "You are not allowed to modify the access list of this protection!");
            return;
        }
        if (this.isPublic())
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "This protection is public and so is accessible to everyone");
            return;
        }
        for (PlayerAccess access : list)
        {
            short accessType = ACCESS_FULL;
            if (access.add && access.admin)
            {
                accessType = ACCESS_ALL; // with AdminAccess
            }
            if (this.setAccess(access.user, access.add, accessType))
            {
                if (!access.add)
                {
                    i18n.sendTranslated(user, POSITIVE, "Removed {user}'s access to this protection!", access.user);
                }
                else if (access.admin)
                {
                    i18n.sendTranslated(user, POSITIVE, "Granted {user} admin access to this protection!", access.user);
                }
                else
                {
                    i18n.sendTranslated(user, POSITIVE, "Granted {user} access to this protection!", access.user);
                }
            }
            else if (!access.add)
            {
                i18n.sendTranslated(ACTION_BAR, user, POSITIVE, "{user} had no access to this protection!", access.user);
            }
            else if (access.admin)
            {
                i18n.sendTranslated(user, POSITIVE, "Updated {user}'s access to admin access!", access.user);
            }
            else
            {
                i18n.sendTranslated(user, POSITIVE, "Updated {user}'s access to normal access!", access.user);
            }
        }
    }

    /**
     * Returns true if the chest could open
     * <p>false if the chest cannot be opened with the KeyBook
     * <p>null if the user has no KeyBook in hand
     *
     * @param user
     * @param effectLocation
     * @return
     */
    public Boolean checkForKeyBook(Player user, Location effectLocation)
    {
        KeyBook keyBook = KeyBook.getKeyBook(user.getItemInHand(HandTypes.MAIN_HAND), user, this.manager.module, i18n);
        if (keyBook != null)
        {
            return keyBook.check(this, effectLocation);
        }
        return null;
    }

    public boolean checkPass(String pass)
    {
        if (!this.hasPass()) return false;

        synchronized (this.manager.messageDigest)
        {
            this.manager.messageDigest.reset();
            return Arrays.equals(this.manager.messageDigest.digest(pass.getBytes()), this.model.getValue(TABLE_LOCK.PASSWORD));
        }
    }

    private Location<World> getLocation(LockLocationModel model)
    {
        Optional<World> world = Sponge.getServer().getWorld(model.getValue(TABLE_LOCK_LOCATION.WORLD_ID));
        return new Location<>(world.get(), model.getValue(TABLE_LOCK_LOCATION.X), model.getValue(TABLE_LOCK_LOCATION.Y), model.getValue(TABLE_LOCK_LOCATION.Z));
    }

    public boolean isBlockLock()
    {
        return !this.locations.isEmpty();
    }

    public boolean isSingleBlockLock()
    {
        return this.locations.size() == 1;
    }

    public Location<World> getFirstLocation()
    {
        return this.locations.get(0);
    }

    public ArrayList<Location<World>> getLocations()
    {
        return this.locations;
    }

    public void handleBlockDoorUse(Cancellable event, Player user, Location<World> clickedDoor)
    {
        if (this.getLockType() == PUBLIC)
        {
            this.doorUse(user, clickedDoor);
            event.setCancelled(true);
            return; // Allow everything
        }
        if (this.handleAccess(user, clickedDoor, event))
        {
            this.doorUse(user, clickedDoor);
            event.setCancelled(true);
            return;
        }
        if (event.isCancelled()) return;

        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getUniqueId())) return; // Its the owner
        switch (this.getLockType())
        {
            case PRIVATE: // block changes
                break;
            case GUARDED:
            case DONATION:
            case FREE:
            default: // Not Allowed for doors
                throw new IllegalStateException();
        }
        AccessListModel access = this.getAccess(user);
        if (access == null || !(access.canIn() && access.canOut()) || user.hasPermission(
            module.perms().ACCESS_OTHER.getId())) // No access Or not full access
        {
            event.setCancelled(true);
            if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "A magical lock from {user} prevents you from using this door!", this.getOwner());
            }
            else
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "A magical lock prevents you from using this door!");
            }
            return;
        } // else has access
        this.doorUse(user, clickedDoor);
        event.setCancelled(true);
    }

    private AccessListModel getAccess(User user)
    {
        AccessListModel model = db.getDSL().selectFrom(TABLE_ACCESS_LIST).
            where(TABLE_ACCESS_LIST.LOCK_ID.eq(this.model.getValue(TABLE_LOCK.ID)),
                  TABLE_ACCESS_LIST.USER_ID.eq(user.getUniqueId())).fetchOne();
        if (model == null)
        {


            model = db.getDSL().selectFrom(TABLE_ACCESS_LIST).
                where(TABLE_ACCESS_LIST.USER_ID.eq(user.getUniqueId()),
                      TABLE_ACCESS_LIST.OWNER_ID.eq(this.model.getValue(TABLE_LOCK.OWNER_ID))).fetchOne();
        }
        return model;
    }

    public void handleInventoryOpen(Cancellable event, Inventory protectedInventory, Location<World> soundLocation, Player user)
    {
        if (soundLocation != null && user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "This inventory is protected by {user}", this.getOwner());
        }
        if (this.handleAccess(user, soundLocation, event) || event.isCancelled()) return;
        boolean in;
        boolean out;
        switch (this.getLockType())
        {
            default: throw new IllegalStateException();
            case PUBLIC: return; // Allow everything
            case PRIVATE: // block changes
            case GUARDED:
                in = false;
                out = false;
                break;
            case DONATION:
                in = true;
                out = false;
                break;
            case FREE:
                in = false;
                out = true;
        }
        AccessListModel access = this.getAccess(user);
        if (access == null && this.getLockType() == LockType.PRIVATE && !user.hasPermission(module.perms().ACCESS_OTHER.getId()))
        {
            event.setCancelled(true); // private & no access
            if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "A magical lock from {user} prevents you from accessing this inventory!", this.getOwner());
            }
            else
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "A magical lock prevents you from accessing this inventory!");
            }
        }
        else // Has access access -> new InventoryGuard
        {
            if (access != null)
            {
                in = in || access.canIn();
                out = out || access.canOut();
            }
            this.notifyUsage(user);
            if ((in && out) || user.hasPermission(module.perms().ACCESS_OTHER.getId())) return; // Has full access
            if (protectedInventory == null) return; // Just checking else do lock
            InventoryGuardFactory igf = module.getModularity().provide(InventoryGuardFactory.class);
            igf.prepareInv(protectedInventory, user.getUniqueId());
            if (!in)
            {
                igf.blockPutInAll();
            }
            if (!out)
            {
                igf.blockTakeOutAll();
            }
            igf.submitInventory(Locker.class, false);

            this.notifyUsage(user);
            updateAccess();
        }
    }

    public void handleEntityInteract(Cancellable event, Player user)
    {
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "This entity is protected by {user}", this.getOwner());
        }
        if (this.getLockType() == PUBLIC) return;
        if (this.handleAccess(user, null, event))
        {
            this.notifyUsage(user);
            return;
        }
        if (event.isCancelled())
        {
            return;
        }
        AccessListModel access = this.getAccess(user);
        if (access == null && this.getLockType() == LockType.PRIVATE)
        {
            event.setCancelled(true); // private & no access
            if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "Magic from {user} repelled your attempts to reach this entity!", this.getOwner());
            }
            else
            {
                i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "Magic repelled your attempts to reach this entity!");
            }
        }
        this.notifyUsage(user);
    }

    private void checkLockType()
    {
        if (this.getLockType().supportedTypes.contains(this.getProtectedType())) return;
        throw new IllegalStateException("LockType is not supported for " + this.getProtectedType().name() + ":" + this.getLockType().name());
    }

    public ProtectedType getProtectedType()
    {
        return ProtectedType.forByte(this.model.getValue(TABLE_LOCK.PROTECTED_TYPE));
    }

    public LockType getLockType()
    {
        return LockType.forByte(this.model.getValue(TABLE_LOCK.LOCK_TYPE));
    }

    public void handleBlockBreak(Cancellable event, Player user)
    {
        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getUniqueId())
            || user.hasPermission(module.perms().BREAK_OTHER.getId()))
        {
            this.delete(user);
            return;
        }
        event.setCancelled(true);
        i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "Magic prevents you from breaking this protection!");
    }


    public void handleBlockInteract(Cancellable event, Player user)
    {
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "This block is protected by {user}", this.getOwner());
        }
        if (this.getLockType() == PUBLIC) return;
        if (this.handleAccess(user, null, event))
        {
            this.notifyUsage(user);
            return;
        }
        event.setCancelled(true);
        i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "Magic prevents you from interacting with this block!");
    }

    public boolean handleEntityDamage(Player user)
    {
        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getUniqueId())
            || user.hasPermission(module.perms().BREAK_OTHER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "The magic surrounding this entity quivers as you hit it!");
            return true;
        }

        // private & no access
        i18n.sendTranslated(ACTION_BAR, user, NEGATIVE, "Magic repelled your attempts to hit this entity!");
        return false;
    }

    public void handleEntityDeletion(Player user)
    {
        this.delete(user);
    }

    /**
     * Deletes a protection and informs the given user
     *
     * @param user
     */
    public void delete(Player user)
    {
        this.manager.removeLock(this, user, true);
    }

    public boolean isOwner(Player user)
    {
        return this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getUniqueId());
    }

    public boolean hasAdmin(Player user)
    {
        AccessListModel access = this.getAccess(user);
        return access != null && (access.getValue(TABLE_ACCESS_LIST.LEVEL) & ACCESS_ADMIN) == ACCESS_ADMIN;
    }

    public UInteger getId()
    {
        return this.model.getValue(TABLE_LOCK.ID);
    }

    public boolean hasPass()
    {
        return this.model.getValue(TABLE_LOCK.PASSWORD).length > 4;
    }

    private Map<UUID, Long> lastKeyNotify;

    public void notifyKeyUsage(Player user)
    {
        if (lastKeyNotify == null)
        {
            this.lastKeyNotify = new HashMap<>();
        }
        User owner = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(model.getValue(TABLE_LOCK.OWNER_ID)).get();
        if (owner.equals(user))
        {
            return;
        }
        Long last = this.lastKeyNotify.get(owner.getUniqueId());
        if (last == null || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - last) > 60) // 60 sec config ?
        {
            this.lastKeyNotify.put(owner.getUniqueId(), System.currentTimeMillis());
            if (owner.isOnline())
            {
                i18n.sendTranslated(owner.getPlayer().get(), NEUTRAL, "{user} used a KeyBook to access one of your protections!", user);
            }
        }
    }

    private Map<UUID, Long> lastNotify;

    public void notifyUsage(Player user)
    {
        if (user.hasPermission(module.perms().PREVENT_NOTIFY.getId())) return;
        if (this.hasFlag(ProtectionFlag.NOTIFY_ACCESS))
        {
            if (user.equals(this.getOwner()))
            {
                return;
            }
            if (lastNotify == null)
            {
                this.lastNotify = new HashMap<>();
            }
            User owner = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(this.model.getValue(
                TABLE_LOCK.OWNER_ID)).get();
            Long last = this.lastNotify.get(owner.getUniqueId());
            if (last == null || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - last) > 60) // 60 sec config ?
            {
                this.lastNotify.put(owner.getUniqueId(), System.currentTimeMillis());
                if (owner.isOnline())
                {
                    Player player = owner.getPlayer().get();
                    this.lastNotify.put(owner.getUniqueId(), System.currentTimeMillis());
                    if (this.isBlockLock())
                    {
                        i18n.sendTranslated(player, NEUTRAL, "{user} accessed one your protection with the id {integer}!", user, this.getId());
                        Location loc = this.getFirstLocation();
                        i18n.sendTranslated(player, NEUTRAL, "which is located at {vector} in {world}!", new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
                    }
                    else
                    {
                        for (Entity entity : user.getWorld().getEntities())
                        {
                            if (entity.getUniqueId().equals(this.getEntityUID()))
                            {
                                i18n.sendTranslated(player, NEUTRAL, "{user} accessed one of your protected entities!", user);
                                Location loc = entity.getLocation();
                                i18n.sendTranslated(player, NEUTRAL, "which is located at {vector} in {world}",  new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
                                return;
                            }
                        }
                        i18n.sendTranslated(player, NEUTRAL, "{user} accessed one of your protected entities somewhere!", user);
                    }
                }
            }
        }
    }

    public User getOwner()
    {
        return Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(this.model.getValue(TABLE_LOCK.OWNER_ID)).get();
    }

    public boolean isPublic()
    {
        return this.getLockType() == PUBLIC;
    }

    public boolean hasFlag(ProtectionFlag flag)
    {
        return flag.flagValue == (this.model.getValue(TABLE_LOCK.FLAGS) & flag.flagValue);
    }

    public void showInfo(Player user)
    {
        if (this.isOwner(user) || this.hasAdmin(user) || user.hasPermission(module.perms().CMD_INFO_OTHER.getId()))
        {
            user.sendMessage(Text.of());
            i18n.sendTranslated(user, POSITIVE, "Protection: #{integer#id} Type: {input#type} by {user}", this.getId().longValue(), this.getLockType().name(), this.getOwner());
            i18n.sendTranslated(user, POSITIVE, "protects {input#type} since {input#time}", this.getProtectedType().name(), this.model.getValue(TABLE_LOCK.CREATED).toString());
            i18n.sendTranslated(user, POSITIVE, "last access was {input#time}", this.model.getValue(TABLE_LOCK.LAST_ACCESS).toString());
            if (this.hasPass())
            {
                if (manager.hasUnlocked(user, this))
                {
                    i18n.sendTranslated(user, POSITIVE, "Has a password and is currently {text:unlocked:color=YELLOW}");
                }
                else
                {
                    i18n.sendTranslated(user, POSITIVE, "Has a password and is currently {text:locked:color=RED}");
                }
            }


            List<String> flags = new ArrayList<>();
            for (ProtectionFlag flag : ProtectionFlag.values())
            {
                if (this.hasFlag(flag))
                {
                    flags.add(flag.flagname);
                }
            }
            if (!flags.isEmpty())
            {
                i18n.sendTranslated(user, POSITIVE, "The following flags are set:");
                Text format = Text.of(" ", GRAY, "- ", YELLOW);
                for (String flag : flags)
                {
                    user.sendMessage(Text.of(format, flag));
                }
            }
            List<AccessListModel> accessors = this.getAccessors();
            if (!accessors.isEmpty())
            {
                i18n.sendTranslated(user, POSITIVE, "The following users have direct access to this protection");
                Text format = Text.of(" ", GRAY, "- ", DARK_GREEN);
                for (AccessListModel listModel : accessors)
                {
                    User accessor = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(listModel.getValue(TABLE_ACCESS_LIST.USER_ID)).get();
                    if ((listModel.getValue(TABLE_ACCESS_LIST.LEVEL) & ACCESS_ADMIN) == ACCESS_ADMIN)
                    {
                        user.sendMessage(Text.of(format, GREEN, accessor.getName(), GOLD, " [Admin]"));
                    }
                    else
                    {
                        user.sendMessage(Text.of(format, GREEN, accessor.getName()));
                    }
                }
            }
            if (!this.locations.isEmpty())
            {
                i18n.sendTranslatedN(user, POSITIVE, locations.size(), "This protection covers a single block!", "This protections covers {amount} blocks!", locations.size());
            }
        }
        else
        {
            if (user.hasPermission(module.perms().CMD_INFO_SHOW_OWNER.getId()))
            {
                i18n.sendTranslated(user, POSITIVE, "ProtectionType: {input#locktype} Owner: {user}", this.getLockType().name(), this.getOwner());
            }
            else
            {
                i18n.sendTranslated(user, POSITIVE, "ProtectionType: {input#locktype}", this.getLockType().name());
            }
            AccessListModel access = this.getAccess(user);
            if (this.hasPass())
            {
                if (manager.hasUnlocked(user, this))
                {
                    i18n.sendTranslated(user, POSITIVE, "As you memorize the pass phrase the magic aura protecting this allows you to interact");
                }
                else
                {
                    i18n.sendTranslated(user, POSITIVE, "You sense that the strong magic aura protecting this won't let you through without the right passphrase");
                }
            }
            else
            {
                i18n.sendTranslated(user, POSITIVE, "You sense a strong magic aura protecting this");
            }
            if (access != null)
            {
                if (access.canIn() && access.canOut())
                {
                    if (this.getProtectedType() == ProtectedType.CONTAINER
                        || this.getProtectedType() == ProtectedType.ENTITY_CONTAINER
                        || this.getProtectedType() == ProtectedType.ENTITY_CONTAINER_LIVING)
                    {
                        i18n.sendTranslated(user, POSITIVE, "but it does not hinder you when moving items");
                    }
                    else
                    {
                        i18n.sendTranslated(user, POSITIVE, "but it lets you interact as if you were not there");
                    }
                }
            }
        }
        if (this.manager.module.getConfig().protectWhenOnlyOffline && this.getOwner().isOnline())
        {
            i18n.sendTranslated(user, NEUTRAL, "The protection is currently not active because its owner is online!");
        }
        if (this.manager.module.getConfig().protectWhenOnlyOnline && !this.getOwner().isOnline())
        {
            i18n.sendTranslated(user, NEUTRAL, "The protection is currently not active because its owner is offline!");
        }
    }

    public List<AccessListModel> getAccessors()
    {
        return db.getDSL().selectFrom(TABLE_ACCESS_LIST).
            where(TABLE_ACCESS_LIST.LOCK_ID.eq(this.model.getValue(TABLE_LOCK.ID))).fetch();
    }

    public void unlock(Player user, Location<World> soundLoc, String pass)
    {
        if (this.hasPass())
        {
            if (this.checkPass(pass))
            {
                i18n.sendTranslated(ACTION_BAR, user, POSITIVE, "Upon hearing the right passphrase the magic gets thinner and lets you pass!");
                KeyBook.playUnlockSound(user, soundLoc, module.getTaskManager());

                manager.addUnlock(user, this);
            }
            else
            {
                // TODO creative has no effect
                i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "Sudden pain makes you realize this was not the right passphrase!");
                user.damage(1, DamageSources.MAGIC, Cause.of(NamedCause.source(user)));
            }
        }
        else
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "You try to open the container with a passphrase but nothing changes!");
        }
    }

    /**
     * If this lock protects a double-door this will open/close the second door too.
     * Also this will schedule auto-closing the door according to the configuration
     *
     * @param user
     * @param doorClicked
     */
    private void doorUse(Player user, Location<World> doorClicked)
    {
        if (doorClicked.getBlockType() == IRON_DOOR && !manager.module.getConfig().openIronDoorWithClick)
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "You cannot open the heavy door!");
            return;
        }
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            i18n.sendTranslated(ACTION_BAR, user, NEUTRAL, "This door is protected by {user}", this.getOwner());
        }
        if (!doorClicked.supports(Keys.OPEN))
        {
            return;
        }
        boolean open = doorClicked.get(Keys.OPEN).get();

        for (Location<World> door : locations)
        {
            if (open)
            {
                door.setBlock(door.getBlock().with(Keys.OPEN, false).get(), Cause.source(module.getPlugin()).build()); // TODO add user
                user.playSound(SoundTypes.BLOCK_WOODEN_DOOR_CLOSE, door.getPosition(), 1);
            }
            else
            {
                door.setBlock(door.getBlock().with(Keys.OPEN, true).get(), Cause.source(module.getPlugin()).build()); // TODO add user
                user.playSound(SoundTypes.BLOCK_WOODEN_DOOR_OPEN, door.getPosition(), 1);
            }
        }
        if (taskId != null) module.getTaskManager().cancelTask(Locker.class, taskId);
        if (!open)
        {
            this.scheduleAutoClose();
        }
        this.notifyUsage(user);
    }

    private void scheduleAutoClose()
    {
        if (this.hasFlag(ProtectionFlag.AUTOCLOSE))
        {
            if (!this.manager.module.getConfig().autoCloseEnable) return;
            taskId = module.getTaskManager().runTaskDelayed(Locker.class, () -> {
                int n = locations.size() / 2;
                for (Location location : locations)
                {
                    if (n-- > 0)
                    {
                        ((World)location.getExtent()).playSound(SoundTypes.BLOCK_WOODEN_DOOR_CLOSE, location.getPosition(), 1);
                    }
                    location.setBlock(location.getBlock().with(Keys.OPEN, false).get(), Cause.source(manager.module.getPlugin()).build());
                }
            }, this.manager.module.getConfig().autoCloseSeconds * 20);
        }
    }

    /**
     * Always true for EntityLocks
     */
    private boolean isValidType = true;

    public boolean validateTypeAt(Location location)
    {
        if (ProtectedType.getProtectedType(location.getBlockType()) == this.getProtectedType())
        {
            this.isValidType = true;
        }
        else
        {
            module.getProvided(Log.class).warn("ProtectedTypes do not match for Guard at {}", location.toString());
        }
        return this.isValidType;
    }

    public void setOwner(User owner)
    {
        this.model.setValue(TABLE_LOCK.OWNER_ID, owner.getUniqueId());
        this.model.updateAsync();
    }

    public void setFlags(short flags)
    {
        this.model.setValue(TABLE_LOCK.FLAGS, flags);
        this.model.updateAsync();
    }

    public short getFlags()
    {
        return this.model.getValue(TABLE_LOCK.FLAGS);
    }

    public UUID getEntityUID()
    {
        return this.model.getUUID();
    }

    public void updateAccess()
    {
        model.setValue(TABLE_LOCK.LAST_ACCESS, new Timestamp(System.currentTimeMillis()));
        model.updateAsync();
    }
}
