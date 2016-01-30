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
package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.command.exception.PermissionDeniedException;
import org.cubeengine.service.user.User;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class ClearInventoryCommand
{
    private VanillaPlus module;

    public ClearInventoryCommand(VanillaPlus module)
    {
        this.module = module;
    }

    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    public void clearinventory(CommandSource context, @Default User player,
                               @Flag(longName = "removeArmor", name = "ra") boolean removeArmor,
                               @Flag boolean quiet,
                               @Flag boolean force)
    {
        //sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
        if (!context.getIdentifier().equals(player.getIdentifier()))
        {
            if (!context.hasPermission(module.perms().COMMAND_CLEARINVENTORY_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.perms().COMMAND_CLEARINVENTORY_OTHER);
            }
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
