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
package org.cubeengine.module.vanillaplus.fix;

import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.service.permission.PermissionDescription;

/**
 * Persists Player flymode on logout so they don't fall out of the sky
 * <p></p>
 * Prevents placing overstacked items into anvil or brewingstands
 */
public class FixListener extends PermissionContainer<VanillaPlus>
{
    public FixListener(VanillaPlus module)
    {
        super(module);
    }

    public final PermissionDescription OVERSTACKED_ANVIL_AND_BREWING = register("allow-overstacked-anvil-and-brewing", "", null);

    /* TODO is this still needed?
    @Listener(order = EARLY)
    public void join(final ClientConnectionEvent.Join event)
    {
        // TODO set persisted flymode
        Player player = event.getTargetEntity();
        if (player.getGameModeData().type().get() != CREATIVE &&
            module.perms().COMMAND_FLY_KEEP.isAuthorized(player))
        {
            player.offer(player.getOrCreate(FlyingData.class).get());
        }
    }

    @Listener(order = FIRST)
    public void quit(final ClientConnectionEvent.Disconnect event)
    {
        Player player = event.getTargetEntity();
        if (player.getGameModeData().type().get() != CREATIVE &&
            player.getData(FlyingData.class).isPresent() && module.perms().COMMAND_FLY_KEEP.isAuthorized(player))
        {
            // TODO set persisted flymode
        }
    }
    */

    @Listener
    public void onPlayerInventoryClick(ClickInventoryEvent event, @First Player player)
    {
        // TODO is this still needed?
        if (this.module.getConfig().fix.preventOverstackedItems &&
            !player.hasPermission(OVERSTACKED_ANVIL_AND_BREWING.getId()))
        {
            /* TODO
            if (event.getView().getTopInventory() instanceof AnvilInventory
                || event.getView().getTopInventory() instanceof BrewerInventory)
            {
                boolean topClick = event.getRawSlot() < event.getView().getTopInventory().getSize();
                switch (event.getAction())
                {
                    case PLACE_ALL:
                    case PLACE_SOME:
                        if (!topClick) return;
                        if (event.getCursor().getAmount() > event.getCursor().getMaxStackSize())
                        {
                            event.setCancelled(true);
                        }
                        break;
                    case MOVE_TO_OTHER_INVENTORY:
                        if (topClick) return;
                        if (event.getCurrentItem().getAmount() > event.getCurrentItem().getMaxStackSize())
                        {
                            event.setCancelled(true);
                        }
                }
            }
            */
        }
    }
}
