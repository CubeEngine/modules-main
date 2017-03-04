/**
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
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.libcube.service.command.TranslatedReaderException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.module.protector.RegionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RegionReader implements ArgumentReader<Region>, Completer, DefaultValue<Region>
{
    private RegionManager manager;
    private I18n i18n;

    public RegionReader(RegionManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        String token = invocation.currentToken();
        List<String> list = new ArrayList<>();
        World world = null;
        if (invocation.getCommandSource() instanceof Locatable)
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
            for (Map.Entry<String, Region> entry : perWorld.getValue().entrySet())
            {
                String value = entry.getValue().getContext().getValue();
                if (value.startsWith(token))
                {
                    list.add(value);
                }
            }
        }
        return list;
    }

    @Override
    public Region read(Class aClass, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        if (invocation.getCommandSource() instanceof Locatable)
        {
            World world = ((Locatable) invocation.getCommandSource()).getWorld();
            Region region = manager.getRegions(world.getUniqueId()).get(token);
            if (region != null)
            {
                return region;
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
        throw new TranslatedReaderException(
                i18n.getTranslation(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                    "There is no such Region as {name}", token));
    }

    @Override
    public Region getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof CommandSource)
        {
            Region activeRegion = manager.getActiveRegion(((CommandSource) invocation.getCommandSource()));
            if (activeRegion != null)
            {
                return activeRegion;
            }
        }
        throw new TranslatedReaderException(
                i18n.getTranslation(invocation.getContext(Locale.class), MessageType.NEGATIVE,
                                    "You need to provide a region"));

    }
}
