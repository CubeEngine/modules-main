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
package org.cubeengine.module.locker.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.core.util.Triplet;
import org.cubeengine.module.core.util.matcher.StringMatcher;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.module.locker.storage.LockType;
import org.cubeengine.module.locker.storage.ProtectionFlag;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.locker.commands.CommandListener.CommandType.*;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class CommandListener
{
    private final Map<UUID, Triplet<CommandType, String, Boolean>> map = new HashMap<>();
    private final Map<UUID,Long> persist = new HashMap<>();

    private final Locker module;
    private final LockManager manager;
    private UserManager um;
    private Log logger;
    private StringMatcher stringMatcher;

    public CommandListener(Locker module, LockManager manager, UserManager um, Log logger, StringMatcher stringMatcher)
    {
        this.module = module;
        this.manager = manager;
        this.um = um;
        this.logger = logger;
        this.stringMatcher = stringMatcher;
    }

    public void setCommandType(CommandSender sender, CommandType commandType, String s, boolean b)
    {
        this.setCommandType0(sender, commandType, s, b);
    }

    public void setCommandType(CommandSender sender, CommandType commandType, String s)
    {
        this.setCommandType0(sender, commandType, s, false);
    }

    private void setCommandType0(CommandSender sender, CommandType commandType, String s, boolean b)
    {
        map.put(sender.getUniqueId(), new Triplet<>(commandType, s, b));
        if (this.doesPersist(sender.getUniqueId()))
        {
            sender.sendTranslated(NEUTRAL, "Persist mode is active. Your command will be repeated until reusing {text:/cpersist}");
        }
    }

    private boolean doesPersist(UUID uuid)
    {
        Long lastUsage = this.persist.get(uuid);
        return lastUsage != null && (System.currentTimeMillis() - lastUsage) < TimeUnit.MINUTES.toMillis(5);
    }

    /**
     * Toggles the persist mode for given user
     *
     * @param sender
     * @return true if persist mode is on for given user
     */
    public boolean persist(User sender)
    {
        if (doesPersist(sender.getUniqueId()))
        {
            persist.remove(sender.getUniqueId());
            this.map.remove(sender.getUniqueId());
            return false;
        }
        persist.put(sender.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    @Listener
    public void onRightClickBlock(InteractBlockEvent.SourcePlayer event)
    {
        if ( !(event instanceof InteractBlockEvent.Use)
            || event.getSourceEntity().get(Keys.IS_SNEAKING).get()
            || !map.keySet().contains(event.getSourceEntity().getUniqueId()))
        {
            return;
        }
        User user = um.getExactUser(event.getSourceEntity().getUniqueId());
        Location<World> location = event.getTargetLocation();
        Triplet<CommandType, String, Boolean> triplet = map.get(user.getUniqueId());
        Lock lock = this.manager.getLockAtLocation(location, user, triplet.getFirst() != INFO);

        TileEntity te = location.getTileEntity().orNull();
        if (this.handleInteract1(triplet, lock, user, te instanceof Carrier,
                 this.manager.canProtect(event.getTargetLocation().getBlockType()), event))
        {
            return;
        }

        switch (triplet.getFirst())
        {
        case C_PRIVATE:
            this.manager.createLock(event.getTargetLocation().getBlockType(), location, user, C_PRIVATE.lockType, triplet.getSecond(), triplet.getThird());
            break;
        case C_PUBLIC:
            this.manager.createLock(event.getTargetLocation().getBlockType(), location, user, C_PUBLIC.lockType, triplet.getSecond(), false);
            break;
        case C_DONATION:
            this.manager.createLock(event.getTargetLocation().getBlockType(), location, user, C_DONATION.lockType, triplet.getSecond(), triplet.getThird());
            break;
        case C_FREE:
            this.manager.createLock(event.getTargetLocation().getBlockType(), location, user, C_FREE.lockType, triplet.getSecond(), triplet.getThird());
            break;
        case C_GUARDED:
            this.manager.createLock(event.getTargetLocation().getBlockType(), location, user, C_GUARDED.lockType, triplet.getSecond(), triplet.getThird());
            break;
        default:
            this.handleInteract2(triplet.getFirst(), lock, user, triplet.getSecond(), triplet.getThird(), location,
                 te instanceof TileEntityCarrier ? ((TileEntityCarrier)te) : null);
        }
        this.cmdUsed(user);
        event.setCancelled(true);
    }

    private void cmdUsed(User user)
    {
        if (doesPersist(user.getUniqueId()))
        {
            this.persist.put(user.getUniqueId(), System.currentTimeMillis());
        }
        else
        {
            this.map.remove(user.getUniqueId());
        }
    }

    private boolean handleInteract1(Triplet<CommandType, String, Boolean> triplet, Lock lock, User user, boolean isHolder, boolean canProtect, Cancellable event)
    {
        if (triplet.getFirst().isCreator())
        {
            if (lock != null)
            {
                user.sendTranslated(NEUTRAL, "There is already protection here!");
                this.cmdUsed(user);
                event.setCancelled(true);
                return true;
            }
            else
            {
                if (!isHolder)
                {
                    switch (triplet.getFirst())
                    {
                    case C_DONATION:
                    case C_FREE:
                    case C_GUARDED:
                        user.sendTranslated(NEUTRAL, "You can only apply guarded, donation and free protections to inventory holders!");
                        event.setCancelled(true);
                        this.cmdUsed(user);
                        return true;
                    }
                }
                if (!canProtect)
                {
                    this.cmdUsed(user);
                    user.sendTranslated(NEGATIVE, "You cannot protect this!");
                    event.setCancelled(true);
                    return true; // do nothing entity is not protectable
                }
            }
        }
        else if (lock == null)
        {
            user.sendTranslated(NEUTRAL, "No protection detected here!");
            event.setCancelled(true);
            this.cmdUsed(user);
            return true;
        }
        return false;
    }

    @Listener
    public void onRightClickEntity(InteractEntityEvent.SourcePlayer event)
    {
        if (!(event instanceof InteractEntityEvent.Use)
            || event.getSourceEntity().get(Keys.IS_SNEAKING).get()
            || !map.keySet().contains(event.getSourceEntity().getUniqueId()))
        {
            return;
        }
        User user = um.getExactUser(event.getSourceEntity().getUniqueId());
        try
        {
            Entity target = event.getTargetEntity();
            Location location = target.getLocation();
            Triplet<CommandType, String, Boolean> triplet = map.get(user.getUniqueId());
            Lock lock = this.manager.getLockForEntityUID(target.getUniqueId(), triplet.getFirst() != INFO);
            if (this.handleInteract1(triplet, lock, user, target instanceof Carrier,
                                     this.manager.canProtect(target.getType()), event))
            {
                return;
            }
            switch (triplet.getFirst())
            {
            case C_PRIVATE:
                this.manager.createLock(target, user, C_PRIVATE.lockType, triplet.getSecond(), triplet
                    .getThird());
                break;
            case C_PUBLIC:
                this.manager.createLock(target, user, C_PUBLIC.lockType, triplet.getSecond(), triplet
                    .getThird());
                break;
            case C_DONATION:
                this.manager.createLock(target, user, C_DONATION.lockType, triplet.getSecond(), triplet
                    .getThird());
                break;
            case C_FREE:
                this.manager.createLock(target, user, C_FREE.lockType, triplet.getSecond(), triplet
                    .getThird());
                break;
            case C_GUARDED:
                this.manager.createLock(target, user, C_GUARDED.lockType, triplet.getSecond(), triplet
                    .getThird());
                break;
            default:
                this.handleInteract2(triplet.getFirst(), lock, user, triplet.getSecond(), triplet.getThird(), location, target instanceof Carrier ? ((Carrier)target) : null);
            }
            this.cmdUsed(user);
            event.setCancelled(true);
        }
        catch (Exception ex)
        {
            logger.error(ex, "Error with CommandInteract!");
            user.sendTranslated(CRITICAL, "An unknown error occurred!");
            user.sendTranslated(CRITICAL, "Please report this error to an administrator.");
        }
    }

    private void handleInteract2(CommandType first, Lock lock, User user, String second, Boolean third, Location location, Carrier possibleHolder)
    {
        switch (first)
        {
        case INFO:
            lock.showInfo(user);
            break;
        case MODIFY:
            lock.modifyLock(user, second);
            break;
        case REMOVE:
            this.manager.removeLock(lock, user, false);
            break;
        case UNLOCK:
            lock.unlock(user, location, second);
            break;
        case INVALIDATE_KEYS:
            if (!lock.isOwner(user))
            {
                user.sendTranslated(NEGATIVE, "This is not your protection!");
            }
            else if (lock.hasPass())
            {
                user.sendTranslated(NEUTRAL, "You cannot invalidate KeyBooks for password protected locks.");
                user.sendTranslated(POSITIVE, "Change the password to invalidate them!");
            }
            else
            {
                lock.invalidateKeyBooks();
                if (possibleHolder != null)
                {
                    possibleHolder.getInventory().<Container>query(Container.class).getViewers().forEach(Human::closeInventory);
                }
            }
            break;
        case KEYS:
            if (lock.isOwner(user) || user.hasPermission(module.perms().CMD_KEY_OTHER.getId()))
            {
                if (lock.isPublic())
                {
                    user.sendTranslated(NEUTRAL, "This protection is public!");
                }
                else
                {
                    lock.attemptCreatingKeyBook(user, third);
                }
            }
            else
            {
                user.sendTranslated(NEGATIVE, "This is not your protection!");
            }
            break;
        case GIVE:
            if (lock.isOwner(user) || user.hasPermission(module.perms().CMD_GIVE_OTHER.getId()))
            {
                // TODO UUID stuff
                User newOwner = um.getExactUser(second);
                lock.setOwner(newOwner);
                user.sendTranslated(NEUTRAL, "{user} is now the owner of this protection.", newOwner);
            }
            else
            {
                user.sendTranslated(NEGATIVE, "This is not your protection!");
            }
        case FLAGS_SET:
            if (lock.isOwner(user) || lock.hasAdmin(user) || user.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
            {
                short flags = 0;
                for (ProtectionFlag protectionFlag : ProtectionFlag.matchFlags(stringMatcher, second))
                {
                    flags |= protectionFlag.flagValue;
                }
                lock.setFlags((short)(flags | lock.getFlags()));
                user.sendTranslated(NEUTRAL, "Flags set!");
            }
            else
            {
                user.sendTranslated(NEGATIVE, "You are not allowed to modify the flags for this protection!");
            }
            break;
        case FLAGS_UNSET:
            if (lock.isOwner(user) || lock.hasAdmin(user) || user.hasPermission(module.perms().CMD_MODIFY_OTHER.getId()))
            {
                if ("all".equalsIgnoreCase(second))
                {
                    lock.setFlags(ProtectionFlag.NONE);
                    user.sendTranslated(POSITIVE, "All flags are now unset!");
                }
                else
                {
                    short flags = 0;
                    for (ProtectionFlag protectionFlag : ProtectionFlag.matchFlags(stringMatcher, second))
                    {
                        flags |= protectionFlag.flagValue;
                    }
                    lock.setFlags((short)(lock.getFlags() & ~flags));
                    user.sendTranslated(NEUTRAL, "Flags unset!");
                }
            }
            else
            {
                user.sendTranslated(NEGATIVE, "You are not allowed to modify the flags for this protection!");
            }
            break;
        default: throw new IllegalArgumentException();
        }

    }

    public enum CommandType
    {
        C_PRIVATE(LockType.PRIVATE),
        C_PUBLIC(LockType.PUBLIC),
        C_DONATION(LockType.DONATION),
        C_FREE(LockType.FREE),
        C_GUARDED(LockType.GUARDED),
        INFO,
        MODIFY,
        REMOVE,
        UNLOCK,
        INVALIDATE_KEYS,
        KEYS,
        GIVE,
        FLAGS_SET,
        FLAGS_UNSET;

        private CommandType(LockType lockType)
        {
            this.lockType = lockType;
        }

        private CommandType()
        {
            lockType = null;
        }

        public final LockType lockType;

        public boolean isCreator()
        {
            return lockType != null;
        }
    }
}
