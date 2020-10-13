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

import com.google.inject.Inject;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParserFor(ZoneConfig.class)
public class ZoneParser implements ValueParser<ZoneConfig>, ValueCompleter, DefaultParameterProvider<ZoneConfig>
{
    private final Zoned module;
    private final ZoneManager manager;
    private final I18n i18n;

    @Inject
    public ZoneParser(Zoned module, ZoneManager manager, I18n i18n)
    {
        this.module = module;
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public ZoneConfig apply(CommandCause cause)
    {
        // TODO for other command-sources?
        if (cause.getAudience() instanceof ServerPlayer)
        {
            ZoneConfig zone = module.getActiveZone(((ServerPlayer) cause.getAudience()));
            if (zone != null)
            {
                return zone;
            }
            List<ZoneConfig> zones = manager.getZonesAt(((ServerPlayer) cause.getAudience()).getServerLocation());
            if (!zones.isEmpty())
            {
                return zones.get(0);
            }
        }

        cause.sendMessage(i18n.translate(cause.getAudience(), MessageType.NEGATIVE, "You need to provide a zone"));
        return null;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        String token = currentInput.toLowerCase();
        List<String> list = new ArrayList<>();
        ServerWorld world = null;
        boolean isLocatable = context.getCause().getAudience() instanceof Locatable;
        if (isLocatable)
        {
            world = ((Locatable) context.getCause().getAudience()).getServerLocation().getWorld();
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
                    && !world.getKey().toString().startsWith(token.replace(".", "")))
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
    public Optional<? extends ZoneConfig> getValue(Parameter.Key<? super ZoneConfig> parameterKey, ArgumentReader.Mutable reader,
            CommandContext.Builder context) throws ArgumentParseException {
        final Audience audience = context.getCause().getAudience();

        final String token = reader.parseString().toLowerCase();
        if (audience instanceof Locatable)
        {
            ServerWorld world = ((Locatable) audience).getServerLocation().getWorld();
            ZoneConfig zone = manager.getZone(token);
            if (zone != null) {
                return Optional.of(zone);
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
        audience.sendMessage(i18n.translate(audience, MessageType.NEGATIVE, "There is no zone named {name}", token));;
        return Optional.empty();
    }

}
