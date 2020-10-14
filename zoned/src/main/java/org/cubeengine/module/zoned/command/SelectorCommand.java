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
package org.cubeengine.module.zoned.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.SpawnUtil;
import org.cubeengine.module.zoned.SelectionTool;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryTypes;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class SelectorCommand
{

    private I18n i18n;

    @Inject
    public SelectorCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    public void giveSelectionTool(ServerPlayer player)
    {
        ItemStack found = null;
        Inventory axes = player.getInventory().query(QueryTypes.ITEM_TYPE, ItemTypes.COAL);
        for (Inventory slot : axes.slots())
        {
            if (SelectionTool.isTool(slot.peek()))
            {
                found = slot.peek();
                slot.clear();
                break;
            }
        }

        final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if (found == null)
        {
            found = SelectionTool.newTool(player);
            player.setItemInHand(HandTypes.MAIN_HAND, found);
            if (!itemInHand.isEmpty())
            {
                if (player.getInventory().offer(itemInHand).revertOnFailure())
                {
                    SpawnUtil.spawnItem(itemInHand, player.getServerLocation());
                }
            }
            i18n.send(player, POSITIVE, "Received a new region selector tool");
            return;
        }

        player.setItemInHand(HandTypes.MAIN_HAND, found);
        player.getInventory().offer(itemInHand);
        i18n.send(player, POSITIVE, "Found a region selector tool in your inventory!");
    }

    @Command(desc = "Provides you with a wand to select a cuboid")
    //    @Restricted(value = ServerPlayer.class, msg =  "You cannot hold a selection tool!")
    public void selectiontool(ServerPlayer context)
    {
        giveSelectionTool(context);
    }
}
