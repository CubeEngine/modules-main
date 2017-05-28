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
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.WHITE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.Selector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Command(name = "region", desc = "Manages the regions")
public class RegionCommands extends ContainerCommand
{
    private Selector selector;
    private RegionManager manager;
    private I18n i18n;

    public RegionCommands(CommandManager base, Selector selector, RegionManager manager, I18n i18n)
    {
        super(base, Protector.class);
        this.selector = selector;
        this.manager = manager;
        this.i18n = i18n;
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
        i18n.send(context, POSITIVE, "Region {region} created!", region);
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
        for (Region region : list)
        {
            if (region.getWorld() == null)
            {
                context.sendMessage(Text.of(" - ", GOLD, "global"));
            }
            else if (region.getName() == null)
            {
                context.sendMessage(Text.of(" - ", GOLD, region.getWorld().getName(), WHITE));
            }
            else
            {
                context.sendMessage(Text.of(" - ", GOLD, region.getWorld().getName(), WHITE, ".", GOLD, region.getName()));
            }

        }

    }

    public void priority(CommandSource context, int priority, @Default Region region)
    {

    }

    // TODO region here / at / there? print all regions at position

    @Command(desc = "Displays Region info")
    public void info(CommandSource context, @Default Region region, @Flag boolean allSettings)
    {
        // TODO default to region player is in when no active region is set
        if (region.getWorld() == null)
        {
            i18n.send(context, POSITIVE, "Global region");
        }
        else if (region.getCuboid() == null)
        {
            i18n.send(context, POSITIVE, "World region in {world}", region.getWorld());
        }
        else
        {
            i18n.send(context, POSITIVE, "Region {region} in {world}", region, region.getWorld());
        }

        Cuboid cuboid = region.getCuboid();
        if (cuboid != null)
        {
            // TODO tp on click to center
            i18n.send(context, POSITIVE, "Inside Cuboid of {vector} to {vector}", cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        }

        // TODO priority
        i18n.send(context, POSITIVE, "Settings:");
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
                cs.sendMessage(Text.of(" - ", YELLOW, key, ": ", GOLD, toText(cs, entry.getValue())));
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

    public void show(Player context)
    {
        // Show boundry with player-only blocks
    }

    public void parent(CommandSource context, Region parent, @Default Region region)
    {

    }

    public void teleport(CommandSource context, @Default Region region)
    {

    }

    public void redstonedefine(CommandSource context, String name)
    {
        // Rightclick redstone - define region around the whole circuit
    }
}
