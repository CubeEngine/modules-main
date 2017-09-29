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
package org.cubeengine.module.protector.region;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.libcube.service.command.TranslatedParserException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.protector.RegionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RegionParser implements ArgumentParser<Region>, Completer, DefaultValue<Region>
{
    private RegionManager manager;
    private I18n i18n;

    public RegionParser(RegionManager manager, I18n i18n)
    {
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
            Map<String, Region> regions = manager.getRegions(world.getUniqueId());

            for (Map.Entry<String, Region> entry : regions.entrySet())
            {
                if (entry.getKey().startsWith(token))
                {
                    list.add(entry.getKey());
                }
            }

        }
        for (Map.Entry<UUID, Map<String, Region>> perWorld : manager.getRegions().entrySet())
        {
            if (world != null && perWorld.getKey().equals(world.getUniqueId())
                              && !world.getName().startsWith(token.replace(".", "")))
            {
                continue; // Skip if already without world ; except when token starts with world
            }
            if (token.contains(".") || !isLocatable) // Skip if without dot and locatable
            {
                String worldName = Sponge.getServer().getWorldProperties(perWorld.getKey()).get().getWorldName();
                if ((worldName + ".world").startsWith(token))
                {
                    list.add(worldName + ".world");
                }
                for (Map.Entry<String, Region> entry : perWorld.getValue().entrySet())
                {
                    String value = entry.getValue().getContext().getValue();
                    if (value.startsWith(token))
                    {
                        list.add(value);
                    }
                }
            }
        }
        if ("global".startsWith(token))
        {
            list.add("global");
        }
        if (isLocatable && "world".startsWith(token))
        {
            list.add("world");
        }
        for (Region region : manager.getWorldRegions())
        {
            String worldName = region.getWorld().getName();
            if (worldName.startsWith(token))
            {
                list.add(worldName);
            }
            if (token.contains(".") && (worldName + ".").startsWith(token))
            {
                list.add(worldName + ".world");
            }
        }

        return list;
    }

    @Override
    public Region parse(Class aClass, CommandInvocation invocation) throws ParserException
    {
        String token = invocation.consume(1).toLowerCase();
        if (invocation.getCommandSource() instanceof Locatable)
        {
            World world = ((Locatable) invocation.getCommandSource()).getWorld();
            Region region = manager.getRegions(world.getUniqueId()).get(token);
            if (region != null)
            {
                return region;
            }
            if ("world".equals(token))
            {
                region = manager.getWorldRegion(world.getUniqueId());
                if (region != null)
                {
                    return region;
                }
            }
        }
        for (Map.Entry<UUID, Map<String, Region>> perWorld : manager.getRegions().entrySet())
        {
            for (Map.Entry<String, Region> entry : perWorld.getValue().entrySet())
            {
                if (entry.getValue().getContext().getValue().equalsIgnoreCase(token))
                {
                    return entry.getValue();
                }
            }
        }
        if (token.endsWith(".world"))
        {
            String worldName = token.replaceAll(".world$", "");
            Optional<WorldProperties> worldProp = Sponge.getServer().getWorldProperties(worldName);
            if (worldProp.isPresent())
            {
                return manager.getWorldRegion(worldProp.get().getUniqueId());
            }
            else
            {
                throw new TranslatedParserException(
                        i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                "Unknown World {name} for world-region", token, worldName));
            }
        }
        if ("global".equals(token))
        {
            return manager.getGlobalRegion();
        }

        throw new TranslatedParserException(
                i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                    "There is no such Region as {name}", token));
    }

    @Override
    public Region provide(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof CommandSource)
        {
            Region activeRegion = manager.getActiveRegion(((CommandSource) invocation.getCommandSource()));
            if (activeRegion != null)
            {
                return activeRegion;
            }
        }
        throw new TranslatedParserException(
                i18n.translate(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                    "You need to provide a region"));

    }
}
