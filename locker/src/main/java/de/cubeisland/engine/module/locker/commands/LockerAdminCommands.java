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
package de.cubeisland.engine.module.locker.commands;

import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.locker.Locker;
import de.cubeisland.engine.module.locker.storage.Lock;
import de.cubeisland.engine.module.locker.storage.LockManager;

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

@Command(name = "admin", desc = "Administrate the protections")
public class LockerAdminCommands extends CommandContainer
{
    private final LockManager manager;

    public LockerAdminCommands(Locker module, LockManager manager)
    {
        super(module);
        this.manager = manager;
    }

    private Lock getLockById(CommandContext context, Integer id)
    {
        if (LockerCommands.isNotUser(context.getSource())) return null;
        if (id == null)
        {
            context.sendTranslated(NEGATIVE, "{input} is not a valid id!", context.get(0));
            return null;
        }
        Lock lockById = this.manager.getLockById(id);
        if (lockById == null)
        {
            context.sendTranslated(NEGATIVE, "There is no protection with the id {integer}", id);
        }
        return lockById;
    }

    @Command(desc = "Opens a protected chest by protection id")
    @Params(positional = @Param(label = "id", type = Integer.class))
    public void view(CommandContext context)
    {
        Lock lock = this.getLockById(context, context.<Integer>get(0));
        switch (lock.getProtectedType())
        {
            case CONTAINER:
            case ENTITY_CONTAINER:
            case ENTITY_CONTAINER_LIVING:
                if (lock.isBlockLock())
                {
                    ((User)context.getSource()).openInventory(((InventoryHolder)lock.getFirstLocation().getBlock()
                                                                                        .getState()).getInventory());
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "The protection with the id {integer} is an entity and cannot be accessed from far away!", lock.getId());
                }
                return;
            default:
                context.sendTranslated(NEGATIVE, "The protection with the id {integer} is not a container!", lock.getId());
        }
    }

    @Command(desc = "Deletes a protection by its id")
    @Params(positional = @Param(label = "id", type = Integer.class))
    public void remove(CommandContext context)
    {
        Lock lock = this.getLockById(context, context.<Integer>get(0, null));
        if (lock == null) return;
        lock.delete((User)context.getSource());
    }

    @Command(desc = "Teleport to a protection")
    @Params(positional = @Param(label = "id", type = Integer.class))
    public void tp(CommandContext context)
    {
        Lock lock = this.getLockById(context, context.<Integer>get(0, null));
        if (lock == null) return;
        if (lock.isBlockLock())
        {
            ((User)context.getSource()).safeTeleport(lock.getFirstLocation(), TeleportCause.PLUGIN, false);
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport to an entity protection!");
        }
    }

    @Command(desc = "Deletes all locks of given player")
    @Params(positional = @Param(label = "player", type = User.class))
    public void purge(CommandContext context)
    {
        User user = context.get(0);
        this.manager.purgeLocksFrom(user);
        context.sendTranslated(POSITIVE, "All locks for {user} are now deleted!", user);
    }

    // TODO admin cmds

    public void cleanup(CommandContext context)
    {
        // cleanup remove not accessed protections / time in config
    }

    public void list(CommandContext context)
    {
        // find & show all protections of a user/selection
    }
}
