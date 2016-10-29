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
package org.cubeengine.module.roles.service;

import java.util.Map;

import org.cubeengine.module.roles.RolesUtil;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

public class RolesPermissionDescription implements PermissionDescription
{
    private final PermissionService permissionService;
    private final String id;
    private final Text description;
    private final PluginContainer owner;

    public RolesPermissionDescription(PermissionService permissionService, String id, Text description,
                                      PluginContainer owner)
    {
        this.permissionService = permissionService;
        this.id = id;
        this.description = description;
        this.owner = owner;
        RolesUtil.allPermissions.add(id);
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Text getDescription()
    {
        return this.description;
    }

    @Override
    public Map<Subject, Boolean> getAssignedSubjects(String type)
    {
        return this.permissionService.getSubjects(type).getAllWithPermission(this.id);
    }

    @Override
    public PluginContainer getOwner()
    {
        return this.owner;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof RolesPermissionDescription))
        {
            return false;
        }

        final RolesPermissionDescription that = (RolesPermissionDescription)o;

        if (id != null ? !id.equals(that.id) : that.id != null)
        {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null)
        {
            return false;
        }
        return !(owner != null ? !owner.equals(that.owner) : that.owner != null);
    }

    @Override
    public int hashCode()
    {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        return result;
    }
}
