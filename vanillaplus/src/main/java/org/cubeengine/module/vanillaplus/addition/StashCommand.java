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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * <p>/stash
 */
@Singleton
public class StashCommand
{
    private I18n i18n;

    private Map<UUID, StashedInventory> stashed = new HashMap<>();

    @Inject
    public StashCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Stashes or unstashes your inventory to reuse later")
    @Restricted(msg = "Yeah you better put it away!")
    public void stash(ServerPlayer context)
    {
        StashedInventory newStash = new StashedInventory();
        for (Inventory slot : context.inventory().slots())
        {
            newStash.items.add(slot.poll().polledItem().createStack());
        }

        StashedInventory replaced = stashed.put(context.uniqueId(), newStash);
        if (replaced != null)
        {
            Iterator<ItemStack> it = replaced.items.iterator();
            for (Inventory inventory : context.inventory().slots())
            {
                ItemStack next = it.next();
                if (next != null)
                {
                    inventory.offer(next);
                }
            }
        }

        i18n.send(context, POSITIVE, "Swapped stashed Inventory!");
    }

    private static class StashedInventory
    {
        public List<ItemStack> items = new ArrayList<>();
    }
}
