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
import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.portals.Portal;
import org.cubeengine.module.portals.Portals;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@Singleton
@ParserFor(Portal.class)
public class PortalParser implements ValueParser<Portal>, ValueCompleter, DefaultParameterProvider<Portal>
{
    private Portals module;
    private I18n i18n;

    @Inject
    public PortalParser(Portals module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Override
    public Portal apply(CommandCause commandCause)
    {
        Portal portal = null;
        if (commandCause.audience() instanceof ServerPlayer)
        {
            portal = module.getPortalsAttachment(((ServerPlayer)commandCause.audience()).uniqueId()).getPortal();
        }
        if (portal == null)
        {
            // TODO error message
            return null;
//            throw new ArgumentParseException(i18n.translate(commandCause,"You need to define a portal to use"), "", 0);
        }
        return portal;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        List<String> list = new ArrayList<>();
        for (Portal portal : module.getPortals())
        {
            if (portal.getName().startsWith(currentInput))
            {
                list.add(portal.getName());
            }
        }
        return list;
    }

    @Override
    public Optional<? extends Portal> parseValue(Key<? super Portal> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String portalName = reader.parseString();
        Portal portal = this.module.getPortal(portalName);
        if (portal == null)
        {
            throw reader.createException(i18n.translate(context.cause(), "Portal {input} not found", portalName));
        }
        return Optional.of(portal);
    }

}
