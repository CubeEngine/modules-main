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
package org.cubeengine.module.roles.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

public class RolesPermissionDescriptionBuilder implements Builder
{
    private final PluginContainer owner;
    private final RolesPermissionService permissionService;
    private String id;
    private Text description;
    private final Map<String, Tristate> roleAssignments = new LinkedHashMap<String, Tristate>();

    public RolesPermissionDescriptionBuilder(PluginContainer owner, RolesPermissionService permissionService)
    {
        this.owner = owner;
        this.permissionService = permissionService;
    }


    @Override
    // TODO template-parts (<part>)
    public Builder id(String permissionId)
    {
        this.id = permissionId;
        return this;
    }

    @Override
    public Builder description(Text description)
    {
        this.description = description;
        return this;
    }

    @Override
    public Builder assign(String role, boolean value)
    {
        this.roleAssignments.put(role, Tristate.fromBoolean(value));
        return this;
    }

    @Override
    public PermissionDescription register() throws IllegalStateException
    {
        return permissionService.addDescription(new RolesPermissionDescription(permissionService, id, description, owner), roleAssignments);
    }
}
