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
package org.cubeengine.module.roles.service.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.cubeengine.module.roles.RolesConfig;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;

public class DefaultSubjectData extends BaseSubjectData
{
    private RolesConfig config;

    public DefaultSubjectData(RolesPermissionService service, RolesConfig config)
    {
        super(service);
        this.config = config;
        this.load();
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent)
    {
        if (super.addParent(contexts, parent))
        {
            config.defaultRoles.add(parent.getIdentifier());
            config.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent)
    {
        config.defaultRoles.remove(parent.getIdentifier());
        config.save();
        return super.removeParent(contexts, parent);
    }

    @Override
    public boolean clearParents(Set<Context> contexts)
    {
        config.defaultRoles.clear();
        config.save();
        return super.clearParents(contexts);
    }

    public void load()
    {
        parents.clear();
        List<Subject> list = new ArrayList<>();
        parents.put(GLOBAL, list);
        for (String name : config.defaultRoles)
        {
            RoleSubject role = service.getGroupSubjects().get(name);
            list.add(role);
        }
    }

    // TODO overrides for add/remove/clear Parent change configuration
}
