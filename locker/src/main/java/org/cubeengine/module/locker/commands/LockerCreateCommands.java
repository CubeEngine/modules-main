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

import javax.inject.Inject;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.locker.Locker;
import org.cubeengine.module.locker.storage.Lock;
import org.cubeengine.module.locker.storage.LockManager;
import org.cubeengine.module.locker.storage.LockType;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.locker.storage.LockType.*;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "create", desc = "Creates various protections")
public class LockerCreateCommands extends ContainerCommand
{
    private final LockManager manager;
    private I18n i18n;

    @Inject
    public LockerCreateCommands(CommandManager base, LockManager manager, I18n i18n)
    {
        super(base, Locker.class);
        this.manager = manager;
        this.i18n = i18n;
    }

    private void setCreateProtection(Player sender, LockAction.LockCreateAction lockAction, boolean createKeyBook)
    {
        this.manager.commandListener.submitLockAction(sender, lockAction);
        if (createKeyBook)
        {
            i18n.sendTranslated(sender, POSITIVE, "Right click the item you want to protect with a book in your hand!");
        }
        else
        {
            i18n.sendTranslated(sender, POSITIVE, "Right click the item you want to protect!");
        }
    }

    @Alias(value = "cprivate")
    @Command(name = "private", desc = "creates a private protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cPrivate(Player context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, password, keybook, lock, location, entity, PRIVATE), keybook);
    }

    private void protect(Player context, String password, boolean keybook, Lock lock, Location<World> location, Entity entity, LockType lockType)
    {
        if (lock != null)
        {
            i18n.sendTranslated(context, NEUTRAL, "There is already protection here!");
            return;
        }
        if (entity != null)
        {
            protectEntity(context, password, keybook, entity, lockType);
        }
        else
        {
            protectBlock(context, password, keybook, location, lockType);
        }
    }

    private void protectBlock(Player context, String password, boolean keybook, Location<World> location, LockType lockType)
    {
        if (!manager.canProtect(location.getBlockType()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You cannot protect this block!");
            return;
        }
        if (canProtectCarrier(context, lockType, location.getTileEntity().map(te -> te instanceof Carrier).orElse(false))) return;
        manager.createLock(location, context, lockType, password, keybook);
    }

    private void protectEntity(Player context, String password, boolean keybook, Entity entity, LockType lockType)
    {
        if (!manager.canProtect(entity.getType()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You cannot protect this entity!");
            return;
        }
        if (canProtectCarrier(context, lockType, entity instanceof Carrier)) return;
        manager.createLock(entity, context, lockType, password, keybook);
    }

    private boolean canProtectCarrier(Player context, LockType lockType, boolean isCarrier)
    {
        if (!isCarrier && (lockType == LockType.DONATION || lockType == LockType.FREE || lockType == LockType.GUARDED))
        {
            i18n.sendTranslated(context, NEUTRAL, "You can only apply guarded, donation and free protections to inventory holders!");
            return true;
        }
        return false;
    }


    @Alias(value = "cpublic")
    @Command(name = "public", desc = "creates a public protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cPublic(Player context)
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, null, false, lock, location, entity, PUBLIC), false);
    }

    @Alias(value = "cdonation")
    @Command(name = "donation", desc = "creates a donation protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cDonation(Player context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, password, keybook, lock, location, entity, DONATION), false);
    }

    @Alias(value = "cfree")
    @Command(name = "free", desc = "creates a free protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cFree(Player context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook)
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, password, keybook, lock, location, entity, FREE), false);
    }

    @Alias(value = "cpassword")
    @Command(name = "password", desc = "creates a donation protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cPassword(Player context, String password, @Flag(name = "key", longName = "keybook") boolean keybook) // same as private but with pw
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, password, keybook, lock, location, entity, PRIVATE), false);
    }

    @Alias(value = "cguarded")
    @Command(name = "guarded", desc = "creates a guarded protection")
    @Restricted(value = Player.class, msg = "This command can only be used in game")
    public void cguarded(Player context, @Optional String password, @Flag(name = "key", longName = "keybook") boolean keybook) // same as private but with pw
    {
        this.setCreateProtection(context, (lock, location, entity) ->
                protect(context, password, keybook, lock, location, entity, GUARDED), false);
    }
}
