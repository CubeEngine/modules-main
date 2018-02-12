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
package org.cubeengine.module.protector.command;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.chat.ChatTypes.ACTION_BAR;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.WHITE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.teleport.TeleportHelperFilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Command(name = "region", desc = "Manages the regions")
public class RegionCommands extends ContainerCommand
{
    private Selector selector;
    private RegionManager manager;
    private I18n i18n;
    private TaskManager tm;

    public RegionCommands(CommandManager base, Selector selector, RegionManager manager, I18n i18n, TaskManager tm)
    {
        super(base, Protector.class);
        this.selector = selector;
        this.manager = manager;
        this.i18n = i18n;
        this.tm = tm;
    }

    @Command(desc = "Defines a new Region")
    public void define(Player context, String name)
    {
        Shape shape = selector.getSelection(context);
        if (shape == null)
        {
            i18n.send(context, NEGATIVE, "Nothing selected!");
            return;
        }
        World world = selector.getFirstPoint(context).getExtent();
        if (manager.hasRegion(world, name))
        {
            i18n.send(context, NEGATIVE, "There is already a Region named {name}", name);
            return;
        }
        Region region = manager.newRegion(world, shape.getBoundingCuboid(), name);
        manager.setActiveRegion(context, region);
        Set<Region> regions = new HashSet<>();
        for (Vector3d vec : region.getCuboid())
        {
            regions.addAll(manager.getRegionsAt(region.getWorld(), vec.toInt()));
        }
        int containedPrio = 0;
        int containsPrio = 9000;
        int containedCnt = 0;
        int containsCnt = 0;
        int intersectCnt = 0;
        for (Region otherRegion : regions)
        {
            if (otherRegion.isGlobal() || otherRegion.isWorldRegion() || otherRegion.getCuboid().contains(region.getCuboid()))
            {
                containedPrio = Math.max(containedPrio, otherRegion.getPriority());
                containedCnt++;
            }
            else if (region.getCuboid().contains(otherRegion.getCuboid()))
            {
                containsPrio = Math.min(containsPrio, otherRegion.getPriority());
                containsCnt++;
            }
            else
            {
                intersectCnt++;
            }
        }

        containedPrio -= 2; // remove world and global region

        // TODO search for intersecting/containing/enveloping regions and adjust base priority accordingly
        region.setPriority((containedPrio + containsPrio) / 2);
        region.save();

        i18n.send(context, POSITIVE, "Region {region} created with priority ({amount})!", region, region.getPriority());
    }

    @Command(desc = "Redefines an existing Region")
    public void redefine(Player context, @Default Region region)
    {
        Shape shape = selector.getSelection(context);
        if (shape == null)
        {
            i18n.send(context, NEGATIVE, "Nothing selected!");
            return;
        }
        World world = selector.getFirstPoint(context).getExtent();
        if (!region.getWorld().equals(world))
        {
            i18n.send(context, NEGATIVE, "This region is in another world!");
            return;
        }
        manager.changeRegion(region, shape.getBoundingCuboid());
        i18n.send(context, POSITIVE, "Region {region} updated!", region);
    }

    @Command(desc = "Selects a Region")
    public void select(CommandSource context, Region region)
    {
        manager.setActiveRegion(context, region);
        if (context instanceof Player && region.getWorld() != null && region.getName() != null)
        {
            selector.setFirstPoint(((Player) context), new Location<>(region.getWorld(), region.getCuboid().getMinimumPoint()));
            selector.setSecondPoint(((Player) context), new Location<>(region.getWorld(), region.getCuboid().getMaximumPoint()));
        }
        i18n.send(context, POSITIVE, "Region {region} selected!", region);
    }

