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

import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.filesystem.FileManager;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.roles.commands.ManagementCommands;
import de.cubeisland.engine.module.roles.commands.RoleCommands;
import de.cubeisland.engine.module.roles.commands.RoleInformationCommands;
import de.cubeisland.engine.module.roles.commands.RoleManagementCommands;
import de.cubeisland.engine.module.roles.commands.UserInformationCommands;
import de.cubeisland.engine.module.roles.commands.UserManagementCommands;
import de.cubeisland.engine.module.roles.config.PermissionTree;
import de.cubeisland.engine.module.roles.config.PermissionTreeConverter;
import de.cubeisland.engine.module.roles.config.Priority;
import de.cubeisland.engine.module.roles.config.PriorityConverter;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import org.spongepowered.api.util.Tristate;
import de.cubeisland.engine.module.roles.role.RolesEventHandler;
import de.cubeisland.engine.module.roles.storage.TableOption;
import de.cubeisland.engine.module.roles.storage.TablePerm;
import de.cubeisland.engine.module.roles.storage.TableRole;
import de.cubeisland.engine.module.service.database.Database;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.user.UserManager;
import de.cubeisland.engine.reflect.Reflector;

@ModuleInfo(name = "Roles", description = "Manages permissions of players and roles")
public class Roles extends Module
{
    private RolesConfig config;

    @Inject private Reflector reflector;
    @Inject private Database db;
    @Inject private Log logger;
    @Inject private UserManager um;
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private TaskManager tm;
    @Inject private FileManager fm;
    @Inject private WorldManager wm;
    @Inject private PermissionManager pm;

    @Enable
    public void onEnable()
    {
        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);

        db.registerTable(TableRole.class);
        db.registerTable(TablePerm.class);
        db.registerTable(TableOption.class);

        cm.getProviderManager().register(this, new DefaultPermissionValueProvider(), Tristate.class);
        RoleCommands cmdRoles = new RoleCommands(this);
        cm.addCommand(cmdRoles);
        RoleManagementCommands cmdRole = new RoleManagementCommands(this, wm);
        cmdRoles.addCommand(cmdRole);
        cm.addCommands(cmdRole, this, new RoleInformationCommands(this, wm));

        UserManagementCommands cmdUsers = new UserManagementCommands(this, wm);
        cmdRoles.addCommand(cmdUsers);
        cm.addCommands(cmdUsers, this, new UserInformationCommands(this, pm, wm));
        cmdRoles.addCommand(new ManagementCommands(this));

        em.registerListener(this, new RolesEventHandler(this));

        this.config = fm.loadConfig(this, RolesConfig.class);
        tm.runTask(this, () -> {
            rolesManager.initRoleProviders();
            rolesManager.recalculateAllRoles();
        });
    }

    @Disable
    public void onDisable()
    {
        em.removeListeners(this);
    }

    public RolesConfig getConfiguration()
    {
        return this.config;
    }

    public Log getLog()
    {
        return logger;
    }
}
