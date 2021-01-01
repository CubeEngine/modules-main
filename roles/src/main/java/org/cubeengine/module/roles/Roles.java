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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ThreadFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.logging.LoggingUtil;
import org.cubeengine.libcube.util.ContextUtil;
import org.cubeengine.logscribe.Log;
import org.cubeengine.logscribe.LogFactory;
import org.cubeengine.logscribe.target.file.AsyncFileTarget;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.commands.formatter.RoleFormatter;
import org.cubeengine.module.roles.data.PermissionData;
import org.cubeengine.module.roles.service.RolesPermissionService;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

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

    @Inject private Path path;
    private boolean firstRun;

    @Listener
    public void onSetup(StartingEngineEvent<Server> event)
    {
        this.firstRun = !Files.exists(path.resolve("delete_me_if_you_need_permissions"));
        this.permLogger = factory.getLog(LogFactory.class, "Permissions");
        ThreadFactory threadFactory = mm.getThreadFactory(Roles.class);
        this.permLogger.addTarget(
                new AsyncFileTarget.Builder(LoggingUtil.getLogFile(fm, "Permissions").toPath(),
                        LoggingUtil.getFileFormat(false, true)
                ).setAppend(true).setCycler(LoggingUtil.getCycler()).setThreadFactory(threadFactory).build());
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
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

    @Listener
    public void onFirstJoin(ServerSideConnectionEvent.Join event)
    {
        if (this.firstRun)
        {
            try
            {
                Files.createFile(path.resolve("delete_me_if_you_need_permissions"));
                this.firstRun = false;
            }
            catch (IOException e)
            {
                throw new IllegalStateException();
            }
            event.getPlayer().getTransientSubjectData().setPermission(Collections.singleton(ContextUtil.GLOBAL), "*", Tristate.TRUE);
            i18n.send(event.getPlayer(), MessageType.POSITIVE, "Welcome to your new Minecraft Server. You are the first to join this server!");
            i18n.send(event.getPlayer(), MessageType.POSITIVE, "As such {text:Roles} gave you temporarily all permissions.");
            i18n.send(event.getPlayer(), MessageType.CRITICAL, "Before you leave, remember to give yourself actual permissions!");

        }
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
