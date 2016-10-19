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
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 * <p>/clearinventory
 * <p>/stash
 */
public class InvseeCommand extends PermissionContainer
{
    private InventoryGuardFactory invGuard;
    private I18n i18n;

    public InvseeCommand(PermissionManager pm, InventoryGuardFactory invGuard, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.invGuard = invGuard;
        this.i18n = i18n;
    }

    private final Permission COMMAND_INVSEE = register("command.invsee", "", null);
    public final Permission COMMAND_INVSEE_ENDERCHEST = register("ender", "", COMMAND_INVSEE);
    public final Permission COMMAND_INVSEE_MODIFY = register("modify.allow", "Allows to modify the inventory of other players", COMMAND_INVSEE);
    public final Permission COMMAND_INVSEE_MODIFY_PREVENT = register("modify.prevent", "Prevents an inventory from being modified unless forced", COMMAND_INVSEE);
    public final Permission COMMAND_INVSEE_MODIFY_FORCE = register("modify.force", "Allows modifying an inventory even if the player has the prevent permission", COMMAND_INVSEE);
    public final Permission COMMAND_INVSEE_NOTIFY = register("notify", "Notifies you when someone is looking into your inventory", COMMAND_INVSEE);
    public final Permission COMMAND_INVSEE_QUIET = register("quiet", "Prevents the other player from being notified when looking into his inventory", COMMAND_INVSEE);

    @Command(desc = "Allows you to see into the inventory of someone else.")
    @Restricted(value = Player.class, msg = "This command can only be used by a player!")
    public void invsee(Player context, User player,
                       @Flag boolean force,
                       @Flag boolean quiet,
                       @Flag boolean ender
                      )
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
            inv = player.getPlayer().map(p -> p.getEnderChestInventory()).orElse(null);
            if (inv == null)
            {
                context.sendMessage(Text.of(TextColors.DARK_RED, "Offline Inventories are not yet supported! Waiting for API"));
                // TODO Offline Enderchest
                return;
            }
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
                i18n.sendTranslated(player.getPlayer().get(), NEUTRAL, "{sender} is looking into your inventory.", context);
            }
        }
        InventoryGuardFactory guard = invGuard.prepareInv(inv, player.getUniqueId());
        if (denyModify)
        {
            guard.blockPutInAll().blockTakeOutAll();
        }
        guard.submitInventory(VanillaPlus.class, true);

    }
}
