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
package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.InventoryGuardFactory;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.basics.BasicsAttachment;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 * <p>/clearinventory
 * <p>/stash
 */
public class InvseeCommand
{
    private final VanillaPlus module;
    private InventoryGuardFactory invGuard;
    private I18n i18n;

    public InvseeCommand(VanillaPlus module, InventoryGuardFactory invGuard, I18n i18n)
    {
        this.module = module;
        this.invGuard = invGuard;
        this.i18n = i18n;
    }

    private final PermissionDescription COMMAND_INVSEE = COMMAND.childWildcard("invsee");
    /**
     * Allows to modify the inventory of other players
     */
    public final PermissionDescription COMMAND_INVSEE_MODIFY = COMMAND_INVSEE.child("modify");
    public final PermissionDescription COMMAND_INVSEE_ENDERCHEST = COMMAND_INVSEE.child("ender");
    /**
     * Prevents an inventory from being modified unless forced
     */
    public final PermissionDescription COMMAND_INVSEE_MODIFY_PREVENT = COMMAND_INVSEE.newPerm("modify.prevent", FALSE);
    /**
     * Allows modifying an inventory even if the player has the prevent permission
     */
    public final PermissionDescription COMMAND_INVSEE_MODIFY_FORCE = COMMAND_INVSEE.child("modify.force");
    /**
     * Notifies you when someone is looking into your inventory
     */
    public final PermissionDescription COMMAND_INVSEE_NOTIFY = COMMAND_INVSEE.child("notify");
    /**
     * Prevents the other player from being notified when looking into his inventory
     */
    public final PermissionDescription COMMAND_INVSEE_QUIET = COMMAND_INVSEE.child("quiet");

    @Command(desc = "Allows you to see into the inventory of someone else.")
    @Restricted(value = Player.class, msg = "This command can only be used by a player!")
    public void invsee(Player context, User player,
                       @Flag boolean force,
                       @Flag boolean quiet,
                       @Flag boolean ender)
    {
        boolean denyModify = false;
        Inventory inv;
        if (ender)
        {
            if (!context.hasPermission(COMMAND_INVSEE_ENDERCHEST.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to look into enderchests!");
                return;
            }
            inv = player.getEnderChest();
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
        if (player.hasPermission(COMMAND_INVSEE_NOTIFY.getId()))
        {
            if (!(quiet && context.hasPermission(COMMAND_INVSEE_QUIET.getId())))
            {
                i18n.sendTranslated(player, NEUTRAL, "{sender} is looking into your inventory.", context);
            }
        }
        InventoryGuardFactory guard = invGuard.prepareInv(inv);
        if (denyModify)
        {
            guard.blockPutInAll().blockTakeOutAll();
        }
        guard.submitInventory(this.module, true);
    }


}
