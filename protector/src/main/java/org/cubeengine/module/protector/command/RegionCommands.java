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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.command.parser.TristateParser;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerWorld;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
@Using(TristateParser.class)
@Command(name = "protect", desc = "Manages the regions")
public class RegionCommands extends DispatcherCommand
{
    private final Protector module;
    private RegionManager manager;
    private I18n i18n;
    private TaskManager tm;
    private final EventManager em;

    @Inject
    public RegionCommands(Protector module, RegionManager manager, I18n i18n, TaskManager tm, EventManager em, SettingsCommands settingsCmd)
    {
        super(settingsCmd);
        this.module = module;
        this.manager = manager;
        this.i18n = i18n;
        this.tm = tm;
        this.em = em;
    }

    @Command(desc = "Lists protected zones")
    public void list(CommandCause context, @Option String match, @Named("in") ServerWorld world)
    {
        // TODO clickable to select
        ServerWorld w = world;
        if (world == null && context instanceof Locatable)
        {
            w = ((Locatable) context).getServerLocation().getWorld();
        }
        Map<ResourceKey, Map<String, Region>> regions = manager.getRegions();
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
            list.add(manager.getWorldRegion(world.getKey()));
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
            Component prio = i18n.translate(context, "priority: {amount}", region.getPriority()).color(NamedTextColor.YELLOW);
            if (region.isGlobal())
            {
                context.sendMessage(Identity.nil(), Component.text(" - ")
                                         .append(Component.text("global", NamedTextColor.GOLD))
                                         .append(i18n.translate(context, "(all worlds)").color(NamedTextColor.GRAY))
                                         .append(Component.space())
                                         .append(prio));
            }
            else if (region.isWorldRegion())
            {
                context.sendMessage(Identity.nil(), Component.text(" - ")
                                                             .append(Component.text(region.getWorldName(), NamedTextColor.GOLD))
                                                             .append(Component.text(".", NamedTextColor.WHITE))
                                                             .append(Component.text("world", NamedTextColor.GOLD))
                                                             .append(i18n.translate(context, "(entire world)").color(NamedTextColor.GRAY))
                                                             .append(Component.space())
                                                             .append(prio));
            }
            else
            {
                context.sendMessage(Identity.nil(), Component.text(" - ")
                                                             .append(Component.text(region.getWorldName(), NamedTextColor.GOLD))
                                                             .append(Component.text(".", NamedTextColor.WHITE))
                                                             .append(Component.text(region.getName(), NamedTextColor.GOLD))
                                                             .append(Component.space())
                                                             .append(prio));
            }

        }

    }

    @Command(desc = "Changes protection zone priority")
    public void priority(CommandCause context, @Option Integer priority, @Default Region region)
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
    public void info(CommandCause context, @Default Region region, @Flag boolean allSettings)
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

    private void showSetting(CommandCause cs, String name, Tristate value, boolean allSettings)
    {
        if (value != Tristate.UNDEFINED || allSettings)
        {
            cs.sendMessage(Identity.nil(), Component.text(name, NamedTextColor.YELLOW).append(toText(cs, value).color(NamedTextColor.GOLD)));
        }
    }

    private void showSetting(CommandCause cs, String name, Map<?, Tristate> values, boolean allSettings)
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
            Component trueText = i18n.translate(cs, "Enabled").color(NamedTextColor.YELLOW).append(Component.space()).append(Component.text(pos, NamedTextColor.GOLD));
            Component falseText = i18n.translate(cs, "Disabled").color(NamedTextColor.YELLOW).append(Component.space()).append(Component.text(neg, NamedTextColor.GOLD));
            cs.sendMessage(Identity.nil(), Component.text(name, NamedTextColor.YELLOW).append(Component.text(":")).append(trueText).append(Component.space()).append(falseText));
            Map<Tristate, List<ComponentLike>> settings = new HashMap<>();
            for (Map.Entry<?, Tristate> entry : values.entrySet())
            {
                ComponentLike key;
                if (entry.getKey() instanceof Enum)
                {
                    key = Component.text(((Enum) entry.getKey()).name());
                }
                else if (entry.getKey() instanceof ComponentLike)
                {
                    key = ((ItemType) entry.getKey());
                }
                else if (entry.getKey() instanceof String)
                {
                    key = Component.text((String) entry.getKey());
                }
                else
                {
                    throw new IllegalArgumentException("Unsupported KeyType for map: " + entry.getKey().getClass().getSimpleName());
                }
                List<ComponentLike> list = settings.computeIfAbsent(entry.getValue(), k -> new ArrayList<>());
                list.add(key);
            }
            for (Map.Entry<Tristate, List<ComponentLike>> entry : settings.entrySet())
            {
                final TextComponent text = Component.text(" - ").append(toText(cs, entry.getKey())).append(Component.text(": ", NamedTextColor.YELLOW));
                boolean first = true;
                for (ComponentLike val : entry.getValue())
                {
                    if (!first)
                    {
                        text.append(Component.text(", ", NamedTextColor.GRAY));
                    }
                    first = false;
                    text.append(val.asComponent().color(NamedTextColor.YELLOW));
                }
                cs.sendMessage(Identity.nil(), text);
            }

        }
    }

    private Component toText(CommandCause cs, Tristate val)
    {
        switch (val)
        {
            case TRUE:
                return i18n.translate(cs, "Enabled").color(NamedTextColor.DARK_GREEN);
            case FALSE:
                return i18n.translate(cs,"Disabled").color(NamedTextColor.DARK_RED);
            default:
                return i18n.translate(cs,"Undefined").color(NamedTextColor.GOLD);
        }
    }

    @Command(desc = "Clears a zones protection settings")
    public void clear(CommandCause context, Region region)
    {
        if (!manager.deleteRegion(region))
        {
            i18n.send(context, CRITICAL, "Could not delete region file.");
            return;
        }
        i18n.send(context, POSITIVE, "The region {name} was deleted.", region.toString());
    }

    public void parent(CommandCause context, Region parent, @Default Region region)
    {

    }

}
