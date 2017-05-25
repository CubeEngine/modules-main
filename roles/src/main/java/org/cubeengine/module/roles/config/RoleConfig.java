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
package org.cubeengine.module.roles.config;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class RoleConfig extends ReflectedYaml
{
    @Comment("Unique Identifier for this role. Do not change!")
    public UUID identifier;

    @Name("role-name")
    @Comment("The name of this role")
    public String roleName = "defaultName";
    @Name("priority")
    @Comment("Use these as priority or just numbers\n"
        + "ABSULTEZERO(-273) < MINIMUM(0) < LOWEST(125) < LOWER(250) < LOW(375) < NORMAL(500) < HIGH(675) < HIGHER(750) < HIGHEST(1000) < OVER9000(9001)")
    public Priority priority = Priority.ABSULTEZERO;

    @Comment("The settings for this role grouped by context\n" +
            "context type and name are separated by |\n" +
            "the world context is the default context and its type can be omitted\n" +
            "for global context use global without separator\n" +
            "\n" +
            "permission nodes can be assigned individually e.g.:\n" +
            "   - cubeengine.roles.command.assign\n" +
            "or grouped into a tree (this will be done automatically) like this:\n" +
            " - cubeengine.roles:\n" +
            "     - command.assign\n" +
            "     - world.world:\n" +
            "         - guest\n" +
            "         - member\n" +
            "Use - directly in front of a permission to revoke that permission e.g.:\n" +
            " - -cubeengine.roles.command.assign\n" +
            "\n" +
            "parents are the roles this one will inherit from\n" +
            "\n" +
            "options can contain any String Key-Value data e.g.:\n" +
            "  prefix: '&7Guest'")
    public Map<String, ContextSetting> settings = new HashMap<>();
    {
        {
            settings.put("global", new ContextSetting());
        }
    }

    @Override
    public void onLoaded(File loadFrom) {
        if (this.priority == null)
        {
            this.priority = Priority.ABSULTEZERO;
        }
        if (settings.isEmpty())
        {
            ContextSetting setting = new ContextSetting();
            settings.put("global", setting);
            setting.options.put("key", "value");
            setting.permissions.getPermissions().put("no.perm", false);
        }
        for (ContextSetting setting : settings.values())
        {
            if (setting.parents == null)
            {
                setting.parents = new HashSet<>();
            }
            if (setting.options == null)
            {
                setting.options = new LinkedHashMap<>();
            }
        }
        if (identifier == null)
        {
            identifier = UUID.randomUUID();
        }
    }

    public static class ContextSetting implements Section
    {
        public PermissionTree permissions = new PermissionTree();
        public Set<String> parents = new HashSet<>();
        public Map<String, String> options = new LinkedHashMap<>();
    }
}
