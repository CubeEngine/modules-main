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
package org.cubeengine.module.locker.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Carrier;

import javax.inject.Inject;

@Command(name = "admin", desc = "Administrate the protections")
public class LockerAdminCommands extends ContainerCommand
{
    private final LockManager manager;
    private I18n i18n;

    @Inject
    public LockerAdminCommands(CommandManager base, Locker module, I18n i18n)
    {
        super(base, Locker.class);
        this.manager = module.getManager();
        this.i18n = i18n;
    }

    private Lock getLockById(CommandSource context, Integer id)
    {
        if (id == null)
        {
            i18n.send(context, NEGATIVE, "Invalid id!"); // TODO parameter reader ; default block you are looking at
            return null;
        }
        Lock lockById = this.manager.getLockById(id.longValue());
        if (lockById == null)
        {
            i18n.send(context, NEGATIVE, "There is no protection with the id {integer}", id);
        }
        return lockById;
    }

    @Command(desc = "Opens a protected chest by protection id")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void view(CommandSource context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        switch (lock.getProtectedType())
        {
            case CONTAINER:
            case ENTITY_CONTAINER:
            case ENTITY_CONTAINER_LIVING:
                if (lock.isBlockLock())
                {
                    TileEntity te = lock.getFirstLocation().getTileEntity().orElse(null);
                    if (te instanceof Carrier)
                    {
                        Sponge.getCauseStackManager().pushCause(context);
                        ((Player)context).openInventory(((Carrier)te).getInventory());
                    }
                }
                else
                {
                    i18n.send(context, NEGATIVE, "The protection with the id {integer} is an entity and cannot be accessed from far away!", lock.getId());
                }
                return;
            default:
                i18n.send(context, NEGATIVE, "The protection with the id {integer} is not a container!", lock.getId());
        }
    }

    @Command(desc = "Deletes a protection by its id")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void remove(CommandSource context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        if (lock == null) return;
        lock.delete((Player)context);
    }

    @Command(desc = "Teleport to a protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void tp(CommandSource context, Integer id)
    {
        Lock lock = this.getLockById(context, id);
        if (lock == null) return;
        if (lock.isBlockLock())
        {
            ((Player)context).setLocation(lock.getFirstLocation());
        }
        else
        {
            i18n.send(context, NEGATIVE, "You cannot teleport to an entity protection!");
        }
    }

    @Command(desc = "Deletes all locks of given player")
    public void purge(CommandSource context, User player)
    {
        manager.purgeLocksFrom(player).thenAccept(i -> {
            if (i != 0)
            {
              i18n.send(context, POSITIVE, "All locks for {user} are now deleted!", player);
            }
            else
            {
              i18n.send(context, NEUTRAL, "{user} had no locks!", player);
            }
        });
    }

    @Command(desc = "Deletes old locks")
    public void cleanup(CommandSource context)
    {
        manager.purgeOldLocks().thenAccept(i -> {
            if (i != 0)
            {
                i18n.send(context, POSITIVE, "{amount} locks deleted!", i);
            }
            else
            {
                i18n.send(context, NEUTRAL, "No locks deleted!");
            }
        });
    }

    // TODO list cmd
    public void list(CommandSource context)
    {
        // find & show all protections of a user/selection
    }

    // TODO reload cmd
     /*
        this.onDisable();
        this.config = this.loadConfig(LockerConfig.class);
        Database db = this.getCore().getDB();
        db.registerTable(TableLocks.class);
        db.registerTable(TableLockLocations.class);
        db.registerTable(TableAccessList.class);
        manager = new LockManager(this);
        LockerCommands lockerCmd = new LockerCommands(this, manager);
        this.getCore().getCommandManager().addCommand(lockerCmd);
        lockerCmd.addCommand(new LockerCreateCommands(this, manager));
        lockerCmd.addCommand(new LockerAdminCommands(this, manager));
        listener = new LockerListener(this, manager);
        */
}
