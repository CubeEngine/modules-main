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
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.GRAY;
import static org.spongepowered.api.text.format.TextColors.WHITE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Command(name = "protect", desc = "Manages the regions")
public class RegionCommands extends ContainerCommand
{
    private final Protector module;
    private RegionManager manager;
    private I18n i18n;
    private TaskManager tm;
    private final EventManager em;

    public RegionCommands(CommandManager base, Protector module, RegionManager manager, I18n i18n, TaskManager tm, EventManager em)
    {
        super(base, Protector.class);
        this.module = module;
        this.manager = manager;
        this.i18n = i18n;
        this.tm = tm;
        this.em = em;
    }

    @Command(desc = "Lists protected zones")
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

    @Command(desc = "Changes protection zone priority")
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

    @Command(desc = "Clears a zones protection settings")
    public void clear(CommandSource context, Region region)
    {
        if (!manager.deleteRegion(region))
        {
            i18n.send(context, CRITICAL, "Could not delete region file.");
            return;
        }
        i18n.send(context, POSITIVE, "The region {name} was deleted.", region.toString());
    }

    public void parent(CommandSource context, Region parent, @Default Region region)
    {

    }

}
