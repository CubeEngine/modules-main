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

import static org.cubeengine.libcube.service.logging.LoggingUtil.getCycler;
import static org.cubeengine.libcube.service.logging.LoggingUtil.getFileFormat;
import static org.cubeengine.libcube.service.logging.LoggingUtil.getLogFile;

import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.logscribe.target.file.AsyncFileTarget;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.marker.Setup;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.roles.commands.ManagementCommands;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.commands.RoleInformationCommands;
import org.cubeengine.module.roles.commands.RoleManagementCommands;
import org.cubeengine.module.roles.commands.UserInformationCommands;
import org.cubeengine.module.roles.commands.UserManagementCommands;
import org.cubeengine.module.roles.commands.provider.DefaultPermissionValueProvider;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.commands.provider.RoleFormatter;
import org.cubeengine.module.roles.commands.provider.RoleReader;
import org.cubeengine.module.roles.config.PermissionTree;
import org.cubeengine.module.roles.config.PermissionTreeConverter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.data.ImmutablePermissionData;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.data.PermissionDataBuilder;
import org.cubeengine.module.roles.exception.RolesExceptionHandler;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.RoleSubject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

@ModuleInfo(name = "Roles", description = "Manages permissions of players and roles")
/*
TODO generate sample configs on the first run AND/OR cmd to generate samples
TODO role / user permlist clickable red - to remove perm after ok - green + to add (deny/allow?)  - then catch chat tab complete for perm
TODO lookup permissions (via PermissionDescription)
TODO lookup permissions (via Command?)

*/
public class Roles extends Module
{
    private RolesConfig config;

    @Inject private Reflector reflector;
    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private FileManager fm;
    @Inject private I18n i18n;

    @Inject private LogFactory factory;
    @Inject private ThreadFactory threadFactory;
    @Inject private PluginContainer plugin;

    private Log permLogger;
    private RolesPermissionService service;

    public Roles()
    {
        Sponge.getDataManager().register(PermissionData.class, ImmutablePermissionData.class, new PermissionDataBuilder());
    }

    @Setup
    public void onSetup()
    {
        cm.getProviderManager().getExceptionHandler().addHandler(new RolesExceptionHandler(i18n));
        this.permLogger = factory.getLog(LogFactory.class, "Permissions");
        this.permLogger.addTarget(new AsyncFileTarget(getLogFile(fm, "Permissions"), getFileFormat(false, false), false, getCycler(), threadFactory));

        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);

        this.config = fm.loadConfig(this, RolesConfig.class);

        service = (RolesPermissionService)getModularity().provide(PermissionService.class);

        Optional<PermissionService> previous = Sponge.getServiceManager().provide(PermissionService.class);
        Sponge.getServiceManager().setProvider(plugin.getInstance().get(), PermissionService.class, service);
        if (previous.isPresent())
        {
            if (!previous.get().getClass().getName().equals(RolesPermissionService.class.getName()))
            {
                logger.info("Replaced existing Permission Service: {}", previous.get().getClass().getName());
            }
        }
    }

    @Enable
    public void onEnable()
    {
        i18n.getCompositor().registerFormatter(new RoleFormatter());

        cm.getProviderManager().register(this, new RoleReader(service), RoleSubject.class);
        cm.getProviderManager().register(this, new DefaultPermissionValueProvider(), Tristate.class);
        cm.getProviderManager().register(this, new PermissionCompleter(service));

        RoleCommands cmdRoles = new RoleCommands(cm);
        cm.addCommand(cmdRoles);
        RoleManagementCommands cmdRole = new RoleManagementCommands(cm, service, i18n);
        cmdRoles.addCommand(cmdRole);
        cm.addCommands(cmdRole, this, new RoleInformationCommands(cm, service, i18n));

        UserManagementCommands cmdUsers = new UserManagementCommands(cm, service, i18n);
        cmdRoles.addCommand(cmdUsers);
        cm.addCommands(cmdUsers, this, new UserInformationCommands(cm, i18n, service));
        cmdRoles.addCommand(new ManagementCommands(cm, this, service, i18n, plugin));
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
