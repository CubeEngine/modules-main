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
package org.cubeengine.module.portals;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.spongepowered.api.entity.living.player.Player;

public class PortalParser implements ArgumentParser<Portal>, DefaultValue<Portal>, Completer
{
    private Portals module;

    public PortalParser(Portals module)
    {
        this.module = module;
    }

    @Override
    public Portal parse(Class type, CommandInvocation invocation) throws ParserException
    {
        String portalName = invocation.consume(1);
        Portal portal = this.module.getPortal(portalName);
        if (portal == null)
        {
            throw new ParserException("Portal {input} not found", portalName);
        }
        return portal;
    }

    @Override
    public Portal getDefault(CommandInvocation invocation)
    {
        Portal portal = null;
        if (invocation.getCommandSource() instanceof Player)
        {
            portal = module.getPortalsAttachment(((Player)invocation.getCommandSource()).getUniqueId()).getPortal();
        }
        if (portal == null)
        {
            throw new ParserException("You need to define a portal to use");
        }
        return portal;
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        List<String> list = new ArrayList<>();
        String token = invocation.currentToken();
        for (Portal portal : module.getPortals())
        {
            if (portal.getName().startsWith(token))
            {
                list.add(portal.getName());
            }
        }
        return list;
    }
}
