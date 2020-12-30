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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class UnlimitedItems
{
    private I18n i18n;
    private Set<UUID> unlimitedPlayers = new HashSet<>();

    @Inject
    public UnlimitedItems(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Grants unlimited items")
    @Restricted
    public void unlimited(ServerPlayer context, @Option Boolean unlimited)
    {
        if (unlimited == null)
        {
            unlimited = !unlimitedPlayers.contains(context.getUniqueId());
        }
        if (unlimited)
        {
            i18n.send(context, POSITIVE, "You now have unlimited items to build!");
        }
        else
        {
            i18n.send(context, NEUTRAL, "You no longer have unlimited items to build!");
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
    public void blockplace(final ChangeBlockEvent.All event, @First Player player)
    {

        if (this.unlimitedPlayers.contains(player.getUniqueId()))
        {
            if (!event.getTransactions(Operations.PLACE.get()).findAny().isPresent())
            {
                return;
            }
            if (player.get(Keys.GAME_MODE).get() == GameModes.CREATIVE.get())
            {
                return;
            }
            ItemStack item = player.getItemInHand(HandTypes.MAIN_HAND);
            item.setQuantity(item.getQuantity() + 1);
            player.setItemInHand(HandTypes.MAIN_HAND, item);
        }
    }
}
