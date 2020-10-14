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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.CompositeShape;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.zoned.ShapeRenderer;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.module.zoned.Zoned;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Command(name = "zone", desc = "Manages zones")
@Using(ZoneParser.class)
public class ZonedCommands extends DispatcherCommand
{

    private Zoned module;
    private I18n i18n;
    private TaskManager tm;
    private EventManager em;

    @Inject
    public ZonedCommands(Zoned module, I18n i18n, TaskManager tm, EventManager em)
    {
        super(Zoned.class);
        this.module = module;
        this.tm = tm;
        this.em = em;
        this.i18n = i18n;
    }

    @Command(desc = "Defines a new zone")
    public void define(ServerPlayer context, String name)
    {
        ZoneConfig cfg = this.module.getActiveZone(context);
        module.getManager().define(context, name, cfg, false);
    }

    @Command(desc = "Redefines an existing zone")
    public void redefine(ServerPlayer context, @Default ZoneConfig zone)
    {
        ZoneConfig cfg = this.module.getActiveZone(context);
        zone.shape = cfg.shape;
        zone.name = cfg.name;
        zone.world = cfg.world;
        module.getManager().define(context, zone.name, zone, true);
    }

    @Command(desc = "Selects a zone")
    public void select(ServerPlayer context, ZoneConfig zone)
    {
        this.module.setActiveZone(context, zone);
        i18n.send(context, POSITIVE, "Zone {name} selected!", zone.name);
    }

    @Command(desc = "Lists zones")
    public void list(Audience context, @Option String match, @Named("in") ServerWorld world)
    {
        Collection<ZoneConfig> zones = this.module.getManager().getZones(match, world);
        if (zones.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No Zones found");
            return;
        }
        i18n.send(context, NEUTRAL, "The following zones were found:");
        for (ZoneConfig zone : zones)
        {
            context.sendMessage(Component.empty().append(Component.text(" - ")).append(
                Component.text(zone.world.getName(), GOLD)).append(Component.text(".", WHITE)).append(
                Component.text(zone.name, GOLD)));
        }
    }

    @Command(desc = "Displays zone info")
    public void info(Audience context, @Default ZoneConfig zone)
    {
        if (zone.name == null)
        {
            i18n.send(context, POSITIVE, "Undefined Zone in {name#world}", zone.world.getName());
            return;
        }
        i18n.send(context, POSITIVE, "Zone {name} in {name#world}", zone.name, zone.world.getName());
        // TODO print shape information
    }

    @Command(desc = "Deletes a zone", alias = "remove")
    public void delete(Audience context, ZoneConfig zone)
    {
        this.module.getManager().delete(context, zone);
    }

    @Command(desc = "Toggles particles for the currently selected zone")
    public void show(ServerPlayer context, @Option ZoneConfig zone)
    {
        if (zone != null)
        {
            if (this.module.getActiveZone(context) != zone)
            {
                this.module.setActiveZone(context, zone);
                if (ShapeRenderer.isShowingActiveZone(context))
                {
                    return;
                }
            }
        }
        if (this.module.getActiveZone(context) == null)
        {
            i18n.send(ACTION_BAR, context, NEGATIVE, "You don't have an active zone");
        }
        if (!ShapeRenderer.toggleShowActiveZone(tm, context, module))
        {
            i18n.send(ACTION_BAR, context, POSITIVE, "Stopped showing active zone.");
            return;
        }
        i18n.send(ACTION_BAR, context, POSITIVE, "Started showing active zone.");
    }

    @Command(desc = "Teleports to a zone", alias = "tp")
    public void teleport(ServerPlayer context, ZoneConfig zone, @Flag boolean force)
    {
        Cuboid boundingCuboid = zone.shape.getBoundingCuboid();
        Vector3d middle = boundingCuboid.getMinimumPoint().add(boundingCuboid.getMaximumPoint()).div(2);
        ServerLocation loc = zone.world.getWorld().getLocation(middle);
        GameMode mode = context.get(Keys.GAME_MODE).orElse(null);
        if (mode != GameModes.SPECTATOR)
        {
            int h = (int)boundingCuboid.getHeight() / 2 + 1;
            int w = (int)Math.max(boundingCuboid.getWidth() / 2 + 1, boundingCuboid.getDepth() / 2 + 1);
            java.util.Optional<ServerLocation> adjusted = Sponge.getServer().getTeleportHelper()
                .getSafeLocation(loc, Math.max(h, 5), Math.max(w, 5),
                                 ((int)boundingCuboid.getHeight()),
                                 mode == GameModes.CREATIVE ? TeleportHelperFilters.FLYING.get() : TeleportHelperFilters.DEFAULT.get());
            if (!adjusted.isPresent() && !force)
            {
                i18n.send(ACTION_BAR, context, POSITIVE,
                          "Could not find a safe spot in zone. Use -force to teleport anyways");
                return;
            }
            loc = adjusted.orElse(loc);
        }
        context.setLocation(loc);
        i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to {name}", zone.name);
    }

