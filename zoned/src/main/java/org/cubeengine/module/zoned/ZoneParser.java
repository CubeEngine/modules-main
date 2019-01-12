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

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.libcube.service.command.TranslatedParserException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ZoneParser implements ArgumentParser<ZoneConfig>, Completer, DefaultValue<ZoneConfig>
{

    private Zoned module;
    private ZoneManager manager;
    private I18n i18n;

    public ZoneParser(Zoned module, ZoneManager manager, I18n i18n)
    {
        this.module = module;

        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        String token = invocation.currentToken().toLowerCase();
        List<String> list = new ArrayList<>();
        World world = null;
        boolean isLocatable = invocation.getCommandSource() instanceof Locatable;
        if (isLocatable)
        {
            world = ((Locatable) invocation.getCommandSource()).getWorld();
            for (ZoneConfig zone : manager.getZones(null, world))
            {
                if (zone.name == null)
                {
                    continue;
                }
                if (zone.name.startsWith(token))
                {
                    list.add(zone.name);
                }
            }
        }

        for (ZoneConfig zone : manager.getZones(null, null))
        {
            /* TODO global region
            if ("global".startsWith(token))
            {
                list.add("global");
            }
             */
            if (world != null && zone.world.getWorld().getUniqueId().equals(world.getUniqueId())
                    && !world.getName().startsWith(token.replace(".", "")))
            {
                continue; // Skip if already without world ; except when token starts with world
            }
            if (token.contains(".") || !isLocatable) // Skip if without dot and locatable
            {
                String fullName = zone.world.getName() + "." + zone.name;
                if (fullName.startsWith(token))
                {
                    list.add(fullName);
                }
                /* TODO world region?
                if ((worldName + ".world").startsWith(token))
                {
                    list.add(worldName + ".world");
                }
                */
            }
        }

        /* TODO world region?
        if (isLocatable && "world".startsWith(token))
        {
            list.add("world");
        }
        */
        /* TODO world regions?
        for (ZoneConfig ZoneConfig : manager.getWorldZoneConfigs())
        {
            String worldName = ZoneConfig.getWorld().getName();
            if (worldName.startsWith(token))
            {
                list.add(worldName);
            }
            if (token.contains(".") && (worldName + ".").startsWith(token))
            {
                list.add(worldName + ".world");
            }
        }
        */

        return list;
    }

    @Override
    public ZoneConfig parse(Class aClass, CommandInvocation invocation) throws ParserException
    {
        String token = invocation.consume(1).toLowerCase();
        if (invocation.getCommandSource() instanceof Locatable)
        {
            World world = ((Locatable) invocation.getCommandSource()).getWorld();
            ZoneConfig zone = manager.getZone(token);
            if (zone != null) {
                return zone;
            }
            /* TODO world regions
            if ("world".equals(token))
            {
                ZoneConfig = manager.getWorldZoneConfig(world.getUniqueId());
                if (ZoneConfig != null)
                {
                    return ZoneConfig;
                }
            }
            */
        }

        /* TODO world region
        if (token.endsWith(".world"))
        {
            String worldName = token.replaceAll(".world$", "");
            Optional<WorldProperties> worldProp = Sponge.getServer().getWorldProperties(worldName);
            if (worldProp.isPresent())
            {
                return manager.getWorldZoneConfig(worldProp.get().getUniqueId());
            }
            else
            {
                throw new TranslatedParserException(
                        i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                "Unknown World {name} for world-ZoneConfig", token, worldName));
            }
        }
        */
        /* TODO global region
        if ("global".equals(token))
        {
            return manager.getGlobalZoneConfig();
        }
        */

        throw new TranslatedParserException(
                i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                        "There is no zone named {name}", token));
    }

    @Override
    public ZoneConfig provide(CommandInvocation invocation)
    {
        // TODO for other command-sources?
        if (invocation.getCommandSource() instanceof Player)
        {
            ZoneConfig zone = module.getActiveZone(((Player) invocation.getCommandSource()));
            if (zone != null)
            {
                return zone;
            }
            List<ZoneConfig> zones = manager.getZonesAt(((Player) invocation.getCommandSource()).getLocation());
            if (!zones.isEmpty())
            {
                return zones.get(0);
            }
        }
        throw new TranslatedParserException(i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                        "You need to provide a zone"));

    }
}
