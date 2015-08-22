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
package org.cubeengine.module.roles.commands.provider;

import org.spongepowered.api.service.permission.context.Context;

public class ContextualRole
{
    public String contextType;
    public String contextName;
    public String roleName;

    private Context context;
    private String identifier;

    public String getIdentifier()
    {
        if (identifier == null)
        {
            identifier = "role:" + contextType + (contextName.isEmpty() ? "" : "|" + contextName) + "|" + roleName;
        }
        return identifier;
    }

    public Context getContext()
    {
        if (context == null)
        {
            context = new Context(contextType, contextName);
        }
        return context;
    }
}