    @Command(desc = "Lists regions")
    public void list(CommandSource context, @Optional String match, @Named("in") World world)
    {
        // TODO clickable to select
        World w = world;
        if (world == null && context instanceof Locatable)
        {
            w = ((Locatable) context).getWorld();
        }
        Map<UUID, Map<String, Region>> regions = manager.getRegions();
        List<Region> list = new ArrayList<>();
        if (w == null)
        {
            for (Map<String, Region> map : regions.values())
            {
                list.addAll(map.values());
            }
        }
        else
        {
            list.addAll(regions.getOrDefault(w.getUniqueId(), Collections.emptyMap()).values());
        }

        if (match != null)
        {
            list = list.stream().filter(r -> r.getName().matches(match) || r.getName().startsWith(match)).collect(toList());
        }

        if (world == null)
        {
            list.add(manager.getGlobalRegion());
            list.addAll(manager.getWorldRegions());
        }
        else
        {
            list.add(manager.getWorldRegion(world.getUniqueId()));
        }

        if (list.isEmpty())
        {
            i18n.send(context, NEGATIVE, "No Regions found");
            return;
        }

        i18n.send(context, NEUTRAL, "The following regions were found:");
        list.sort(Comparator.comparingInt(Region::getPriority).reversed());
        for (Region region : list)
        {
            Text prio = i18n.translate(context, TextFormat.of(YELLOW), "priority: {amount}", region.getPriority());
            if (region.isGlobal())
            {
                context.sendMessage(Text.of(" - ", GOLD, "global ",
                        GRAY, i18n.getTranslation(context, "(all worlds)"), " ", prio));
            }
            else if (region.isWorldRegion())
            {
                context.sendMessage(Text.of(" - ", GOLD, region.getWorld().getName(), WHITE, ".", GOLD, "world ",
                        GRAY, i18n.getTranslation(context, "(entire world)"), " ", prio));
            }
            else
            {
                context.sendMessage(Text.of(" - ", GOLD, region.getWorld().getName(), WHITE, ".", GOLD, region.getName(), " ", prio));
            }

        }

    }

    @Command(desc = "Changes Region priority")
    public void priority(CommandSource context, @Optional Integer priority, @Default Region region)
    {
        if (priority == null)
        {
            i18n.send(context, POSITIVE, "{name} has priority {amount}", region.toString(), region.getPriority());
            return;
        }
        region.setPriority(priority);
        region.save();
        i18n.send(context, POSITIVE, "{name} priority set to {amount}", region.toString(), region.getPriority());
    }

    // TODO region here / at / there? print all regions at position

