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

import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.module.locker.storage.LockType;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.TameEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;

public class CommandListener
{
    private final Map<UUID, LockAction> lockActions = new HashMap<>();
    private final Map<UUID, Long> persist = new HashMap<>();

    private Locker module;
    private final LockManager manager;
    private I18n i18n;

    public CommandListener(Locker module, LockManager manager, I18n i18n)
    {
        this.module = module;
        this.manager = manager;
        this.i18n = i18n;
    }

    public void submitLockAction(Player sender, LockAction lockAction)
    {
        lockActions.put(sender.getUniqueId(), lockAction);
        persistInfo(sender);
    }

    private void persistInfo(Player sender)
    {
        if (this.doesPersist(sender.getUniqueId()))
        {
            i18n.sendTranslated(sender, NEUTRAL, "Persist mode is active. Your command will be repeated until reusing {text:/cpersist}");
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
    public boolean persist(Player sender)
    {
        if (doesPersist(sender.getUniqueId()))
        {
            persist.remove(sender.getUniqueId());
            this.lockActions.remove(sender.getUniqueId());
            return false;
        }
        persist.put(sender.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    @Listener
    public void onRightClickBlock(InteractBlockEvent.Secondary event, @First Player player)
    {
        if (isPlayerInteract(player))
        {
            return;
        }

        Location<World> location = event.getTargetBlock().getLocation().get();

        LockAction lockAction = lockActions.get(player.getUniqueId());
        if (lockAction != null)
        {
            Lock lock = this.manager.getLockAtLocation(location, player);
            if (lock != null || lockAction instanceof LockAction.LockCreateAction)
            {
                lockAction.apply(lock, location, null);
            }
            // else no lock to modify
            cmdUsed(player);
            event.setCancelled(true);
        }
    }

    private boolean isPlayerInteract(Player player)
    {
        return (player.get(Keys.IS_SNEAKING).get() ||
                !lockActions.keySet().contains(player.getUniqueId()));
    }

    private void cmdUsed(Player user)
    {
        if (doesPersist(user.getUniqueId()))
        {
            this.persist.put(user.getUniqueId(), System.currentTimeMillis());
        }
        else
        {
            this.lockActions.remove(user.getUniqueId());
        }
    }

    @Listener
    public void onRightClickEntity(InteractEntityEvent.Secondary event, @First Player player)
    {
        if (isPlayerInteract(player))
        {
            return;
        }
        LockAction lockAction = lockActions.get(player.getUniqueId());

        if (lockAction != null)
        {
            Entity target = event.getTargetEntity();
            Location<World> location = target.getLocation();

            Lock lock = this.manager.getLockForEntityUID(target.getUniqueId());

            lockAction.apply(lock, location, target);

            cmdUsed(player);
            event.setCancelled(true);
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

        CommandType(LockType lockType)
        {
            this.lockType = lockType;
        }

        CommandType()
        {
            lockType = null;
        }

        public final LockType lockType;

        public boolean isCreator()
        {
            return lockType != null;
        }
    }


    @Listener
    public void onTame(TameEntityEvent event, @First Player player)
    {
        Entity entity = event.getTargetEntity();
        for (ConfigWorld world : module.getConfig().disableAutoProtect)
        {
            if (entity.getWorld().equals(world.getWorld()))
            {
                return;
            }
        }
        module.getConfig().entityProtections.stream()
                .filter(entityProtection -> entityProtection.isType(entity.getType()) && entityProtection.isAutoProtect())
                .forEach(entityProtection -> {
                    if (this.manager.getLockForEntityUID(entity.getUniqueId()) == null)
                    {
                        this.manager.createLock(entity, player, entityProtection.getAutoProtect(), null, false);
                    }
                });
    }
}
