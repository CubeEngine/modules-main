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
package org.cubeengine.module.zoned;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.block.BlockTypes.AIR;

@Singleton
public class ZonedListener
{
    private Map<UUID, ZoneConfig> zonesByPlayer = new HashMap<>();

    private I18n i18n;
    private Permission selectPerm;
    private Reflector reflector;

    @Inject
    public ZonedListener(I18n i18n, PermissionManager pm, Reflector reflector)
    {
        this.i18n = i18n;
        selectPerm = pm.register(Zoned.class, "use-tool", "Allows using the selector tool", null);
        this.reflector = reflector;
    }

    @Listener
    public void onInteract(InteractBlockEvent event, @First ServerPlayer player)
    {
        if (!event.getContext().get(EventContextKeys.USED_HAND).map(
            hand -> hand.equals(HandTypes.MAIN_HAND.get())).orElse(false))
        {
            return;
        }

        final ServerLocation block = player.getWorld().getLocation(event.getBlock().getPosition());
        if (block.getBlockType().isAnyOf(AIR) || !SelectionTool.inHand(player) || !player.hasPermission(
            selectPerm.getId()))
        {
            return;
        }

        ZoneConfig config = getZone(player);
        if (config.world == null)
        {
            config.world = new ConfigWorld(player.getWorld());
        }
        else
        {
            if (config.world.getWorld() != player.getWorld())
            {
                i18n.send(player, NEUTRAL, "Position in new World detected. Clearing all previous Positions.");
                config.clear();
            }
        }

        Component added = config.addPoint(i18n, player, event instanceof InteractBlockEvent.Primary, block.getPosition());
        Component selected = config.getSelected(i18n, player);

        i18n.send(player, POSITIVE, "{txt} ({integer}, {integer}, {integer}). {txt}", added, block.getBlockX(),
                  block.getBlockY(), block.getBlockZ(), selected);
        if (event instanceof Cancellable)
        {
            ((Cancellable)event).setCancelled(true);
        }

    }

    public ZoneConfig getZone(ServerPlayer player)
    {
        return zonesByPlayer.computeIfAbsent(player.getUniqueId(), k -> reflector.create(ZoneConfig.class));
    }

    public void setZone(ServerPlayer player, ZoneConfig zone)
    {
        zonesByPlayer.put(player.getUniqueId(), zone.clone(reflector));
    }
}
