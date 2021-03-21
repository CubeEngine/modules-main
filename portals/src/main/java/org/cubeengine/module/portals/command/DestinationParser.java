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
package org.cubeengine.module.portals.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.cubeengine.module.portals.config.Destination;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
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
import org.spongepowered.api.world.server.ServerWorld;

@Singleton
@ParserFor(Destination.class)
public class DestinationParser implements ValueParser<Destination>, ValueCompleter
{
    private final Portals module;
    private I18n i18n;
    private final Random random = new Random();

    @Inject
    public DestinationParser(Portals module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Override
    public Optional<? extends Destination> parseValue(Key<? super Destination> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String token = reader.parseString();
        if ("here".equalsIgnoreCase(token))
        {
            if (context.cause().audience() instanceof ServerPlayer)
            {
                final ServerPlayer player = (ServerPlayer)context.cause().audience();
                return Optional.of(new Destination(player.serverLocation(), player.rotation(), i18n));
            }
            throw reader.createException(
                i18n.translate(context.cause(), "The Portal Agency will bring you your portal for just {text:$ 1337} within {input#amount} weeks",
                               String.valueOf(random.nextInt(51) + 1)));
        }
        else if (token.startsWith("p:")) // portal dest
        {
            Portal destPortal = module.getPortal(token.substring(2));
            if (destPortal == null)
            {
                throw reader.createException(i18n.translate(context.cause(), "Portal {input} not found!", token.substring(2)));
            }
            return Optional.of(new Destination(destPortal));
        }
        else // world
        {
            Optional<ServerWorld> world = Sponge.server().worldManager().world(ResourceKey.resolve(token));
            if (!world.isPresent())
            {
                throw reader.createException(i18n.translate(context.cause(), "World {input} not found!", token));
            }
            return world.map(Destination::new);
        }
    }

    @Override
    public List<ClientCompletionType> clientCompletionType()
    {
        return Collections.singletonList(ClientCompletionTypes.RESOURCE_KEY.get());
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        final List<String> list = new ArrayList<>();
        if (currentInput.toLowerCase().startsWith("here"))
        {
            list.add("here");
        }

        if (currentInput.toLowerCase().startsWith("p:"))
        {
            final String portalInput = currentInput.substring(2);
            for (Portal portal : module.getPortals())
            {
                if (portal.getName().startsWith(portalInput))
                {
                    list.add("p:" + portal.getName());
                }
            }
        }
        else
        {
            for (ServerWorld world : Sponge.server().worldManager().worlds())
            {
                if (world.key().toString().startsWith(currentInput))
                {
                    list.add(world.key().toString());
                }
            }
        }
        return list;
    }
}
