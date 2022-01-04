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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.zoned.ZoneManager;
import org.cubeengine.module.zoned.Zoned;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionType;
import org.spongepowered.api.command.parameter.managed.clientcompletion.ClientCompletionTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerWorld;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

@Singleton
@ParserFor(Region.class)
public class RegionParser implements ValueParser<Region>, ValueCompleter, DefaultParameterProvider<Region>
{

    private ZoneManager zoneMan;
    private RegionManager manager;
    private I18n i18n;

    @Inject
    public RegionParser(ModuleManager mm, RegionManager manager, I18n i18n)
    {
        this.zoneMan = ((Zoned)mm.getModule(Zoned.class)).getManager();
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public List<ClientCompletionType> clientCompletionType()
    {
        return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
    }

    @Override
    public Region apply(CommandCause commandCause)
    {
        Region activeRegion = manager.getActiveRegion(commandCause);
        if (activeRegion != null)
        {
            return activeRegion;
        }
        if (commandCause.subject() instanceof ServerPlayer)
        {
            List<Region> regions = manager.getRegionsAt(((ServerPlayer)commandCause.subject()).serverLocation());
            if (!regions.isEmpty())
            {
                return regions.get(0);
            }
        }
        i18n.send(commandCause, NEGATIVE, "You need to provide a region");;
        return null;
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        String token = currentInput.toLowerCase();
        List<CommandCompletion> list = new ArrayList<>();
        ServerWorld world = null;
        boolean isLocatable = context.subject() instanceof Locatable;
        if (isLocatable)
        {
            world = ((Locatable)context.subject()).serverLocation().world();
            Map<String, Region> regions = manager.getRegions(world.key());

            for (Map.Entry<String, Region> entry : regions.entrySet())
            {
                if (entry.getKey().startsWith(token))
                {
                    list.add(CommandCompletion.of(entry.getKey()));
                }
            }

        }
        for (Map.Entry<ResourceKey, Map<String, Region>> perWorld : manager.getRegions().entrySet())
        {
            if (world != null && perWorld.getKey().equals(world.key())
                && !world.key().asString().startsWith(token.replace("/", ""))) // TODO correct?
            {
                continue; // Skip if already without world ; except when token starts with world
            }

            if (!isLocatable) // Skip if without dot and locatable
            {
                String worldName = perWorld.getKey().asString();
                for (Map.Entry<String, Region> entry : perWorld.getValue().entrySet())
                {
                    String value = entry.getValue().getContext().getValue();
                    if (value.startsWith(token))
                    {
                        list.add(CommandCompletion.of(value));
                    }
                }
            }
        }
        if ("global".startsWith(token))
        {
            list.add(CommandCompletion.of("global"));
        }
        if (isLocatable && "world".startsWith(token))
        {
            list.add(CommandCompletion.of("world"));
        }
        for (Region region : manager.getWorldRegions())
        {
            String worldName = region.getWorld().key().asString();
            if (worldName.startsWith(token))
            {
                list.add(CommandCompletion.of(worldName));
            }
        }
        // Add Zones
        if (isLocatable)
        {
            zoneMan.getZones(token, world).stream().map(cfg -> cfg.name).map(CommandCompletion::of).forEach(list::add);
        }

        return list;
    }

    @Override
    public Optional<? extends Region> parseValue(Key<? super Region> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final ResourceKey tokenAsKey = reader.parseResourceKey("protector");
        String token = tokenAsKey.asString();
        if (tokenAsKey.namespace().equals("protector"))
        {
            if (context.subject() instanceof Locatable)
            {
                final ServerWorld world = ((Locatable)context.subject()).serverLocation().world();
                Region region = manager.getRegions(world.key()).get(tokenAsKey.value());
                if (region != null)
                {
                    return Optional.of(region);
                }
                if ("protector:world".equals(token))
                {
                    region = manager.getWorldRegion(world.key());
                    if (region != null)
                    {
                        return Optional.of(region);
                    }
                }
            }

            if ("protector:global".equals(token))
            {
                return Optional.of(manager.getGlobalRegion());
            }

            ZoneConfig zone = zoneMan.getZone(tokenAsKey.value());
            if (zone != null)
            {
                return Optional.of(manager.newRegion(zone));
            }
            throw reader.createException(i18n.translate(context.cause(), NEGATIVE, "There is no such region as {name}", token));
        }

        for (Map.Entry<ResourceKey, Map<String, Region>> perWorld : manager.getRegions().entrySet())
        {
            for (Map.Entry<String, Region> entry : perWorld.getValue().entrySet())
            {
                if (entry.getValue().getContext().getValue().equalsIgnoreCase(token))
                {
                    return Optional.of(entry.getValue());
                }
            }
        }

        if (Sponge.server().worldManager().world(tokenAsKey).isPresent())
        {
            return Optional.of(manager.getWorldRegion(tokenAsKey));
        }
        else
        {
            // TODO region in world
            throw reader.createException(i18n.translate(context.cause(), NEGATIVE, "Unknown World {name} for world-region", token, token));
        }
    }

}
