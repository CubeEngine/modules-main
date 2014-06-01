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
package de.cubeisland.engine.module.roles;

import org.bukkit.World;

import de.cubeisland.engine.core.module.service.Metadata;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RoleProvider;
import de.cubeisland.engine.module.roles.role.RolesManager;
import de.cubeisland.engine.module.roles.role.UserDatabaseStore;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedMetadata;

public class MetadataProvider implements Metadata
{
    private RolesManager rolesManager;

    public MetadataProvider(RolesManager rolesManager)
    {
        this.rolesManager = rolesManager;
    }

    @Override
    public String setMetadata(User user, String key, String value)
    {
        return setMetadata(user, user.getWorld(), key, value);
    }

    @Override
    public String getMetadata(User user, String key)
    {
        return getMetadata(user, user.getWorld(), key);
    }

    @Override
    public String setMetadata(User user, World world, String key, String value)
    {
        UserDatabaseStore dataHolder = this.rolesManager.getRolesAttachment(user).getDataHolder(world);
        ResolvedMetadata resolvedMetadata = dataHolder.getMetadata().get(key);
        dataHolder.setMetadata(key, value);
        return resolvedMetadata == null ? null : resolvedMetadata.getValue();
    }

    @Override
    public String getMetadata(User user, World world, String key)
    {
        UserDatabaseStore dataHolder = this.rolesManager.getRolesAttachment(user).getDataHolder(world);
        ResolvedMetadata resolvedMetadata = dataHolder.getMetadata().get(key);
        return resolvedMetadata == null ? null : resolvedMetadata.getValue();
    }

    @Override
    public String getRoleMetadata(String roleName, World world, String key)
    {
        RoleProvider provider;
        if (world == null)
        {
            provider = this.rolesManager.getGlobalProvider();
        }
        else
        {
            provider = this.rolesManager.getProvider(world);
        }
        if (provider == null)
        {
            return null;
        }
        Role role = provider.getRole(roleName);
        if (role == null)
        {
            return null;
        }
        ResolvedMetadata metadata = role.getMetadata().get(key);
        if (metadata == null)
        {
            return null;
        }
        return metadata.getValue();
    }

    @Override
    public String setRoleMetadata(String roleName, World world, String key, String value)
    {
        RoleProvider provider;
        if (world == null)
        {
            provider = this.rolesManager.getGlobalProvider();
        }
        else
        {
            provider = this.rolesManager.getProvider(world);
        }
        if (provider == null)
        {
            return null;
        }
        Role role = provider.getRole(roleName);
        if (role == null)
        {
            return null;
        }
        String old = role.setMetadata(key, value);
        role.save();
        return old;
    }

    @Override
    public String getName()
    {
        return "CubeEngine:Roles";
    }
}
