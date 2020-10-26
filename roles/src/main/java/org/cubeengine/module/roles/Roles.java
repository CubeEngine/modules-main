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

import java.util.concurrent.ThreadFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.cubeengine.logscribe.Log;
import org.cubeengine.logscribe.LogFactory;
import org.cubeengine.logscribe.target.file.AsyncFileTarget;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.commands.formatter.RoleFormatter;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;

/*
TODO generate sample configs on the first run AND/OR cmd to generate samples
TODO role / user permlist clickable red - to remove perm after ok - green + to add (deny/allow?)  - then catch chat tab complete for perm
TODO lookup permissions (via PermissionDescription)
TODO lookup permissions (via Command?)
TODO SubjectDataUpdateEvent
*/
@Singleton
@Module
public class Roles
{
    @Inject private FileManager fm;
    @Inject private I18n i18n;

    @Inject private LogFactory factory;
    @Inject private ModuleManager mm;

    private Log permLogger;
    @Inject private RolesPermissionService service;
    @ModuleCommand private RoleCommands roleCommands;

    @Listener
    public void onSetup(StartingEngineEvent<Server> event)
    {
        this.permLogger = factory.getLog(LogFactory.class, "Permissions");
        ThreadFactory threadFactory = mm.getThreadFactory(Roles.class);
        this.permLogger.addTarget(
                new AsyncFileTarget.Builder(LoggingUtil.getLogFile(fm, "Permissions").toPath(),
                        LoggingUtil.getFileFormat(false, true)
                ).setAppend(true).setCycler(LoggingUtil.getCycler()).setThreadFactory(threadFactory).build());
    }

    @Listener
    public void onRegisterData(RegisterCatalogEvent<DataRegistration> event)
    {
        PermissionData.register(event);
    }

    @Listener
    public void onProvideService(ProvideServiceEvent<PermissionService> event)
    {
        event.suggest(this::getService);
    }

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        i18n.getCompositor().registerFormatter(new RoleFormatter());
    }

    public RolesConfig getConfiguration()
    {
        return this.service.getConfig();
    }

    public RolesPermissionService getService()
    {
        return service;
    }
}
