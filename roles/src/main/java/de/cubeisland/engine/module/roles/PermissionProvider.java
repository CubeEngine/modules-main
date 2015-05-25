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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.Permission;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.roles.role.DataStore.PermissionValue;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesManager;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedPermission;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.player.User;
import org.spongepowered.api.world.World;

@ServiceImpl(Permission.class)
@Version(1)
public class PermissionProvider implements Permission
{
    private RolesManager manager;
    private PermissionManager permMananger;
    private UserManager um;

    @Inject
    public PermissionProvider(RolesManager manager, PermissionManager permMananger, UserManager um)
    {
        this.manager = manager;
        this.permMananger = permMananger;
        this.um = um;
    }

    @Override
    public String getName()
    {
        return "CubeEngine:Roles";
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public boolean has(World world, User player, String permission)
    {
        if (player.isOnline() && player.getPlayer().getWorld().equals(world))
        {
            return this.has(um.getExactUser(player.getUniqueId()), permission);
        }
        ResolvedPermission rPerm = this.manager.getRolesAttachment(player).getDataHolder(world).getPermissions().get(permission);
        return rPerm == null ? getDefaultPerm(permission, (CommandSender)player) : rPerm.isSet();
    }

    @Override
    public boolean has(CommandSender sender, String permission)
    {
        return sender.hasPermission(permission);
    }

    @Override
    public boolean add(World world, User player, String permission)
    {
        this.manager.getRolesAttachment(player).getDataHolder(world).setPermission(permission, PermissionValue.TRUE);
        return true;
    }

    @Override
    public boolean addTemporary(World world, Player player, String permission)
    {
        this.manager.getRolesAttachment(player).getDataHolder(world).setTempPermission(permission, PermissionValue.TRUE);
        return true;
    }

    @Override
    public boolean remove(World world, User player, String permission)
    {
        this.manager.getRolesAttachment(player).getDataHolder(world).setPermission(permission, PermissionValue.RESET);
        return true;
    }

    @Override
    public boolean removeTemporary(World world, User player, String permission)
    {
        this.manager.getRolesAttachment(player).getDataHolder(world).setTempPermission(permission, PermissionValue.RESET);
        return true;
    }

    @Override
    public boolean has(World world, String roleName, String permission)
    {
        Role role = this.manager.getProvider(world).getRole(roleName);
        if (role == null)
        {
            return false;
        }
        ResolvedPermission resolvedPermission = role.getPermissions().get(permission);
        return resolvedPermission != null && resolvedPermission.isSet();
    }

    private boolean getDefaultPerm(String permission, CommandSender sender)
    {
        switch (permMananger.getDefaultFor(permission))
        {
            case TRUE:
                return true;
            case FALSE:
                return false;
            case OP:
                return sender.isOp();
            case NOT_OP:
                return !sender.isOp();
            default:
                return sender.isOp();
        }
    }

    @Override
    public boolean add(World world, String roleName, String permission)
    {
        Role role = this.manager.getProvider(world).getRole(roleName);
        if (role == null)
        {
            return false;
        }
        role.setPermission(permission, PermissionValue.TRUE);
        return true;
    }

    @Override
    public boolean remove(World world, String roleName, String permission)
    {
        Role role = this.manager.getProvider(world).getRole(roleName);
        if (role == null)
        {
            return false;
        }
        role.setPermission(permission, PermissionValue.RESET);
        return true;
    }

    @Override
    public boolean hasRole(World world, User player, String role)
    {
        return this.manager.getRolesAttachment(player).getDataHolder(world).getRawRoles().contains(role);
    }

    @Override
    public boolean addRole(World world, User player, String roleName)
    {
        Role role = this.manager.getProvider(world).getRole(roleName);
        return role != null && this.manager.getRolesAttachment(player).getDataHolder(world).assignRole(role);
    }

    @Override
    public boolean removeRole(World world, User player, String roleName)
    {
        Role role = this.manager.getProvider(world).getRole(roleName);
        return role != null && this.manager.getRolesAttachment(player).getDataHolder(world).removeRole(role);
    }

    @Override
    public String[] getRoles(World world, User player)
    {
        List<String> list = new ArrayList<>();
        for (Role role : this.manager.getRolesAttachment(player).getDataHolder(world).getRoles())
        {
            list.add(role.getName());
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public String getDominantRole(World world, User player)
    {
        return this.manager.getRolesAttachment(player).getDominantRole(world).getName();
    }

    @Override
    public boolean hasRoleSupport()
    {
        return true;
    }

    @Override
    public String[] getRoles(World world)
    {
        List<String> list = new ArrayList<>();
        for (Role role : this.manager.getProvider(world).getRoles())
        {
            list.add(role.getName());
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getGlobalRoles()
    {
        List<String> list = new ArrayList<>();
        for (Role role : this.manager.getGlobalProvider().getRoles())
        {
            list.add(role.getName());
        }
        return list.toArray(new String[list.size()]);
    }
}
