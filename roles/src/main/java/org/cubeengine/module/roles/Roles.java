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
package org.cubeengine.module.roles;

import org.cubeengine.converter.ConverterManager;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.cubeengine.logscribe.Log;
import org.cubeengine.logscribe.LogFactory;
import org.cubeengine.logscribe.target.file.AsyncFileTarget;
import org.cubeengine.module.roles.commands.ManagementCommands;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.commands.RoleInformationCommands;
import org.cubeengine.module.roles.commands.RoleManagementCommands;
import org.cubeengine.module.roles.commands.UserInformationCommands;
import org.cubeengine.module.roles.commands.UserManagementCommands;
import org.cubeengine.module.roles.commands.provider.DefaultPermissionValueProvider;
import org.cubeengine.module.roles.commands.provider.PermissionCompleter;
import org.cubeengine.module.roles.commands.provider.RoleFormatter;
import org.cubeengine.module.roles.commands.provider.RoleParser;
import org.cubeengine.module.roles.config.PermissionTree;
import org.cubeengine.module.roles.config.PermissionTreeConverter;
import org.cubeengine.module.roles.config.Priority;
import org.cubeengine.module.roles.config.PriorityConverter;
import org.cubeengine.module.roles.data.IPermissionData;
import org.cubeengine.module.roles.data.ImmutablePermissionData;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.data.PermissionDataBuilder;
import org.cubeengine.module.roles.exception.RolesExceptionHandler;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.module.roles.service.subject.FileSubject;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/*
TODO generate sample configs on the first run AND/OR cmd to generate samples
TODO role / user permlist clickable red - to remove perm after ok - green + to add (deny/allow?)  - then catch chat tab complete for perm
TODO lookup permissions (via PermissionDescription)
TODO lookup permissions (via Command?)
TODO SubjectDataUpdateEvent
*/
@Singleton
@Module
public class Roles extends CubeEngineModule
{
    @Inject private CommandManager cm;
    @Inject private FileManager fm;
    @Inject private I18n i18n;

    @Inject private LogFactory factory;
    @Inject private PluginContainer plugin;
    @Inject private ModuleManager mm;

    private Log permLogger;
    @Inject private RolesPermissionService service;

    @Inject
    public Roles(Reflector reflector, PluginContainer plugin)
    {
        DataRegistration<PermissionData, ImmutablePermissionData> dr = DataRegistration.<PermissionData, ImmutablePermissionData>builder()
                        .dataClass(PermissionData.class).immutableClass(ImmutablePermissionData.class)
                        .builder(new PermissionDataBuilder()).manipulatorId("permission")
                        .dataName("CubeEngine Roles Permissions")
                        .buildAndRegister(plugin);

        IPermissionData.OPTIONS.getQuery();

        Sponge.getDataManager().registerLegacyManipulatorIds(PermissionData.class.getName(), dr);

        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);
    }

    @Listener
    public void onSetup(GamePreInitializationEvent event)
    {
        cm.getProviders().getExceptionHandler().addHandler(new RolesExceptionHandler(i18n));
        this.permLogger = factory.getLog(LogFactory.class, "Permissions");
        ThreadFactory threadFactory = mm.getThreadFactory(Roles.class);
        this.permLogger.addTarget(
                new AsyncFileTarget.Builder(LoggingUtil.getLogFile(fm, "Permissions").toPath(),
                        LoggingUtil.getFileFormat(false, true)
                ).setAppend(true).setCycler(LoggingUtil.getCycler()).setThreadFactory(threadFactory).build());

        Optional<PermissionService> previous = Sponge.getServiceManager().provide(PermissionService.class);
        Sponge.getServiceManager().setProvider(plugin.getInstance().get(), PermissionService.class, service);
        if (previous.isPresent())
        {
            if (!previous.get().getClass().getName().equals(RolesPermissionService.class.getName()))
            {
                this.service.getLog().info("Replaced existing Permission Service: {}", previous.get().getClass().getName());
            }
        }
    }

    @Listener
    public void onEnable(GameInitializationEvent event)
    {
        i18n.getCompositor().registerFormatter(new RoleFormatter());

        cm.getProviders().register(this, new RoleParser(service), FileSubject.class);
        cm.getProviders().register(this, new DefaultPermissionValueProvider(), Tristate.class);
        cm.getProviders().register(this, new PermissionCompleter(service));

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
        return this.service.getConfig();
    }

    public RolesPermissionService getService() {
        return service;
    }
}
