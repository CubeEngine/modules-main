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
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.InventoryGuardFactory;
import org.cubeengine.module.basics.Basics;
import org.cubeengine.module.basics.BasicsAttachment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 * <p>/clearinventory
 * <p>/stash
 */
public class InventoryCommands
{
    private final Basics module;
    private InventoryGuardFactory invGuard;

    public InventoryCommands(Basics module, InventoryGuardFactory invGuard)
    {
        this.module = module;
        this.invGuard = invGuard;
    }

    @Command(desc = "Allows you to see into the inventory of someone else.")
    @Restricted(value = User.class, msg = "This command can only be used by a player!")
    public void invsee(CommandContext context, User player,
                       @Flag boolean force,
                       @Flag boolean quiet,
                       @Flag boolean ender)
    {
        User sender = (User)context.getSource();
        boolean denyModify = false;
        Inventory inv;
        if (ender)
        {
            if (!module.perms().COMMAND_INVSEE_ENDERCHEST.isAuthorized(sender))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to look into enderchests!");
                return;
            }
            inv = player.getEnderChest();
        }
        else
        {
            inv = player.getInventory();
        }
        if (module.perms().COMMAND_INVSEE_MODIFY.isAuthorized(sender))
        {
            denyModify = !(force && module.perms().COMMAND_INVSEE_MODIFY_FORCE.isAuthorized(sender))
                && module.perms().COMMAND_INVSEE_MODIFY_PREVENT.isAuthorized(player);
        }
        if (module.perms().COMMAND_INVSEE_NOTIFY.isAuthorized(player))
        {
            if (!(quiet && module.perms().COMMAND_INVSEE_QUIET.isAuthorized(context.getSource())))
            {
                player.sendTranslated(NEUTRAL, "{sender} is looking into your inventory.", sender);
            }
        }
        InventoryGuardFactory guard = invGuard.prepareInventory(inv, sender);
        if (denyModify)
        {
            guard.blockPutInAll().blockTakeOutAll();
        }
        guard.submitInventory(this.module, true);
    }

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    @Restricted(value = User.class, msg = "Yeah you better put it away!")
    public void stash(CommandContext context)
    {
        User sender = (User)context.getSource();
        ItemStack[] stashedInv = sender.get(BasicsAttachment.class).getStashedInventory();
        ItemStack[] stashedArmor = sender.get(BasicsAttachment.class).getStashedArmor();
        ItemStack[] invToStash = sender.getInventory().getContents().clone();
        ItemStack[] armorToStash = sender.getInventory().getArmorContents().clone();
        if (stashedInv != null)
        {
            sender.getInventory().setContents(stashedInv);
        }
        else
        {
            sender.getInventory().clear();
        }

        sender.get(BasicsAttachment.class).setStashedInventory(invToStash);
        if (stashedArmor != null)
        {
            sender.getInventory().setBoots(stashedArmor[0]);
            sender.getInventory().setLeggings(stashedArmor[1]);
            sender.getInventory().setChestplate(stashedArmor[2]);
            sender.getInventory().setHelmet(stashedArmor[3]);
        }
        else
        {
            sender.getInventory().setBoots(null);
            sender.getInventory().setLeggings(null);
            sender.getInventory().setChestplate(null);
            sender.getInventory().setHelmet(null);
        }
        sender.get(BasicsAttachment.class).setStashedArmor(armorToStash);
        sender.sendTranslated(POSITIVE, "Swapped stashed Inventory!");
    }


}
