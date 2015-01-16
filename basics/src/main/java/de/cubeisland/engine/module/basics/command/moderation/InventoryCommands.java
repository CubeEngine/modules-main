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
package de.cubeisland.engine.module.basics.command.moderation;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.InventoryGuardFactory;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * Contains commands that allow to modify an inventory.
 * <p>/invsee
 * <p>/clearinventory
 * <p>/stash
 */
public class InventoryCommands
{
    private final Basics module;

    public InventoryCommands(Basics module)
    {
        this.module = module;
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
        InventoryGuardFactory guard = InventoryGuardFactory.prepareInventory(inv, sender);
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

    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    @SuppressWarnings("deprecation")
    public void clearinventory(CommandContext context, @Default User player,
                               @Flag(longName = "removeArmor", name = "ra") boolean removeArmor,
                               @Flag boolean quiet,
                               @Flag boolean force)
    {
        //sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
        CommandSender sender = context.getSource();
        if (!sender.equals(player))
        {
            context.ensurePermission(module.perms().COMMAND_CLEARINVENTORY_OTHER);
            if (module.perms().COMMAND_CLEARINVENTORY_PREVENT.isAuthorized(player)
                && !(force && module.perms().COMMAND_CLEARINVENTORY_FORCE.isAuthorized(sender)))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to clear the inventory of {user}", player);
                return;
            }
        }
        player.getInventory().clear();
        if (removeArmor)
        {
            player.getInventory().setBoots(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setHelmet(null);
        }
        player.updateInventory();
        if (sender.equals(player))
        {
            sender.sendTranslated(POSITIVE, "Your inventory has been cleared!");
            return;
        }
        if (module.perms().COMMAND_CLEARINVENTORY_NOTIFY.isAuthorized(player)) // notify
        {
            if (!(module.perms().COMMAND_CLEARINVENTORY_QUIET.isAuthorized(sender) && quiet)) // quiet
            {
                player.sendTranslated(NEUTRAL, "Your inventory has been cleared by {sender}!", sender);
            }
        }
        sender.sendTranslated(POSITIVE, "The inventory of {user} has been cleared!", player);
    }
}
