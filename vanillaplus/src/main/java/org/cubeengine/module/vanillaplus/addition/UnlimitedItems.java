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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class UnlimitedItems
{
    private I18n i18n;
    private Set<UUID> unlimitedPlayers = new HashSet<>();

    public UnlimitedItems(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Grants unlimited items")
    @Restricted(Player.class)
    public void unlimited(Player context, @Optional boolean unlimited)
    {
        if (unlimited)
        {
            i18n.sendTranslated(context, POSITIVE, "You now have unlimited items to build!");
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "You no longer have unlimited items to build!");
        }
        if (unlimited)
        {
            this.unlimitedPlayers.add(context.getUniqueId());
        }
        else
        {
            this.unlimitedPlayers.remove(context.getUniqueId());
        }
    }

    @Listener
    public void blockplace(final ChangeBlockEvent.Place event, @First Player player)
    {
        if (this.unlimitedPlayers.contains(player.getUniqueId()))
        {
            ItemStack item = player.getItemInHand().orElse(null);
            if (item != null)
            {
                item.setQuantity(item.getQuantity() + 1);
                player.setItemInHand(item);
            }
        }
    }
}
