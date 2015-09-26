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

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.Lock;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.item.inventory.Carrier;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "admin", desc = "Administrate the protections")
public class LockerAdminCommands extends ContainerCommand
{
    private final LockManager manager;

    public LockerAdminCommands(Locker module, LockManager manager)
    {
        super(module);
        this.manager = manager;
    }

    private Lock getLockById(CommandContext context, Integer id)
    {
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
    @Restricted(value = MultilingualPlayer.class, msg = "This command can only be used in game")
    public void view(CommandContext context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        switch (lock.getProtectedType())
        {
            case CONTAINER:
            case ENTITY_CONTAINER:
            case ENTITY_CONTAINER_LIVING:
                if (lock.isBlockLock())
                {
                    TileEntity te = lock.getFirstLocation().getTileEntity().orNull();
                    if (te instanceof Carrier)
                    {
                        ((MultilingualPlayer)context.getSource()).original().openInventory(((Carrier)te).getInventory());
                    }
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
    @Restricted(value = MultilingualPlayer.class, msg = "This command can only be used in game")
    public void remove(CommandContext context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        if (lock == null) return;
        lock.delete((MultilingualPlayer)context.getSource());
    }

    @Command(desc = "Teleport to a protection")
    @Restricted(value = MultilingualPlayer.class, msg = "This command can only be used in game")
    public void tp(CommandContext context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        if (lock == null) return;
        if (lock.isBlockLock())
        {
            ((MultilingualPlayer)context.getSource()).original().setLocation(lock.getFirstLocation());
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport to an entity protection!");
        }
    }

    @Command(desc = "Deletes all locks of given player")
    public void purge(CommandContext context, MultilingualPlayer player)
    {
        this.manager.purgeLocksFrom(player);
        context.sendTranslated(POSITIVE, "All locks for {user} are now deleted!", player);
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
