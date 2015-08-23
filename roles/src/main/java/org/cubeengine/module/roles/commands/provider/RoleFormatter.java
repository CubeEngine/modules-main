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

import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.cubeengine.dirigent.Component;
import org.cubeengine.dirigent.formatter.AbstractFormatter;
import org.cubeengine.dirigent.formatter.Context;
import org.cubeengine.dirigent.parser.component.Text;

public class RoleFormatter extends AbstractFormatter<RoleSubject>
{
    public RoleFormatter()
    {
        super("role");
    }

    @Override
    public Component format(RoleSubject object, Context context)
    {
        return new Text(object.getName());
    }
}