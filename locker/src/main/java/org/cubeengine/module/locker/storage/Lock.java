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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.InventoryGuardFactory;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.LockerAttachment;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.user.MultilingualPlayer;
import org.jooq.Result;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.PortionTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.block.BreakBlockEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.locker.storage.AccessListModel.*;
import static org.cubeengine.module.locker.storage.LockType.PUBLIC;
import static org.cubeengine.module.locker.storage.TableAccessList.TABLE_ACCESS_LIST;
import static org.cubeengine.module.locker.storage.TableLocks.TABLE_LOCK;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.block.BlockTypes.IRON_DOOR;
import static org.spongepowered.api.item.ItemTypes.ENCHANTED_BOOK;

public class Lock
{
    private final Locker module;
    private final LockManager manager;
    protected final LockModel model;
    protected final ArrayList<Location<World>> locations = new ArrayList<>();
    private final Database db;

    private UUID taskId = null; // for autoclosing doors

    /**
     * EntityLock
     *
     * @param manager
     * @param model
     */
    public Lock(LockManager manager, LockModel model)
    {
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
    public Lock(LockManager manager, LockModel model, Result<LockLocationModel> lockLocs)
    {
        this(manager, model);
        this.locations.addAll(lockLocs.stream().map(this::getLocation).collect(toList()));
        this.isValidType = false;
    }

    public Lock(LockManager manager, LockModel model, List<Location<World>> locations)
    {
        this(manager, model);
        this.locations.addAll(locations);
        this.isValidType = false;
    }

    public void invalidateKeyBooks()
    {
        this.model.createPassword(this.manager, null);
        this.model.updateAsync();
    }


    public void showCreatedMessage(MultilingualPlayer user)
    {
        switch (this.getLockType())
        {
        case PRIVATE:
            user.sendTranslated(POSITIVE, "Private Lock created!");
            break;
        case PUBLIC:
            user.sendTranslated(POSITIVE, "Public Lock created!");
            break;
        case GUARDED:
            user.sendTranslated(POSITIVE, "Guarded Lock created!");
            break;
        case DONATION:
            user.sendTranslated(POSITIVE, "Donation Lock created!");
            break;
        case FREE:
            user.sendTranslated(POSITIVE, "Free Lock created!");
            break;
        }
    }

    public boolean handleAccess(MultilingualPlayer user, Location soundLocation, Cancellable event)
    {
        if (this.isOwner(user)) return true;
        Boolean keyBookUsed = this.checkForKeyBook(user, soundLocation);
        if (keyBookUsed != null && !keyBookUsed)
        {
            event.setCancelled(true);
            return false;
        }
        return keyBookUsed != null || this.checkForUnlocked(user) || user.hasPermission(
            module.perms().ACCESS_OTHER.getId());
    }

    public boolean checkForUnlocked(MultilingualPlayer user)
    {
        LockerAttachment lockerAttachment = user.get(LockerAttachment.class);
        return lockerAttachment != null && lockerAttachment.hasUnlocked(this);
    }

    public void attemptCreatingKeyBook(MultilingualPlayer user, Boolean third)
    {
        if (this.getLockType() == PUBLIC) return; // ignore
        if (!this.manager.module.getConfig().allowKeyBooks)
        {
            user.sendTranslated(POSITIVE, "KeyBooks are not enabled!");
            return;
        }
        if (!third)
        {
            return;
        }
        ItemStack itemStack = user.original().getItemInHand().orNull();
        if (itemStack != null && itemStack.getItem() == ItemTypes.BOOK)
        {
            itemStack.setQuantity(itemStack.getQuantity() - 1);
        }
        if (user.original().getItemInHand().transform(ItemStack::getItem).orNull() != ItemTypes.BOOK)
        {
            user.sendTranslated(NEGATIVE, "Could not create KeyBook! You need to hold a book in your hand in order to do this!");
            return;
        }
        ItemStack item = module.getGame().getRegistry().createItemBuilder().itemType(ENCHANTED_BOOK).quantity(1).build();
        item.offer(Keys.DISPLAY_NAME, Texts.of(getColorPass() + KeyBook.TITLE + getId()));
        item.offer(Keys.ITEM_LORE, Arrays.asList(user.getTranslation(NEUTRAL, "This book can"), user.getTranslation(
            NEUTRAL, "unlock a magically"), user.getTranslation(NEUTRAL, "locked protection")));
        user.original().setItemInHand(item);
        if (itemStack != null)
        {
            user.original().getInventory().offer(itemStack);
            if (itemStack.getQuantity() != 0)
            {
                // TODO drop items in world
                //((World)location.getExtent()).dropItem(location, stack);
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
    public boolean setAccess(MultilingualPlayer modifyUser, boolean add, short level)
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
     * Sets multiple acceslevels
     *
     * @param user the user modifying
     * @param usersString
     */
    public void modifyLock(MultilingualPlayer user, String usersString)
    {
        if (this.isOwner(user) || this.hasAdmin(user) || user.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
        {
            if (!this.isPublic())
            {
                String[] explode = StringUtils.explode(",", usersString); // TODO custom reader also for all other occurences of user with - or @ in front
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
                    MultilingualPlayer modifyUser = this.manager.um.findExactUser(name);
                    if (modifyUser == null) throw new IllegalArgumentException(); // This is prevented by checking first in the cmd execution
                    short accessType = ACCESS_FULL;
                    if (add && admin)
                    {
                        accessType = ACCESS_ALL; // with AdminAccess
                    }
                    if (this.setAccess(modifyUser, add, accessType))
                    {
                        if (add)
                        {
                            if (admin)
                            {
                                user.sendTranslated(POSITIVE, "Granted {user} admin access to this protection!", modifyUser);
                            }
                            else
                            {
                                user.sendTranslated(POSITIVE, "Granted {user} access to this protection!", modifyUser);
                            }
                        }
                        else
                        {
                            user.sendTranslated(POSITIVE, "Removed {user}'s access to this protection!", modifyUser);
                        }
                    }
                    else
                    {
                        if (add)
                        {
                            if (admin)
                            {
                                user.sendTranslated(POSITIVE, "Updated {user}'s access to admin access!", modifyUser);
                            }
                            else
                            {
                                user.sendTranslated(POSITIVE, "Updated {user}'s access to normal access!", modifyUser);
                            }
                        }
                        else
                        {
                            user.sendTranslated(POSITIVE, "{user} had no access to this protection!", modifyUser);
                        }
                    }
                }
                return;
            }
            user.sendTranslated(NEUTRAL, "This protection is public and so is accessible to everyone");
            return;
        }
        user.sendTranslated(NEGATIVE, "You are not allowed to modify the access list of this protection!");
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
    public Boolean checkForKeyBook(MultilingualPlayer user, Location effectLocation)
    {
        KeyBook keyBook = KeyBook.getKeyBook(user.original().getItemInHand().orNull(), user, this.manager.module);
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
        return new Location<>(this.manager.wm.getWorld(model.getValue(TableLockLocations.TABLE_LOCK_LOCATION.WORLD_ID)), model.getValue(
            TableLockLocations.TABLE_LOCK_LOCATION.X), model.getValue(TableLockLocations.TABLE_LOCK_LOCATION.Y), model.getValue(
            TableLockLocations.TABLE_LOCK_LOCATION.Z));
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

    public void handleBlockDoorUse(Cancellable event, MultilingualPlayer user, Location<World> clickedDoor)
    {
        if (this.getLockType() == PUBLIC)
        {
            this.doorUse(user, clickedDoor);
            return; // Allow everything
        }
        if (this.handleAccess(user, clickedDoor, event))
        {
            this.doorUse(user, clickedDoor);
            return;
        }
        if (event.isCancelled()) return;
        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getEntity().getId())) return; // Its the owner
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
                user.sendTranslated(NEGATIVE, "A magical lock from {user} prevents you from using this door!", this.getOwner());
            }
            else
            {
                user.sendTranslated(NEGATIVE, "A magical lock prevents you from using this door!");
            }
            return;
        } // else has access
        this.doorUse(user, clickedDoor);
    }

    private AccessListModel getAccess(MultilingualPlayer user)
    {
        AccessListModel model = db.getDSL().selectFrom(TABLE_ACCESS_LIST).
            where(TABLE_ACCESS_LIST.LOCK_ID.eq(this.model.getValue(TABLE_LOCK.ID)),
                  TABLE_ACCESS_LIST.USER_ID.eq(user.getEntity().getId())).fetchOne();
        if (model == null)
        {
            model = db.getDSL().selectFrom(TABLE_ACCESS_LIST).
                where(TABLE_ACCESS_LIST.USER_ID.eq(user.getEntity().getId()),
                      TABLE_ACCESS_LIST.OWNER_ID.eq(this.getOwner().getEntity().getId())).fetchOne();
        }
        return model;
    }

    public void handleInventoryOpen(Cancellable event, Inventory protectedInventory, Location soundLocation, MultilingualPlayer user)
    {
        if (soundLocation != null && user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            user.sendTranslated(NEUTRAL, "This inventory is protected by {user}", this.getOwner());
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
        if (access == null && this.getLockType() == LockType.PRIVATE && !user.hasPermission(
            module.perms().ACCESS_OTHER.getId()))
        {
            event.setCancelled(true); // private & no access
            if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
            {
                user.sendTranslated(NEGATIVE, "A magical lock from {user} prevents you from accessing this inventory!", this.getOwner());
            }
            else
            {
                user.sendTranslated(NEGATIVE, "A magical lock prevents you from accessing this inventory!");
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
            InventoryGuardFactory inventoryGuardFactory = module.getModularity().provide(InventoryGuardFactory.class);
            inventoryGuardFactory.prepareInv(protectedInventory, user);
            if (!in)
            {
                inventoryGuardFactory.blockPutInAll();
            }
            if (!out)
            {
                inventoryGuardFactory.blockTakeOutAll();
            }
            inventoryGuardFactory.submitInventory(this.manager.module, false);

            this.notifyUsage(user);
        }
    }

    public void handleEntityInteract(Cancellable event, MultilingualPlayer user)
    {
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            user.sendTranslated(NEUTRAL, "This entity is protected by {user}", this.getOwner());
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
                user.sendTranslated(NEGATIVE, "Magic from {user} repelled your attempts to reach this entity!", this.getOwner());
            }
            else
            {
                user.sendTranslated(NEGATIVE, "Magic repelled your attempts to reach this entity!");
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

    public void handleBlockBreak(BreakBlockEvent.SourcePlayer event, MultilingualPlayer user)
    {
        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getEntity().getId()) || user.hasPermission(
            module.perms().BREAK_OTHER.getId()))
        {
            this.delete(user);
            return;
        }
        event.setCancelled(true);
        user.sendTranslated(NEGATIVE, "Magic prevents you from breaking this protection!");
    }


    public void handleBlockInteract(Cancellable event, MultilingualPlayer user)
    {
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            user.sendTranslated(NEUTRAL, "This block is protected by {user}", this.getOwner());
        }
        if (this.getLockType() == PUBLIC) return;
        if (this.handleAccess(user, null, event))
        {
            this.notifyUsage(user);
            return;
        }
        event.setCancelled(true);
        user.sendTranslated(NEGATIVE, "Magic prevents you from interacting with this block!");
    }

    public boolean handleEntityDamage(Cancellable event, MultilingualPlayer user)
    {
        if (this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getEntity().getId()) || user.hasPermission(
            module.perms().BREAK_OTHER.getId()))
        {
            user.sendTranslated(NEUTRAL, "The magic surrounding this entity quivers as you hit it!");
            return true;
        }
        event.setCancelled(true); // private & no access
        user.sendTranslated(NEGATIVE, "Magic repelled your attempts to hit this entity!");
        return false;
    }

    public void handleEntityDeletion(MultilingualPlayer user)
    {
        this.delete(user);
    }

    /**
     * Deletes a protection and informs the given user
     *
     * @param user
     */
    public void delete(MultilingualPlayer user)
    {
        this.manager.removeLock(this, user, true);
    }

    public boolean isOwner(MultilingualPlayer user)
    {
        return this.model.getValue(TABLE_LOCK.OWNER_ID).equals(user.getEntity().getId());
    }

    public boolean hasAdmin(MultilingualPlayer user)
    {
        AccessListModel access = this.getAccess(user);
        return access != null && (access.getValue(TABLE_ACCESS_LIST.LEVEL) & ACCESS_ADMIN) == ACCESS_ADMIN;
    }

    public String getColorPass()
    {
        return this.model.getColorPass();
    }

    public Long getId()
    {
        return this.model.getValue(TABLE_LOCK.ID).longValue();
    }

    public boolean hasPass()
    {
        return this.model.getValue(TABLE_LOCK.PASSWORD).length > 4;
    }

    private Map<UUID, Long> lastKeyNotify;

    public void notifyKeyUsage(MultilingualPlayer user)
    {
        if (lastKeyNotify == null)
        {
            this.lastKeyNotify = new HashMap<>();
        }
        MultilingualPlayer owner = this.manager.um.getById(this.model.getValue(TABLE_LOCK.OWNER_ID));
        Long last = this.lastKeyNotify.get(owner.getUniqueId());
        if (last == null || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - last) > 60) // 60 sec config ?
        {
            this.lastKeyNotify.put(owner.getUniqueId(), System.currentTimeMillis());
            owner.sendTranslated(NEUTRAL, "{user} used a KeyBook to access one of your protections!", user);
        }
    }

