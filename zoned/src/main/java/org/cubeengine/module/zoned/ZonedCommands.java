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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.WHITE;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.CompositeShape;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.PoweredProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import javax.inject.Inject;

@Command(name = "zone", desc = "Manages zones")
public class ZonedCommands extends ContainerCommand {

    private Zoned module;
    private I18n i18n;
    private Reflector reflector;
    private TaskManager tm;
    private EventManager em;

    @Inject
    public ZonedCommands(CommandManager cm, Zoned module, I18n i18n, TaskManager tm, EventManager em, Reflector reflector) {
        super(cm, Zoned.class);
        this.module = module;
        this.tm = tm;
        this.em = em;
        this.i18n = i18n;
        this.reflector = reflector;
    }

    @Command(desc = "Defines a new zone")
    public void define(Player context, String name) {
        ZoneConfig cfg = this.module.getActiveZone(context);
        module.getManager().define(context, name, cfg, false);
    }

    @Command(desc = "Redefines an existing zone")
    public void redefine(Player context, @Default ZoneConfig zone) {
        ZoneConfig cfg = this.module.getActiveZone(context);
        zone.shape = cfg.shape;
        zone.name = cfg.name;
        zone.world = cfg.world;
        module.getManager().define(context, zone.name, zone, true);
    }

    @Command(desc = "Selects a zone")
    public void select(Player context, ZoneConfig zone) {
        this.module.setActiveZone(context, zone);
        i18n.send(context, POSITIVE, "Zone {name} selected!", zone.name);
    }

    @Command(desc = "Lists zones")
    public void list(CommandSource context, @Optional String match, @Named("in") World world) {
        Collection<ZoneConfig> zones = this.module.getManager().getZones(match, world);
        if (zones.isEmpty()) {
            i18n.send(context, NEGATIVE, "No Zones found");
            return;
        }
        i18n.send(context, NEUTRAL, "The following zones were found:");
        for (ZoneConfig zone : zones) {
            context.sendMessage(Text.of(" - ", GOLD, zone.world.getName(), WHITE, ".", GOLD, zone.name));
        }
    }

    @Command(desc = "Displays zone info")
    public void info(CommandSource context, @Default ZoneConfig zone)
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
    public void delete(CommandSource context, ZoneConfig zone) {
        this.module.getManager().delete(context, zone);
    }

    @Command(desc = "Toggles particles for the currently selected region")
    public void show(Player context, @Optional ZoneConfig zone) {
        if (zone != null) {
            this.module.setActiveZone(context, zone);
        }
        if (!ShapeRenderer.toggleShowActiveRegion(tm, context, module)) {
            i18n.send(ACTION_BAR, context, POSITIVE, "Stopped showing active region.");
            return;
        }
        i18n.send(ACTION_BAR, context, POSITIVE, "Started showing active region.");
    }

    @Command(desc = "Teleports to a zone", alias = "tp")
    public void teleport(Player context, ZoneConfig zone, @Flag boolean force) {
        Cuboid boundingCuboid = zone.shape.getBoundingCuboid();
        Vector3d middle = boundingCuboid.getMinimumPoint().add(boundingCuboid.getMaximumPoint()).div(2);
        Location<World> loc = new Location<>(zone.world.getWorld(), middle);
        GameMode mode = context.get(Keys.GAME_MODE).orElse(null);
        if (mode != GameModes.SPECTATOR) {
            int h = (int) boundingCuboid.getHeight() / 2 + 1;
            int w = (int) Math.max(boundingCuboid.getWidth() / 2 + 1, boundingCuboid.getDepth() / 2 + 1);
            java.util.Optional<Location<World>> adjusted =
                    Sponge.getTeleportHelper().getSafeLocation(loc, Math.max(h, 5), Math.max(w, 5), ((int) boundingCuboid.getHeight()),
                            mode == GameModes.CREATIVE ? TeleportHelperFilters.FLYING : TeleportHelperFilters.DEFAULT);
            if (!adjusted.isPresent() && !force) {
                i18n.send(ACTION_BAR, context, POSITIVE, "Could not find a safe spot in region. Use -force to teleport anyways");
                return;
            }
            loc = adjusted.orElse(loc);
        }
        context.setLocation(loc);
        i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to {name}", zone.name);
    }

