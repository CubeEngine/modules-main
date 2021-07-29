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
package org.cubeengine.module.vanillaplus.improvement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.entity.StandardInventory;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * <p>/clearinventory
 */
@Singleton
public class ClearInventoryCommand extends PermissionContainer
{
    public final Permission COMMAND_CLEARINVENTORY_OTHER = register("command.clearinventory.notify",
                                                                    "Allows clearing the inventory of other players", null);
    public final Permission COMMAND_CLEARINVENTORY_NOTIFY = register("command.clearinventory.other",
                                                                     "Notifies you if your inventory got cleared by someone else", null);
    public final Permission COMMAND_CLEARINVENTORY_PREVENT = register("command.clearinventory.prevent",
                                                                      "Prevents your inventory from being cleared unless forced", null);
    public final Permission COMMAND_CLEARINVENTORY_FORCE = register("command.clearinventory.force",
                                                                    "Clears an inventory even if the player has the prevent PermissionDescription", null);

    private I18n i18n;

    @Inject
    public ClearInventoryCommand(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }


    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    public void clearinventory(CommandCause context, @Default User player,
                               @Flag(longName = "armor", value = "a") boolean removeArmor,
                               @ParameterPermission // TODO permission description (value = "quiet", desc = "Prevents the other player being notified when his inventory got cleared")
                                    @Flag boolean quiet,
                               @Flag boolean force)
    {
        //sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
        boolean self = context.identifier().equals(player.identifier());
        if (!self)
        {
            if (!context.hasPermission(COMMAND_CLEARINVENTORY_OTHER.getId()))
            {
                i18n.send(context, NEGATIVE, "Permission denied");
                return;
            }
            if (player.hasPermission(COMMAND_CLEARINVENTORY_PREVENT.getId())
                && !(force && context.hasPermission(COMMAND_CLEARINVENTORY_FORCE.getId())))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to clear the inventory of {user}", player);
                return;
            }
        }

        StandardInventory playerInventory = player.isOnline() ? player.player().get().inventory() : (StandardInventory) player.inventory();
        playerInventory.primary().clear();
        if (removeArmor)
        {
            playerInventory.armor().clear();
            playerInventory.offhand().clear();
        }
        if (self)
        {
            i18n.send(context, POSITIVE, "Your inventory has been cleared!");
            return;
        }
        if (player.isOnline() && player.hasPermission(COMMAND_CLEARINVENTORY_NOTIFY.getId()) && !quiet)
        {
            i18n.send(player.player().get(), NEUTRAL, "Your inventory has been cleared by {sender}!", context);
        }
        i18n.send(context, POSITIVE, "The inventory of {user} has been cleared!", player);
    }
}
