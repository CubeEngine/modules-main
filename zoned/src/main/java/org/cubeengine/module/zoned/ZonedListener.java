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
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent.Primary;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Direction.Division;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
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
    private TaskManager tm;

    @Inject
    public ZonedListener(I18n i18n, PermissionManager pm, Reflector reflector, TaskManager tm)
    {
        this.i18n = i18n;
        selectPerm = pm.register(Zoned.class, "use-tool", "Allows using the selector tool", null);
        this.reflector = reflector;
        this.tm = tm;
    }

    @Listener
    public void onInteract(InteractItemEvent.Secondary event, @First ServerPlayer player)
    {
        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (!ZonedData.isSavedSelection(itemInHand))
        {
            return;
        }
        event.setCancelled(true);

        if (!EventUtil.isMainHand(event.context()))
        {
            return;
        }

        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Recalled selection");
        final ZoneConfig zone = new ZoneConfig();
        zone.world = new ConfigWorld(itemInHand.get(ZonedData.ZONE_WORLD).get().asString());
        final Vector3d min = itemInHand.get(ZonedData.ZONE_MIN).get();
        final Vector3d max = itemInHand.get(ZonedData.ZONE_MAX).get();
        final Vector3d size = max.sub(min);
        zone.shape = new Cuboid(min, size);
        this.setZone(player, zone);
        ShapeRenderer.showActiveZone(tm, player, this::getZone);
    }

    @Listener
    public void onInteract(InteractItemEvent event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()))
        {
            return;
        }

        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (!ZonedData.isTool(itemInHand) || !player.hasPermission(selectPerm.getId()))
        {
            return;
        }
        if (!player.get(Keys.IS_SNEAKING).orElse(false))
        {
            return;
        }

        ZoneConfig config = getZone(player);
        if (config.world == null)
        {
            config.world = new ConfigWorld(player.world());
        }
        else
        {
            if (config.world.getWorld() != player.world())
            {
                i18n.send(player, NEUTRAL, "No Zone selected in this world yet.");
                return;
            }
        }

        final boolean extend = event instanceof InteractItemEvent.Primary;
        this.moveShape(extend, player, config);
    }

    @Listener
    public void onInteract(InteractBlockEvent event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()))
        {
            return;
        }

        final ItemStack itemInHand = player.itemInHand(HandTypes.MAIN_HAND);
        if (!ZonedData.isTool(itemInHand) || !player.hasPermission(selectPerm.getId()))
        {
            return;
        }
        final ServerLocation block = player.world().location(event.block().position());
        if (block.blockType().isAnyOf(AIR))
        {
            return;
        }

        ZoneConfig config = getZone(player);
        if (config.world == null)
        {
            config.world = new ConfigWorld(player.world());
        }
        else
        {
            if (config.world.getWorld() != player.world())
            {
                i18n.send(player, NEUTRAL, "Position in new World detected. Clearing all previous Positions.");
                config.clear();
            }
        }

        if (player.get(Keys.IS_SNEAKING).orElse(false))
        {
            final boolean forward = event instanceof Primary;
            this.moveShape(forward, player, config);
        }
        else
        {
            Component added = config.addPoint(i18n, player, event instanceof InteractBlockEvent.Primary, block.position());
            Component selected = config.getSelected(i18n, player);

            ShapeRenderer.showActiveZone(tm, player, this::getZone);
            i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "{txt} ({integer}, {integer}, {integer}). {txt}", added, block.blockX(),
                      block.blockY(), block.blockZ(), selected);
        }

        if (event instanceof Cancellable)
        {
            ((Cancellable)event).setCancelled(true);
        }
    }

    @Listener
    public void onScrollBar(ChangeInventoryEvent.Held event, @First ServerPlayer player)
    {
        if (player.get(Keys.IS_SNEAKING).orElse(false))
        {
            if (ZonedData.isTool(event.originalSlot().peek()) && player.hasPermission(selectPerm.getId()))
            {
                ZoneConfig config = getZone(player);
                if (config.world == null)
                {
                    config.world = new ConfigWorld(player.world());
                }
                int scrollDirection = this.compareSlots(event.originalSlot().get(Keys.SLOT_INDEX).get(), event.finalSlot().get(Keys.SLOT_INDEX).get());
                if (scrollDirection == 0)
                {
                    return;
                }
                this.expandShape(scrollDirection > 0, player, config);
                event.setCancelled(true);
                return;
            }
        }
        final ItemStack originalStack = event.originalSlot().peek();
        if (ZonedData.isSavedSelection(originalStack) || ZonedData.isTool(originalStack))
        {
            ShapeRenderer.hideActiveZone(player);
        }
        final ItemStack finalStack = event.finalSlot().peek();
        if (ZonedData.isTool(finalStack))
        {
            ShapeRenderer.showActiveZone(tm, player, this::getZone);
        }
    }

    private int compareSlots(int previousSlot, int newSlot)
    {
        if(previousSlot == 8 && newSlot == 0)
        {
            newSlot = 9;
        }
        if(previousSlot == 0 && newSlot == 8)
        {
            newSlot = -1;
        }
        return Integer.compare(previousSlot, newSlot);
    }

    private boolean expandShape(boolean expand, ServerPlayer player, ZoneConfig config)
    {
        if (!config.isComplete())
        {
            i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Cannot extend incomplete selection");
            return true;
        }
        ShapeRenderer.showActiveZone(tm, player, this::getZone);

        final Direction direction = Direction.closest(player.headDirection(), Division.CARDINAL);
        final Shape shape = config.shape;
        if (shape instanceof Cuboid)
        {
            final Vector3d min = ((Cuboid)shape).getMinimumPoint();
            final Vector3d max = ((Cuboid)shape).getMaximumPoint();


            Vector3d newMin = min;
            Vector3d newMax = max;

            final Vector3i directionOffset = direction.asBlockOffset();
            if (directionOffset.getX() > 0)
            {
                newMax = newMax.add(expand ? 1 : -1, 0, 0);
            }
            else if (directionOffset.getX() < 0)
            {
                newMin = newMin.add(expand ? -1 : 1, 0, 0);
            }
            if (directionOffset.getY() > 0)
            {
                newMax = newMax.add(0, expand ? 1 : -1, 0);
            }
            else if (directionOffset.getY() < 0)
            {
                newMin = newMin.add(0, expand ? -1 : 1, 0);
            }
            if (directionOffset.getZ() > 0)
            {
                newMax = newMax.add(0, 0, expand ? 1 : -1);
            }
            else if (directionOffset.getZ() < 0)
            {
                newMin = newMin.add(0, 0, expand ? -1 : 1);
            }
            if (newMax.getX() < newMin.getX() || newMax.getY() < newMin.getY() || newMax.getZ() < newMin.getZ())
            {
                i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Cannot contract anymore in this direction");
                return false;
            }

            config.addPoint(i18n, player, true, newMin);
            config.addPoint(i18n, player, false, newMax);
        }
        else
        {
            i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Cannot extend shapes of type {input#shape}", shape.getClass().getSimpleName());
            return true;
        }

        Component selected = config.getSelected(i18n, player);

        if (expand)
        {
            i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Expanded towards {txt#direction}. {txt#selected}", direction.toString(), selected);
        }
        else
        {
            i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Contracted towards {txt#direction}. {txt#selected}", direction.opposite().toString(), selected);
        }
        return false;
    }

    private boolean moveShape(boolean forward, ServerPlayer player, ZoneConfig config)
    {
        if (!config.isComplete())
        {
            i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Cannot move incomplete selection");
            return true;
        }
        ShapeRenderer.showActiveZone(tm, player, this::getZone);

        final Direction direction = Direction.closest(player.headDirection(), Division.CARDINAL);
        final Shape shape = config.shape;
        if (shape instanceof Cuboid)
        {
            final Vector3d min = ((Cuboid)shape).getMinimumPoint();
            final Vector3d max = ((Cuboid)shape).getMaximumPoint();

            final Vector3d newMin = min.add(direction.asOffset().mul(forward ? 1 : -1));
            final Vector3d newMax = max.add(direction.asOffset().mul(forward ? 1 : -1));

            config.addPoint(i18n, player, true, newMin);
            config.addPoint(i18n, player, false, newMax);
        }
        else
        {
            i18n.send(ChatType.ACTION_BAR, player, NEGATIVE, "Cannot move shapes of type {input#shape}", shape.getClass().getSimpleName());
            return true;
        }

        Component selected = config.getSelected(i18n, player);

        i18n.send(ChatType.ACTION_BAR, player, POSITIVE, "Moved towards {txt#direction}. {txt#selected}", forward ? direction.toString() : direction.opposite().toString(), selected);
        return false;
    }

    public ZoneConfig getZone(ServerPlayer player)
    {
        return zonesByPlayer.computeIfAbsent(player.uniqueId(), k -> reflector.create(ZoneConfig.class));
    }

    public void setZone(ServerPlayer player, ZoneConfig zone)
    {
        zonesByPlayer.put(player.uniqueId(), zone.clone(reflector));
    }
}
