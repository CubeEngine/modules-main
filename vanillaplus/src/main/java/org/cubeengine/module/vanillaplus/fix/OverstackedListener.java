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
package org.cubeengine.module.vanillaplus.fix;

import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.permission.PermissionContainer;
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
public class OverstackedListener extends PermissionContainer
{
    private final VanillaPlus module;

    public OverstackedListener(PermissionManager pm, VanillaPlus module)
    {
        super(pm, VanillaPlus.class);
        this.module = module;
    }

    public final Permission OVERSTACKED_ANVIL_AND_BREWING = register("allow-overstacked-anvil-and-brewing", "", null);



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
