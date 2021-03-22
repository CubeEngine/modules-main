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
package org.cubeengine.module.protector.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent.Reposition;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.spongepowered.api.util.Tristate.FALSE;
import static org.spongepowered.api.util.Tristate.UNDEFINED;

@Singleton
public class MoveSettingsListener extends PermissionContainer
{
    public final Map<MoveType, Permission> movePerms = new HashMap<>();

    private RegionManager manager;
    private I18n i18n;

    @Inject
    public MoveSettingsListener(RegionManager manager, PermissionManager pm, I18n i18n)
    {
        super(pm, Protector.class);
        this.manager = manager;
        this.i18n = i18n;

        // TODO ENTER remember last valid pos.
        movePerms.put(MoveType.MOVE, this.register("bypass.move.move", "Region bypass for moving in a region"));
        movePerms.put(MoveType.EXIT, this.register("bypass.move.exit", "Region bypass for exiting a region"));
        movePerms.put(MoveType.ENTER, this.register("bypass.move.enter", "Region bypass for entering a region"));
        movePerms.put(MoveType.TELEPORT, this.register("bypass.move.teleport", "Region bypass for teleport in a region"));
        movePerms.put(MoveType.TELEPORT_PORTAL, this.register("bypass.move.teleport-portal", "Region bypass for teleport using portals in a region"));
    }

    public enum MoveType
    {
        MOVE, ENTER, EXIT, TELEPORT, TELEPORT_PORTAL
    }

    @Listener(order = Order.EARLY)
    public void onMove(MoveEntityEvent event, @Getter("entity") ServerPlayer player)
    {
        // Ignore subblock movements
        if (event.destinationPosition().toInt().equals(event.originalPosition().toInt()))
        {
            return;
        }

        if (event instanceof ChangeEntityWorldEvent.Reposition)
        {
            List<Region> from = manager.getRegionsAt(((Reposition)event).originalWorld(), event.originalPosition().toInt());
            List<Region> to = manager.getRegionsAt(((Reposition)event).destinationWorld(), event.destinationPosition().toInt());
            if (from.isEmpty() && to.isEmpty())
            {
                return;
            }

            // If no region has a Teleport setting set ignore this.
            if (from.stream().anyMatch(region -> region.getSettings().move.getOrDefault(MoveType.TELEPORT, UNDEFINED) != UNDEFINED))
            {
                if (checkMove(event, player, from, to, MoveType.TELEPORT, false))
                {
                    return; // Teleport out denied
                }

                if (checkMove(event, player, to, from, MoveType.TELEPORT, false))
                {
                    return; // Teleport in denied
                }
            }
        }
        else
        {
            List<Region> from = manager.getRegionsAt(player.world(), event.originalPosition().toInt());
            List<Region> to = manager.getRegionsAt(player.world(), event.destinationPosition().toInt());
            if (from.isEmpty() && to.isEmpty())
            {
                return;
            }

            if (from.stream().anyMatch(region -> region.getSettings().move.getOrDefault(MoveType.MOVE, UNDEFINED) != UNDEFINED))
            {
                if (checkMove(event, player, from, to, MoveType.MOVE, false))
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to move in here.");
                    return; // Move in from denied
                }
            }
            if (from.stream().anyMatch(region -> region.getSettings().move.getOrDefault(MoveType.EXIT, UNDEFINED) != UNDEFINED))
            {
                if (checkMove(event, player, from, to, MoveType.EXIT, true))
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to exit this area.");
                    return; // Move out of from denied
                }
            }
            if (from.stream().anyMatch(region -> region.getSettings().move.getOrDefault(MoveType.ENTER, UNDEFINED) != UNDEFINED))
            {
                if (checkMove(event, player, to, from, MoveType.ENTER, true))
                {
                    i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "You are not allowed to enter this area.");
                    return; // Move into to denied
                }
            }
        }
    }

    private boolean checkMove(MoveEntityEvent event, @Root ServerPlayer player, List<Region> source, List<Region> dest, MoveType type, boolean contain)
    {
        Permission perm = this.movePerms.get(type);
        if (!perm.check(player))
        {
            Tristate allow = UNDEFINED;
            for (Region region : source)
            {
                allow = allow.and(region.getSettings().move.getOrDefault(type, UNDEFINED));
                if (allow != UNDEFINED)
                {
                    if (allow == FALSE)
                    {
                        if (!contain || !dest.contains(region))
                        {
                            event.setCancelled(true);
                            return true;
                        }
                    }
                    if (!contain)
                    {
                        break;
                    }
                }
            }
        }
        return false;
    }

}