    @Command(desc = "Creates a zone by following connected redstone")
    public void circuitSelect(Player player) {
        Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        em.listenUntil(this.module.getClass(), InteractBlockEvent.Secondary.class, e -> e.getCause().root().equals(player),
                e -> this.circuitSelect(e, player, directions));
        i18n.send(player, POSITIVE, "Select a piece of your redstone circuit.");
    }

    // TODO command for compound zone creation
    // @Command(desc = "Composite Shape zone management")
    public void composite(Player context, @Optional Integer part)
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

    private boolean circuitSelect(InteractBlockEvent.Secondary e, Player player, Set<Direction> directions) {
        e.setCancelled(true);
        java.util.Optional<Location<World>> target = e.getTargetBlock().getLocation();
        if (target.isPresent()) {
            Location<World> start = target.get();
            if (start.getProperty(PoweredProperty.class).isPresent()) {
                Set<Location<World>> knownLocations = new HashSet<>();
                Set<Location<World>> poweringLocations = new HashSet<>();
                Vector3i min = start.getBlockPosition();
                Vector3i max = min;

                Queue<Location<World>> next = new ArrayDeque<>();
                next.offer(start);
                if (isPowering(start)) {
                    poweringLocations.add(start);
                }

                while (!next.isEmpty()) {
                    Location<World> current = next.poll();
                    knownLocations.add(current);
                    for (Direction dir : directions) {
                        Location<World> loc = current.getRelative(dir);
                        if (!knownLocations.contains(loc)) {
                            if (isPowering(loc)) {
                                poweringLocations.add(loc);
                                next.add(loc);

                                Vector3i pos = loc.getBlockPosition();
                                min = min.min(pos);
                                max = max.max(pos);
                            }
                            if (poweringLocations.contains(current)) {
                                next.add(loc);
                            }
                        }
                    }

                    if (knownLocations.size() > 1000) {
                        break;
                    }
                }


                ZoneConfig cfg = module.getActiveZone(player);
                cfg.world = new ConfigWorld(start.getExtent());
                cfg.shape = new Cuboid(min.toDouble(), max.sub(min).toDouble());

                i18n.send(player, POSITIVE, "Minimum: x={number} y={number} z={number}", min.getX(), min.getY(), min.getZ());
                i18n.send(player, POSITIVE, "Maximum: x={number} y={number} z={number}", max.getX(), max.getY(), max.getZ());
                i18n.send(player, POSITIVE, "Redstone blocks contained in region: {number}", poweringLocations.size());

                return true;

            } else {
                i18n.send(player, NEGATIVE, "Click a redstone powered block!");
                return false;
            }

        } else {
            i18n.send(player, NEGATIVE, "Block had no location.");
            return false;
        }
    }

    private boolean isPowering(Location<World> loc) {
        return loc.supports(Keys.POWER) ||
                loc.supports(Keys.POWERED) ||
                loc.getBlockType() == BlockTypes.REDSTONE_TORCH ||
                loc.getBlockType() == BlockTypes.UNLIT_REDSTONE_TORCH ||
                loc.getBlockType() == BlockTypes.REDSTONE_BLOCK ||
                loc.getBlockType() == BlockTypes.POWERED_REPEATER ||
                loc.getBlockType() == BlockTypes.UNPOWERED_REPEATER ||
                loc.getBlockType() == BlockTypes.PISTON_EXTENSION ||
                loc.getBlockType() == BlockTypes.PISTON_HEAD ||
                loc.getBlockType() == BlockTypes.SLIME;
    }

}
