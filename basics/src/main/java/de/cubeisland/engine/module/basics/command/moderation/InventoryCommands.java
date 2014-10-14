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

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.InventoryGuardFactory;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsAttachment;

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
    @Params(positional = @Param(label = "player", type = User.class))
    @Flags({@Flag(longName = "force", name = "f"),
            @Flag(longName = "quiet", name = "q"),
            @Flag(longName = "ender", name = "e")})
    public void invsee(CommandContext context)
    {
        if (context.getSource() instanceof User)
        {
            User sender = (User)context.getSource();
            User user = context.get(0);
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "User {user} not found!", context.get(0));
                return;
            }
            boolean denyModify = false;
            Inventory inv;
            if (context.hasFlag("e"))
            {
                if (module.perms().COMMAND_INVSEE_ENDERCHEST.isAuthorized(sender))
                {
                    inv = user.getEnderChest();
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to look into enderchests!");
                    return;
                }
            }
            else
            {
                inv = user.getInventory();
            }
            if (module.perms().COMMAND_INVSEE_MODIFY.isAuthorized(sender))
            {
                denyModify = !( context.hasFlag("f")
                    && module.perms().COMMAND_INVSEE_MODIFY_FORCE.isAuthorized(sender))
                    && module.perms().COMMAND_INVSEE_MODIFY_PREVENT.isAuthorized(user);
            }
            if (module.perms().COMMAND_INVSEE_NOTIFY.isAuthorized(user))
            {
                if (!(context.hasFlag("q") && module.perms().COMMAND_INVSEE_QUIET.isAuthorized(context.getSource())))
                {
                    user.sendTranslated(NEUTRAL, "{sender} is looking into your inventory.", sender);
                }
            }
            InventoryGuardFactory guard = InventoryGuardFactory.prepareInventory(inv, sender);
            if (denyModify)
            {
                guard.blockPutInAll().blockTakeOutAll();
            }
            guard.submitInventory(this.module, true);
            return;
        }
        context.sendTranslated(NEGATIVE, "This command can only be used by a player!");
    }

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    public void stash(CommandContext context)
    {
        if (context.getSource() instanceof User)
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
            return;
        }
        context.sendTranslated(NEGATIVE, "Yeah you better put it away!");
    }

    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    @Params(positional = @Param(req = false, label = "player", type = User.class))
    @Flags({@Flag(longName = "removeArmor", name = "ra"),
            @Flag(longName = "quiet", name = "q")})
    @SuppressWarnings("deprecation")
    public void clearinventory(CommandContext context)
    {
        CommandSender sender = context.getSource();
        final User target;
        if (context.hasPositional(0))
        {
            target = context.get(0);
        }
        else if (sender instanceof User)
        {
            target = (User)sender;
        }
        else
        {
            sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
            return;
        }
        if (!sender.equals(target))
        {
            context.ensurePermission(module.perms().COMMAND_CLEARINVENTORY_OTHER);
            if (module.perms().COMMAND_CLEARINVENTORY_PREVENT.isAuthorized(target) && !(context.hasFlag("f")
                && module.perms().COMMAND_CLEARINVENTORY_FORCE.isAuthorized(sender)))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to clear the inventory of {user}", target);
                return;
            }
        }
        target.getInventory().clear();
        if (context.hasFlag("ra"))
        {
            target.getInventory().setBoots(null);
            target.getInventory().setLeggings(null);
            target.getInventory().setChestplate(null);
            target.getInventory().setHelmet(null);
        }
        target.updateInventory();
        if (sender == target)
        {
            sender.sendTranslated(POSITIVE, "Your inventory has been cleared!");
            return;
        }
        if (module.perms().COMMAND_CLEARINVENTORY_NOTIFY.isAuthorized(target)) // notify
        {
            if (!(module.perms().COMMAND_CLEARINVENTORY_QUIET.isAuthorized(sender) && context.hasFlag("q"))) // quiet
            {
                target.sendTranslated(NEUTRAL, "Your inventory has been cleared by {sender}!", sender);
            }
        }
        sender.sendTranslated(POSITIVE, "The inventory of {user} has been cleared!", target);
    }
}
