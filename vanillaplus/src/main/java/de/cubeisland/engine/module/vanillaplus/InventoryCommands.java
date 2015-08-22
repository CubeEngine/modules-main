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
package de.cubeisland.engine.module.vanillaplus;

import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.user.User;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.POSITIVE;

public class InventoryCommands
{
    private VanillaPlus module;

    public InventoryCommands(VanillaPlus module)
    {
        this.module = module;
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
        player.getUser().getInventory().clear();
        org.spongepowered.api.entity.player.User user = player.getOfflinePlayer();
        if (removeArmor)
        {
            user.setBoots(null);
            user.setLeggings(null);
            user.setChestplate(null);
            user.setHelmet(null);
        }
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