    @Command(desc = "Creates a zone by following connected redstone")
    public void circuitSelect(ServerPlayer player)
    {
        Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        em.listenUntil(this.module.getClass(), InteractBlockEvent.Secondary.class, e -> e.getCause().root().equals(player), e -> this.circuitSelect(e, player, directions));
        i18n.send(player, POSITIVE, "Select a piece of your redstone circuit.");
    }

    // TODO command for compound zone creation
    // @Command(desc = "Composite Shape zone management")
    public void composite(ServerPlayer context, @Option Integer part)
    {
        if (part == null)
        {
            ZoneConfig zone = module.getActiveZone(context);
            zone.shape = new CompositeShape(zone.shape);
            i18n.send(context, POSITIVE, "Zone converted to composite shape.");
            // TODO select first part
            return;
        }
        // TODO select part
        i18n.send(context, POSITIVE, "Part {number} of composite zone selected.", part);
        i18n.send(context, POSITIVE, "Part {number} does not exist yet", part);
    }

    private boolean circuitSelect(InteractBlockEvent.Secondary e, ServerPlayer player, Set<Direction> directions)
    {
        if (!e.getContext().get(EventContextKeys.USED_HAND).map(h -> h.equals(HandTypes.MAIN_HAND.get())).orElse(false))
        {
            return false;
        }
        e.setCancelled(true);
        final ServerWorld world = player.getWorld();
        Vector3i start = e.getBlock().getPosition();
        if (!isPowering(world, start))
        {
            i18n.send(player, NEGATIVE, "Click a redstone powered block!");
            return true;
        }
        Set<Vector3i> knownLocations = new HashSet<>();
        Set<Vector3i> poweringLocations = new HashSet<>();
        Vector3i min = start;
        Vector3i max = min;

        Queue<Vector3i> next = new ArrayDeque<>();
        next.offer(start);
        if (isPowering(world, start))
        {
            poweringLocations.add(start);
        }

        while (!next.isEmpty())
        {
            final Vector3i current = next.poll();
            knownLocations.add(current);
            for (Direction dir : directions)
            {
                final Vector3i pos = current.add(dir.asBlockOffset());
                if (!knownLocations.contains(pos))
                {
                    if (isPowering(world, pos))
                    {
                        poweringLocations.add(pos);
                        next.add(pos);

                        min = min.min(pos);
                        max = max.max(pos);
                    }
                    if (poweringLocations.contains(current))
                    {
                        next.add(pos);
                    }
                }
            }

            if (knownLocations.size() > 1000)
            {
                break;
            }
        }

        ZoneConfig cfg = module.getActiveZone(player);
        cfg.world = new ConfigWorld(world);
        cfg.shape = new Cuboid(min.toDouble(), max.sub(min).toDouble());

        i18n.send(player, POSITIVE, "Minimum: x={number} y={number} z={number}", min.getX(), min.getY(), min.getZ());
        i18n.send(player, POSITIVE, "Maximum: x={number} y={number} z={number}", max.getX(), max.getY(), max.getZ());
        i18n.send(player, POSITIVE, "Redstone blocks contained in zone: {number}", poweringLocations.size());

        return true;
    }

    private boolean isPowering(ServerWorld world, Vector3i pos)
    {
        final BlockState state = world.getBlock(pos);
        return state.supports(Keys.POWER) || state.supports(Keys.IS_POWERED) || state.getType().isAnyOf(
            BlockTypes.REDSTONE_TORCH, BlockTypes.REDSTONE_WALL_TORCH, BlockTypes.REPEATER, BlockTypes.PISTON,
            BlockTypes.PISTON_HEAD, BlockTypes.MOVING_PISTON, BlockTypes.STICKY_PISTON, BlockTypes.SLIME_BLOCK);
    }
}
