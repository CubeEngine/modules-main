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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.cubeengine.module.roles.RolesUtil;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.plugin.PluginContainer;

public class RolesPermissionDescription implements PermissionDescription
{
    private static final Pattern PLACEHOLDER = Pattern.compile("\\.<[a-zA-Z0-9_-]+>");

    private final PermissionService permissionService;
    private final String id;
    private final Component description;
    private final PluginContainer owner;
    private final String strippedId;
    private Tristate defaultValue;

    public RolesPermissionDescription(PermissionService permissionService, String id, Component description, PluginContainer owner, Tristate defaultValue)
    {
        this.permissionService = permissionService;
        this.id = id;
        this.description = description;
        this.owner = owner;
        this.defaultValue = defaultValue;
        this.strippedId = PLACEHOLDER.matcher(this.id).replaceAll("");
        RolesUtil.allPermissions.add(id);
    }

    @Override
    public String id()
    {
        return this.id;
    }

    @Override
    public Optional<Component> description()
    {
        return Optional.ofNullable(this.description);
    }

    @Override
    public Map<? extends Subject, Boolean> assignedSubjects(String type)
    {
        return this.permissionService.collection(type).get().loadedWithPermission(this.id);
    }

    @Override
    public Optional<PluginContainer> owner()
    {
        return Optional.ofNullable(this.owner);
    }

    @Override
    public CompletableFuture<? extends Map<? extends SubjectReference, Boolean>> findAssignedSubjects(String type)
    {
        return this.permissionService.collection(type).get().allWithPermission(this.id);
    }

    @Override
    public boolean query(Subject subj)
    {
        return subj.hasPermission(this.strippedId);
    }

    @Override
    public boolean query(Subject subj, ResourceKey key)
    {
        return subj.hasPermission(this.strippedId + '.' + key.namespace() + '.' + key.value());
    }

    @Override
    public boolean query(Subject subj, String... parameters)
    {
        if (parameters.length == 0) {
            return this.query(subj);
        } else if (parameters.length == 1) {
            return this.query(subj, parameters[0]);
        }
        final StringBuilder build = new StringBuilder(this.strippedId);
        for (final String parameter : parameters) {
            build.append('.').append(parameter);
        }
        return subj.hasPermission(build.toString());
    }

    @Override
    public boolean query(Subject subj, String parameter)
    {
        final String extendedPermission = this.strippedId + '.' + Objects.requireNonNull(parameter, "parameter");
        return subj.hasPermission(extendedPermission);
    }

    @Override
    public Tristate defaultValue()
    {
        return this.defaultValue;
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
