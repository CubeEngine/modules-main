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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.logscribe.LogFactory;
import de.cubeisland.engine.logscribe.target.file.AsyncFileTarget;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.LifeCycle;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.ValueProvider;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.marker.Setup;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.core.CoreModule;
import org.cubeengine.module.roles.commands.ManagementCommands;
import org.cubeengine.module.roles.commands.RoleCommands;
import org.cubeengine.module.roles.commands.RoleInformationCommands;
import org.cubeengine.module.roles.commands.RoleManagementCommands;
import org.cubeengine.module.roles.commands.UserInformationCommands;
import org.cubeengine.module.roles.commands.UserManagementCommands;
import org.cubeengine.module.roles.commands.provider.ContextFormatter;
import org.cubeengine.module.roles.commands.provider.ContextReader;
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
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.service.logging.LoggingUtil.*;

@ModuleInfo(name = "Roles", description = "Manages permissions of players and roles")
/*
TODO generate sample configs on the first run AND/OR cmd to generate samples
TODO role / user permlist clickable red - to remove perm after ok - green + to add (deny/allow?)  - then catch chat tab complete for perm
*/
public class Roles extends Module
{
    private RolesConfig config;

    @Inject private Reflector reflector;
    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private FileManager fm;
    @Inject private Game game;
    @Inject private I18n i18n;
    @Inject private PermissionManager manager;

    @Inject private LogFactory factory;
    @Inject private ThreadFactory threadFactory;

    private RolesPermissionService service;

    private Log permLogger;

    @Setup
    public void onSetup()
    {
        game.getDataManager().register(PermissionData.class, ImmutablePermissionData.class, new PermissionDataBuilder());

        cm.getProviderManager().getExceptionHandler().addHandler(new RolesExceptionHandler(i18n));
        this.permLogger = factory.getLog(CoreModule.class, "Permissions");
        this.permLogger.addTarget(new AsyncFileTarget(getLogFile(fm, "Permissions"), getFileFormat(false, false), false, getCycler(), threadFactory));

        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);

        this.config = fm.loadConfig(this, RolesConfig.class);

        service = new RolesPermissionService(this, reflector, config, game, manager, permLogger);

        ValueProvider<SettableInvocationHandler> provider = new InvocationHandlerProvider(new SettableInvocationHandler());
        getModularity().registerProvider(SettableInvocationHandler.class, provider);

        SettableInvocationHandler handler = getProvided(SettableInvocationHandler.class).with(service);
        handler.meta.forEach(service::registerContextCalculator); // read contextcalculators

        Optional<PermissionService> previous = game.getServiceManager().provide(PermissionService.class);

        PermissionService proxy = (PermissionService)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{PermissionService.class}, handler);

        Object plugin = game.getPluginManager().getPlugin("org.cubeengine").get().getInstance().get();

        game.getServiceManager().setProvider(plugin, PermissionService.class, proxy);
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
        i18n.getCompositor().registerFormatter(new ContextFormatter());
        i18n.getCompositor().registerFormatter(new RoleFormatter());

        cm.getProviderManager().register(this, new ContextReader(service, game), Context.class);
        cm.getProviderManager().register(this, new RoleReader(service), RoleSubject.class);
        cm.getProviderManager().register(this, new DefaultPermissionValueProvider(), Tristate.class);
        cm.getProviderManager().register(this, new PermissionCompleter(service));

        RoleCommands cmdRoles = new RoleCommands(this);
        cm.addCommand(cmdRoles);
        RoleManagementCommands cmdRole = new RoleManagementCommands(this, service, i18n);
        cmdRoles.addCommand(cmdRole);
        cm.addCommands(cmdRole, this, new RoleInformationCommands(this, service, i18n));

        UserManagementCommands cmdUsers = new UserManagementCommands(this, service, i18n);
        cmdRoles.addCommand(cmdUsers);
        cm.addCommands(cmdUsers, this, new UserInformationCommands(this, i18n, service));
        cmdRoles.addCommand(new ManagementCommands(this, service, i18n));
    }

    @Disable
    public void onDisable()
    {
        getProvided(SettableInvocationHandler.class).with(null).and(service.getContextCalculators());
    }

    public RolesConfig getConfiguration()
    {
        return this.config;
    }

    public Log getLog()
    {
        return logger;
    }

    private static class SettableInvocationHandler implements InvocationHandler
    {
        private Object target;
        private List<ContextCalculator> meta = Collections.emptyList();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            try
            {
                return method.invoke(target, args);
            }
            catch (InvocationTargetException e)
            {
                throw e.getCause();
            }
        }

        public SettableInvocationHandler with(Object target)
        {
            this.target = target;
            return this;
        }

        public SettableInvocationHandler and(List<ContextCalculator> meta)
        {
            this.meta = meta;
            return this;
        }
    }

    private static class InvocationHandlerProvider implements ValueProvider<SettableInvocationHandler>
    {
        private SettableInvocationHandler handler;

        public InvocationHandlerProvider(SettableInvocationHandler handler)
        {
            this.handler = handler;
        }

        @Override
        public SettableInvocationHandler get(LifeCycle lifeCycle, Modularity modularity)
        {
            return handler;
        }
    }
}
