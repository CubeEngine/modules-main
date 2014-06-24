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

import de.cubeisland.engine.core.command.CommandManager;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.service.Metadata;
import de.cubeisland.engine.core.module.service.Permission;
import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.module.roles.commands.ManagementCommands;
import de.cubeisland.engine.module.roles.commands.RoleCommands;
import de.cubeisland.engine.module.roles.commands.RoleInformationCommands;
import de.cubeisland.engine.module.roles.commands.RoleManagementCommands;
import de.cubeisland.engine.module.roles.commands.UserInformationCommands;
import de.cubeisland.engine.module.roles.commands.UserManagementCommands;
import de.cubeisland.engine.module.roles.config.MirrorConfig;
import de.cubeisland.engine.module.roles.config.MirrorConfigConverter;
import de.cubeisland.engine.module.roles.config.PermissionTree;
import de.cubeisland.engine.module.roles.config.PermissionTreeConverter;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.PriorityConverter;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.RolesEventHandler;
import de.cubeisland.engine.module.roles.role.RolesManager;
import de.cubeisland.engine.module.roles.storage.TableData;
import de.cubeisland.engine.module.roles.storage.TablePerm;
import de.cubeisland.engine.module.roles.storage.TableRole;
import de.cubeisland.engine.reflect.codec.ConverterManager;

public class Roles extends Module
{
    private RolesConfig config;
    private RolesManager rolesManager;

    @Override
    public void onEnable()
    {
        ConverterManager cManager = this.getCore().getConfigFactory().getDefaultConverterManager();
        cManager.registerConverter(PermissionTree.class, new PermissionTreeConverter(this));
        cManager.registerConverter(Priority.class, new PriorityConverter());
        cManager.registerConverter(MirrorConfig.class, new MirrorConfigConverter(this));

        Database db = this.getCore().getDB();
        db.registerTable(TableRole.class);
        db.registerTable(TablePerm.class);
        db.registerTable(TableData.class);

        this.rolesManager = new RolesManager(this);

        this.getCore().getUserManager().addDefaultAttachment(RolesAttachment.class, this);

        final CommandManager cm = this.getCore().getCommandManager();
        cm.registerCommand(new RoleCommands(this));
        cm.registerCommand(new RoleManagementCommands(this), "roles");
        cm.registerCommands(this, new RoleInformationCommands(this), "roles", "role");
        cm.registerCommand(new UserManagementCommands(this), "roles");
        cm.registerCommands(this, new UserInformationCommands(this), "roles", "user");
        cm.registerCommand(new ManagementCommands(this), "roles");

        this.getCore().getEventManager().registerListener(this, new RolesEventHandler(this));

        this.config = loadConfig(RolesConfig.class);
        this.getCore().getTaskManager().runTask(this, new Runnable()
        {
            @Override
            public void run()
            {
                rolesManager.initRoleProviders();
                rolesManager.recalculateAllRoles();
            }
        });

        this.getCore().getModuleManager().getServiceManager().registerService(this, Metadata.class, new MetadataProvider(this.rolesManager));
        this.getCore().getModuleManager().getServiceManager().registerService(this, Permission.class, new PermissionProvider(this, this.rolesManager, this.getCore().getPermissionManager()));
    }

    @Override
    public void onDisable()
    {
        this.getCore().getEventManager().removeListeners(this);
    }

    public RolesConfig getConfiguration()
    {
        return this.config;
    }

    public RolesManager getRolesManager()
    {
        return this.rolesManager;
    }
}