    @Command(desc = "Displays Region info")
    public void info(CommandSource context, @Default Region region, @Flag boolean allSettings)
    {
        if (region.getWorld() == null)
        {
            i18n.send(context, POSITIVE, "Global region has the following settings:");
        }
        else if (region.getCuboid() == null)
        {
            i18n.send(context, POSITIVE, "World region in {world} has the following settings:", region.getWorld());
            // TODO tp on click to spawn
        }
        else
        {
            i18n.send(context, POSITIVE, "Region {region} in {world} has the following settings:", region, region.getWorld());
        }

        Cuboid cuboid = region.getCuboid();
        if (cuboid != null)
        {
            // TODO tp on click to center
            i18n.send(context, POSITIVE, "Inside Cuboid of {vector} to {vector}", cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        }

        // TODO priority
        RegionConfig.Settings settings = region.getSettings();
        showSetting(context, i18n.getTranslation(context, "build"), settings.build, allSettings);
        showSetting(context, i18n.getTranslation(context, "move"), settings.move, allSettings);
        showSetting(context, i18n.getTranslation(context, "use-block"), settings.use.block, allSettings);
        showSetting(context, i18n.getTranslation(context, "use-item"), settings.use.item, allSettings);
        showSetting(context, i18n.getTranslation(context, "useall blocks"), settings.use.all.block, allSettings);
        showSetting(context, i18n.getTranslation(context, "useall items"), settings.use.all.item, allSettings);
        showSetting(context, i18n.getTranslation(context, "useall container"), settings.use.all.container, allSettings);
        showSetting(context, i18n.getTranslation(context, "useall openables"), settings.use.all.open, allSettings);
        showSetting(context, i18n.getTranslation(context, "useall redstone"), settings.use.all.redstone, allSettings);

        showSetting(context, i18n.getTranslation(context, "spawn naturally"), settings.spawn.naturally, allSettings);
        showSetting(context, i18n.getTranslation(context, "spawn player"), settings.spawn.player, allSettings);
        showSetting(context, i18n.getTranslation(context, "spawn plugin"), settings.spawn.plugin, allSettings);

        showSetting(context, i18n.getTranslation(context, "blockdamage all explosion"), settings.blockDamage.allExplosion, allSettings);
        showSetting(context, i18n.getTranslation(context, "blockdamage player explosion"), settings.blockDamage.playerExplosion, allSettings);
        showSetting(context, i18n.getTranslation(context, "blockdamage block"), settings.blockDamage.block, allSettings);
        showSetting(context, i18n.getTranslation(context, "blockdamage monster"), settings.blockDamage.monster, allSettings);

        showSetting(context, i18n.getTranslation(context, "entitydamage all"), settings.entityDamage.all, allSettings);
        showSetting(context, i18n.getTranslation(context, "entitydamage byliving"), settings.entityDamage.byLiving, allSettings);
        showSetting(context, i18n.getTranslation(context, "entitydamage byentity"), settings.entityDamage.byEntity, allSettings);

        showSetting(context, i18n.getTranslation(context, "playerdamage all"), settings.playerDamage.all, allSettings);
        showSetting(context, i18n.getTranslation(context, "playerdamage pvp"), settings.playerDamage.pvp, allSettings);
        showSetting(context, i18n.getTranslation(context, "playerdamage byliving"), settings.entityDamage.byLiving, allSettings);

        showSetting(context, i18n.getTranslation(context, "blocked commands"), settings.blockedCommands, allSettings);

        showSetting(context, i18n.getTranslation(context, "deadcircuit"), settings.deadCircuit, allSettings);

        // TODO remaining settings
    }

    private void showSetting(CommandSource cs, String name, Tristate value, boolean allSettings)
    {
        if (value != Tristate.UNDEFINED || allSettings)
        {
            cs.sendMessage(Text.of(YELLOW, name, ": ", GOLD, toText(cs, value)));
        }
    }

    private void showSetting(CommandSource cs, String name, Map<?, Tristate> values, boolean allSettings)
    {
        int pos = 0;
        int neg = 0;
        for (Tristate tristate : values.values())
        {
            if (tristate == Tristate.TRUE)
            {
                pos++;
            }
            else if (tristate == Tristate.FALSE)
            {
                neg++;
            }
        }

        if (values.size() > 0 || allSettings)
        {
            Text trueText = Text.of(YELLOW, i18n.getTranslation(cs, "Enabled"), " ", GOLD, pos);
            Text falseText = Text.of(YELLOW, i18n.getTranslation(cs, "Disabled"), " ", GOLD, neg);
            cs.sendMessage(Text.of(YELLOW, name, ": ", trueText, " ", falseText));
            Map<Tristate, List<String>> settings = new HashMap<>();
            for (Map.Entry<?, Tristate> entry : values.entrySet())
            {
                String key;
                if (entry.getKey() instanceof Enum)
                {
                    key = ((Enum) entry.getKey()).name();
                }
                else if (entry.getKey() instanceof CatalogType)
                {
                    key = ((CatalogType) entry.getKey()).getName();
                }
                else if (entry.getKey() instanceof String)
                {
                    key = ((String) entry.getKey());
                }
                else
                {
                    throw new IllegalArgumentException("Unsupported KeyType for map: " + entry.getKey().getClass().getSimpleName());
                }
                List<String> list = settings.computeIfAbsent(entry.getValue(), k -> new ArrayList<>());
                list.add(key);
            }
            for (Map.Entry<Tristate, List<String>> entry : settings.entrySet())
            {
                Text.Builder builder = Text.of(" - ", toText(cs, entry.getKey()), YELLOW, ": ").toBuilder();
                boolean first = true;
                for (String val : entry.getValue())
                {
                    if (!first)
                    {
                        builder.append(Text.of(GRAY, ", "));
                    }
                    first = false;
                    builder.append(Text.of(YELLOW, val));
                }
                cs.sendMessage(builder.build());
            }

        }
    }

    private Text toText(CommandSource cs, Tristate val)
    {
        switch (val)
        {
            case TRUE:
                return Text.of(TextColors.DARK_GREEN, i18n.getTranslation(cs,"Enabled"));
            case FALSE:
                return Text.of(TextColors.DARK_RED, i18n.getTranslation(cs,"Disabled"));
            default:
                return Text.of(TextColors.GOLD, i18n.getTranslation(cs,"Undefined"));
        }
    }

    @Command(desc = "Deletes a region", alias = "remove")
    public void delete(CommandSource context, Region region)
    {
        if (region.isGlobal())
        {
            i18n.send(context, NEGATIVE, "Cannot delete global region.");
            return;
        }
        if (!manager.deleteRegion(region))
        {
            i18n.send(context, CRITICAL, "Could not delete region file.");
            return;
        }
        i18n.send(context, POSITIVE, "The region {name} was deleted.", region.toString());
    }

    private Map<UUID, UUID> showRegionTasks = new HashMap<>();

    @Command(desc = "Toggles particles for the currently selected region")
    public void show(Player context)
    {
        UUID task = this.showRegionTasks.remove(context.getUniqueId());
        if (task != null)
        {
            tm.cancelTask(Protector.class, task);
            i18n.send(ACTION_BAR, context, POSITIVE, "Stopped showing active region.");
            return;
        }
        task = tm.runTimer(Protector.class, () -> this.showActiveRegion(context), 10, 5);
        this.showRegionTasks.put(context.getUniqueId(), task);
        i18n.send(ACTION_BAR, context, POSITIVE, "Started showing active region.");
    }

    private void showActiveRegion(Player player)
    {
        if (!player.isOnline()) {
            UUID task = this.showRegionTasks.remove(player.getUniqueId());
            tm.cancelTask(Protector.class, task);
            return;
        }

        Region region = manager.getActiveRegion(player);
        if (region == null || region.isGlobal() || region.isWorldRegion()) {
            return;
        }
        Cuboid cuboid = region.getCuboid();
        Vector3d mmm = cuboid.getMinimumPoint();
        Vector3d xxx = cuboid.getMaximumPoint();

        Vector3d mmx = new Vector3d(mmm.getX(), mmm.getY(), xxx.getZ());
        Vector3d mxx = new Vector3d(mmm.getX(), xxx.getY(), xxx.getZ());
        Vector3d xmm = new Vector3d(xxx.getX(), mmm.getY(), mmm.getZ());
        Vector3d xxm = new Vector3d(xxx.getX(), xxx.getY(), mmm.getZ());

        Vector3d mxm = new Vector3d(mmm.getX(), xxx.getY(), mmm.getZ());
        Vector3d xmx = new Vector3d(xxx.getX(), mmm.getY(), xxx.getZ());

        Map<Color, List<Vector3d>> particles = new HashMap<>();
        List<Vector3d> red = new ArrayList<>();
        particles.put(Color.RED, red);
        int stepZ = ((int) mmm.distance(mmx)) * 5;
        linePoints(red, mmm, mmx, stepZ);
        linePoints(red, mxx, mxm, stepZ);
        linePoints(red, xxx, xxm, stepZ);
        linePoints(red, xmx, xmm, stepZ);

        List<Vector3d> green = new ArrayList<>();
        particles.put(Color.GREEN, green);
        int stepY = ((int) mmm.distance(mxm)) * 5;
        linePoints(green, mmm, mxm, stepY);
        linePoints(green, mmx, mxx, stepY);
        linePoints(green, xxx, xmx, stepY);
        linePoints(green, xxm, xmm, stepY);

        List<Vector3d> blue = new ArrayList<>();
        particles.put(Color.BLUE, blue);
        int stepX = ((int) mmm.distance(xmm)) * 5;
        linePoints(blue, mmm, xmm, stepX);
        linePoints(blue, mmx, xmx, stepX);
        linePoints(blue, xxx, mxx, stepX);
        linePoints(blue, xxm, mxm, stepX);

        draw(player, new Vector3d(0.5,0.5,0.5), particles);
    }

    public void linePoints(List<Vector3d> list, Vector3d point, Vector3d point2, int steps) {
        Vector3d move = point2.sub(point).div(steps);
        for (int step = 0; step < steps; step++) {
            point = point.add(move);
            list.add(point);
        }
    }

    public void draw(Player player, Vector3d position, Map<Color, List<Vector3d>> particles) {
        for (Map.Entry<Color, List<Vector3d>> entry : particles.entrySet()) {
            for (Vector3d vec : entry.getValue()) {
                player.spawnParticles(ParticleEffect.builder()
                                .type(ParticleTypes.REDSTONE_DUST)
                                .option(ParticleOptions.COLOR, entry.getKey())
                                .build(),
                        position.add(vec));
            }
        }
    }

    public void parent(CommandSource context, Region parent, @Default Region region)
    {

    }

    @Command(desc = "Teleports to a region")
    public void teleport(Player context, Region region, @Flag boolean force)
    {
        if (region.isGlobal())
        {
            // TODO
            return;
        }
        Vector3d middle = region.getCuboid().getMinimumPoint().add(region.getCuboid().getMaximumPoint()).div(2);
        Location<World> loc = new Location<>(region.getWorld(), middle);
        GameMode mode = context.get(Keys.GAME_MODE).orElse(null);
        if (mode != GameModes.SPECTATOR)
        {
            int h = (int) region.getCuboid().getHeight() / 2 + 1;
            int w = (int) Math.max(region.getCuboid().getWidth() / 2 + 1, region.getCuboid().getDepth() / 2 + 1);
            java.util.Optional<Location<World>> adjusted =
                    Sponge.getTeleportHelper().getSafeLocation(loc, Math.max(h, 5), Math.max(w, 5), ((int) region.getCuboid().getHeight()),
                            mode == GameModes.CREATIVE ? TeleportHelperFilters.FLYING : TeleportHelperFilters.DEFAULT);
            if (!adjusted.isPresent() && !force) {
                i18n.send(ACTION_BAR, context, POSITIVE, "Could not find a safe spot in region. Use -force to teleport anyways");
                return;
            }
            loc = adjusted.orElse(loc);
        }
        context.setLocation(loc);
        i18n.send(ACTION_BAR, context, POSITIVE, "Teleported to {name}", region.toString());
    }

    public void redstonedefine(CommandSource context, String name)
    {
        // Rightclick redstone - define region around the whole circuit
    }
}
