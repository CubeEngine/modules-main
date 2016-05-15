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
package org.cubeengine.module.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class RolesConfig extends ReflectedYaml
{
    @Name("disable-permission-in-offlinemode")
    @Comment("If this is set to true no permissions will be assigned to any user if the server runs in offline mode")
    public boolean doNotAssignPermIfOffline = true;
    @Name("default-roles")
    @Comment("The list of roles a user will get when first joining the server.\n" +
                 "default-roles:\n" +
                 "      - guest\n")
    public List<String> defaultRoles = new ArrayList<>();

    @Comment("When enabled provides a lot of additional logging")
    public boolean debug = false;
}
