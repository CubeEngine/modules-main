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
package org.cubeengine.module.vanillaplus.addition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 */
@Singleton
public class InvseeCommand extends PermissionContainer
{
    private I18n i18n;

    @Inject
    public InvseeCommand(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    public final Permission COMMAND_INVSEE_ENDERCHEST = register("command.invsee.ender", "Allows to look at someones enderchest", null);
    public final Permission COMMAND_INVSEE_MODIFY = register("command.invsee.modify.allow", "Allows to modify the inventory of other players", null);
    public final Permission COMMAND_INVSEE_MODIFY_PREVENT = register("command.invsee.modify.prevent", "Prevents an inventory from being modified unless forced", null);
    public final Permission COMMAND_INVSEE_MODIFY_FORCE = register("command.invsee.modify.force", "Allows modifying an inventory even if the player has the prevent permission", null);
    public final Permission COMMAND_INVSEE_NOTIFY = register("command.invsee.notify", "Notifies you when someone is looking into your inventory", null);
    public final Permission COMMAND_INVSEE_QUIET = register("command.invsee.quiet", "Prevents the other player from being notified when looking into his inventory", null);

    @Command(desc = "Allows you to see into the inventory of someone else.")
    @Restricted(msg = "This command can only be used by a player!")
    public void invsee(ServerPlayer context, User player,
                       @Flag boolean force,
                       @Flag boolean quiet,
                       @Flag boolean ender)
    {
        boolean denyModify = false;
        Inventory inv;
        if (ender)
        {

            if (!COMMAND_INVSEE_ENDERCHEST.check(context))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to look into enderchests!");
                return;
            }
            inv = player.getEnderChestInventory();
        }
        else
        {
            inv = player.getInventory();
        }
        if (context.hasPermission(COMMAND_INVSEE_MODIFY.getId()))
        {
            denyModify = !(force && context.hasPermission(COMMAND_INVSEE_MODIFY_FORCE.getId()))
                && player.hasPermission(COMMAND_INVSEE_MODIFY_PREVENT.getId());
        }
        if (player.isOnline() && player.hasPermission(COMMAND_INVSEE_NOTIFY.getId()))
        {
            if (!(quiet && context.hasPermission(COMMAND_INVSEE_QUIET.getId())))
            {
                i18n.send(player.getPlayer().get(), NEUTRAL, "{sender} is looking into your inventory.", context);
            }
        }

        final InventoryMenu menu = inv.asViewable().get().asMenu();
        if (denyModify)
        {
            menu.setReadOnly(true);
        }
        menu.open(context);
    }
}
