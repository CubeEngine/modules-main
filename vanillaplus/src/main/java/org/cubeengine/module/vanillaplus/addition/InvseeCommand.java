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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.StandardInventory;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

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
        ViewableInventory viewable;
        boolean canModify = COMMAND_INVSEE_MODIFY.check(context) && ((force && COMMAND_INVSEE_MODIFY_FORCE.check(context)) || !COMMAND_INVSEE_MODIFY_PREVENT.check(player));
        if (player.isOnline() && player.hasPermission(COMMAND_INVSEE_NOTIFY.getId()))
        {
            if (!(quiet && context.hasPermission(COMMAND_INVSEE_QUIET.getId())))
            {
                if (ender)
                {
                    i18n.send(player.player().get(), NEUTRAL, "{sender} is looking into your ender chest.", context);
                }
                else
                {
                    i18n.send(player.player().get(), NEUTRAL, "{sender} is looking into your inventory.", context);
                }
            }
        }
        if (ender)
        {

            if (!COMMAND_INVSEE_ENDERCHEST.check(context))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to look into enderchests!");
                return;
            }
            viewable = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3)
                                        .slots(player.enderChestInventory().slots(), 0)
                                        .completeStructure()
                                        .build();
        }
        else
        {
            final ItemStack barrier = ItemStack.of(ItemTypes.BARRIER);
            barrier.offer(Keys.CUSTOM_NAME, Component.text("Unused Slot", NamedTextColor.BLACK));
            final StandardInventory playerInventory = player.isOnline() ? player.player().get().inventory() : player.inventory();
            viewable = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X5)
                                        .slots(playerInventory.armor().slots(), 0)
                                        .dummySlots(4, 4).item(barrier.createSnapshot())
                                        .slots(playerInventory.offhand().slots(), 8)
                                        .slots(playerInventory.storage().slots(), 9)
                                        .slots(playerInventory.hotbar().slots(), 4*9)
                                        .completeStructure()
                                        .build();
        }

        final InventoryMenu menu = viewable.asMenu();
        if (!canModify)
        {
            menu.setReadOnly(true);
        }
        else if (!ender)
        {
            menu.registerSlotClick((cause, container, slot, slotIndex, clickType) -> !(slotIndex >= 4 && slotIndex < 8));
            menu.registerChange((cause, container, slot, slotIndex, oldStack, newStack) -> !(slotIndex >= 4 && slotIndex < 8));
        }
        menu.open(context);
    }

}
