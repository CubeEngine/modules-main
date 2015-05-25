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
package de.cubeisland.engine.module.portals;

import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.DefaultValue;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.service.user.User;

public class PortalReader implements ArgumentReader<Portal>, DefaultValue<Portal>
{
    private Portals module;

    public PortalReader(Portals module)
    {
        this.module = module;
    }

    @Override
    public Portal read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String portalName = invocation.consume(1);
        Portal portal = this.module.getPortalManager().getPortal(portalName);
        if (portal == null)
        {
            throw new ReaderException("Portal {input} not found", portalName);
        }
        return portal;
    }

    @Override
    public Portal getDefault(CommandInvocation invocation)
    {
        Portal portal = null;
        if (invocation.getCommandSource() instanceof User)
        {
            portal = ((User)invocation.getCommandSource()).attachOrGet(PortalsAttachment.class, module).getPortal();
        }
        if (portal == null)
        {
            throw new ReaderException("You need to define a portal to use");
        }
        return portal;
    }
}