    private Map<UUID, Long> lastNotify;

    public void notifyUsage(MultilingualPlayer user)
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
            MultilingualPlayer owner = this.manager.um.getById(this.model.getValue(TABLE_LOCK.OWNER_ID));
            Long last = this.lastNotify.get(owner.getUniqueId());
            if (last == null || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - last) > 60) // 60 sec config ?
            {
                this.lastNotify.put(owner.getUniqueId(), System.currentTimeMillis());
                if (this.isBlockLock())
                {
                    owner.sendTranslated(NEUTRAL, "{user} accessed one your protection with the id {integer}!", user, this.getId());
                    Location loc = this.getFirstLocation();
                    owner.sendTranslated(NEUTRAL, "which is located at {vector} in {world}!", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
                }
                else
                {
                    for (Entity entity : user.original().getWorld().getEntities())
                    {
                        if (entity.getUniqueId().equals(this.getEntityUID()))
                        {
                            owner.sendTranslated(NEUTRAL, "{user} accessed one of your protected entities!", user);
                            Location loc = entity.getLocation();
                            owner.sendTranslated(NEUTRAL, "which is located at {vector} in {world}",  new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
                            return;
                        }
                    }
                    owner.sendTranslated(NEUTRAL, "{user} accessed one of your protected entities somewhere!", user);
                }
            }
        }
    }

    public MultilingualPlayer getOwner()
    {
        return module.getUserManager().getById(this.model.getValue(TABLE_LOCK.OWNER_ID));
    }

    public boolean isPublic()
    {
        return this.getLockType() == PUBLIC;
    }

    public boolean hasFlag(ProtectionFlag flag)
    {
        return flag.flagValue == (this.model.getValue(TABLE_LOCK.FLAGS) & flag.flagValue);
    }

    public void showInfo(MultilingualPlayer user)
    {
        if (this.isOwner(user) || this.hasAdmin(user) || user.hasPermission(module.perms().CMD_INFO_OTHER.getId()))
        {
            user.sendMessage("");
            user.sendTranslated(POSITIVE, "Protection: #{integer#id} Type: {input#type} by {user}", this.getId(), this.getLockType().name(), this.getOwner());
            user.sendTranslated(POSITIVE, "protects {input#type} since {input#time}", this.getProtectedType().name(), this.model.getValue(TABLE_LOCK.CREATED).toString());
            user.sendTranslated(POSITIVE, "last access was {input#time}", this.model.getValue(TABLE_LOCK.LAST_ACCESS).toString());
            if (this.hasPass())
            {
                if (user.attachOrGet(LockerAttachment.class, this.manager.module).hasUnlocked(this))
                {
                    user.sendTranslated(POSITIVE, "Has a password and is currently {text:unlocked:color=YELLOW}");
                }
                else
                {
                    user.sendTranslated(POSITIVE, "Has a password and is currently {text:locked:color=RED}");
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
                user.sendTranslated(POSITIVE, "The following flags are set:");
                String format = "  " + ChatFormat.GREY + "- " + ChatFormat.YELLOW;
                for (String flag : flags)
                {
                    user.sendMessage(format + flag);
                }
            }
            List<AccessListModel> accessors = this.getAccessors();
            if (!accessors.isEmpty())
            {
                user.sendTranslated(POSITIVE, "The following users have direct access to this protection");
                for (AccessListModel listModel : accessors)
                {
                    MultilingualPlayer accessor = module.getUserManager().getById(listModel.getValue(
                        TABLE_ACCESS_LIST.USER_ID));
                    if ((listModel.getValue(TABLE_ACCESS_LIST.LEVEL) & ACCESS_ADMIN) == ACCESS_ADMIN)
                    {
                        user.sendMessage("  " + ChatFormat.GREY + "- " + ChatFormat.DARK_GREEN + accessor.getDisplayName() + ChatFormat.GOLD + " [Admin}");
                    }
                    else
                    {
                        user.sendMessage("  " + ChatFormat.GREY + "- " + ChatFormat.DARK_GREEN + accessor.getDisplayName());
                    }
                }
            }
            if (!this.locations.isEmpty())
            {
                user.sendTranslated(POSITIVE, "This protections covers {amount} blocks!", this.locations.size());
            }
        }
        else
        {
            if (user.hasPermission(module.perms().CMD_INFO_SHOW_OWNER.getId()))
            {
                user.sendTranslated(POSITIVE, "ProtectionType: {input#locktype} Owner: {user}", this.getLockType().name(), this.getOwner());
            }
            else
            {
                user.sendTranslated(POSITIVE, "ProtectionType: {input#locktype}", this.getLockType().name());
            }
            AccessListModel access = this.getAccess(user);
            if (this.hasPass())
            {
                if (user.attachOrGet(LockerAttachment.class, this.manager.module).hasUnlocked(this))
                {
                    user.sendTranslated(POSITIVE, "As you memorize the pass phrase the magic aura protecting this allows you to interact");
                }
                else
                {
                    user.sendTranslated(POSITIVE, "You sense that the strong magic aura protecting this won't let you through without the right passphrase");
                }
            }
            else
            {
                user.sendTranslated(POSITIVE, "You sense a strong magic aura protecting this");
            }
            if (access != null)
            {

                if (access.canIn() && access.canOut())
                {
                    if (this.getProtectedType() == ProtectedType.CONTAINER
                        || this.getProtectedType() == ProtectedType.ENTITY_CONTAINER
                        || this.getProtectedType() == ProtectedType.ENTITY_CONTAINER_LIVING)
                    {
                        user.sendTranslated(POSITIVE, "but it does not hinder you when moving items");
                    }
                    else
                    {
                        user.sendTranslated(POSITIVE, "but it lets you interact as if you were not there");
                    }
                }
            }
        }
        if (this.manager.module.getConfig().protectWhenOnlyOffline && this.getOwner().original().isOnline())
        {
            user.sendTranslated(NEUTRAL, "The protection is currently not active because its owner is online!");
        }
        if (this.manager.module.getConfig().protectWhenOnlyOnline && !this.getOwner().original().isOnline())
        {
            user.sendTranslated(NEUTRAL, "The protection is currently not active because its owner is offline!");
        }
    }

    public List<AccessListModel> getAccessors()
    {
        return db.getDSL().selectFrom(TABLE_ACCESS_LIST).
            where(TABLE_ACCESS_LIST.LOCK_ID.eq(this.model.getValue(TABLE_LOCK.ID))).fetch();
    }

    public void unlock(MultilingualPlayer user, Location soundLoc, String pass)
    {
        if (this.hasPass())
        {
            if (this.checkPass(pass))
            {
                user.sendTranslated(POSITIVE, "Upon hearing the right passphrase the magic surrounding the container gets thinner and lets you pass!");
                user.original().playSound(SoundTypes.PISTON_EXTEND, soundLoc.getPosition(), 1, 2);
                user.original().playSound(SoundTypes.PISTON_EXTEND, soundLoc.getPosition(), 1, (float)1.5);
                user.attachOrGet(LockerAttachment.class, this.manager.module).addUnlock(this);
            }
            else
            {
                user.sendTranslated(NEUTRAL, "Sudden pain makes you realize this was not the right passphrase!");
                // TODO deal 0 damage is this working?
                user.original().playSound(SoundTypes.HURT_FLESH, user.original().getLocation().getPosition(), 1);
                user.original().offer(Keys.INVULNERABILITY_TICKS, 1);
            }
        }
        else
        {
            user.sendTranslated(NEUTRAL, "You try to open the container with a passphrase but nothing changes!");
        }
    }

    /**
     * If this lock protects a double-door this will open/close the second door too.
     * Also this will schedule auto-closing the door according to the configuration
     *
     * @param user
     * @param doorClicked
     */
    private void doorUse(MultilingualPlayer user, Location<World> doorClicked)
    {
        if (doorClicked.getBlockType() == IRON_DOOR && !manager.module.getConfig().openIronDoorWithClick)
        {
            user.sendTranslated(NEUTRAL, "You cannot open the heavy door!");
            return;
        }
        if (user.hasPermission(module.perms().SHOW_OWNER.getId()))
        {
            user.sendTranslated(NEUTRAL, "This door is protected by {user}", this.getOwner());
        }
        if (!doorClicked.supports(Keys.OPEN))
        {
            return;
        }
        boolean open = doorClicked.get(Keys.OPEN).get();

        for (Location door : locations)
        {
            if (door.get(Keys.PORTION_TYPE).get() == PortionTypes.TOP)
            {
                continue;
            }
            if (open)
            {
                door.offer(Keys.OPEN, false);
                user.original().playSound(SoundTypes.DOOR_CLOSE, door.getPosition(), 1);
            }
            else
            {
                door.offer(Keys.OPEN, true);
                user.original().playSound(SoundTypes.DOOR_OPEN, door.getPosition(), 1);
            }
        }
        if (taskId != null) module.getTaskManager().cancelTask(this.manager.module, taskId);
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
            taskId = module.getTaskManager().runTaskDelayed(this.manager.module, () -> {
                int n = locations.size() / 2;
                for (Location location : locations)
                {
                    if (n-- > 0)
                    {
                        ((World)location.getExtent()).playSound(SoundTypes.DOOR_CLOSE, location.getPosition(), 1);
                    }
                    location.offer(Keys.OPEN, false);
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

    public void setOwner(MultilingualPlayer owner)
    {
        this.model.setValue(TABLE_LOCK.OWNER_ID, owner.getEntity().getId());
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
}
