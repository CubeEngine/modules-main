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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.ValueProvider;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.service.filesystem.FileManager;
import de.cubeisland.engine.service.i18n.I18n;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.roles.commands.provider.ContextFormatter;
import de.cubeisland.engine.module.roles.commands.provider.ContextReader;
import de.cubeisland.engine.module.roles.commands.provider.ContextualRole;
import de.cubeisland.engine.module.roles.commands.provider.ContextualRoleReader;
import de.cubeisland.engine.module.roles.commands.provider.DefaultPermissionValueProvider;
import de.cubeisland.engine.module.roles.commands.provider.PermissionCompleter;
import de.cubeisland.engine.module.roles.commands.provider.RoleFormatter;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.service.command.CommandManager;
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
import de.cubeisland.engine.service.permission.PermissionManager;
import de.cubeisland.engine.service.world.WorldManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.util.Tristate;
import de.cubeisland.engine.module.roles.storage.TableOption;
import de.cubeisland.engine.module.roles.storage.TablePerm;
import de.cubeisland.engine.module.roles.storage.TableRole;
import de.cubeisland.engine.service.database.Database;
import de.cubeisland.engine.service.task.TaskManager;
import de.cubeisland.engine.service.user.UserManager;
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
    @Inject private Game game;
    @Inject private I18n i18n;
    @Inject private PermissionManager manager;
    private RolesPermissionService service;

    @Enable
    public void onEnable()
    {
        ConverterManager cManager = reflector.getDefaultConverterManager();
        cManager.registerConverter(new PermissionTreeConverter(this), PermissionTree.class);
        cManager.registerConverter(new PriorityConverter(), Priority.class);
        this.config = fm.loadConfig(this, RolesConfig.class);

        i18n.getCompositor().registerFormatter(new ContextFormatter());
        i18n.getCompositor().registerFormatter(new RoleFormatter());

        db.registerTable(TableRole.class);
        db.registerTable(TablePerm.class);
        db.registerTable(TableOption.class);

        service = new RolesPermissionService(this, reflector, config, game, db, wm, manager);

        cm.getProviderManager().register(this, new ContextReader(service, wm), Context.class);
        cm.getProviderManager().register(this, new ContextualRoleReader(service, wm), ContextualRole.class);
        cm.getProviderManager().register(this, new DefaultPermissionValueProvider(), Tristate.class);
        cm.getProviderManager().register(this, new PermissionCompleter(pm));

        RoleCommands cmdRoles = new RoleCommands(this);
        cm.addCommand(cmdRoles);
        RoleManagementCommands cmdRole = new RoleManagementCommands(this, service);
        cmdRoles.addCommand(cmdRole);
        cm.addCommands(cmdRole, this, new RoleInformationCommands(this, service));

        UserManagementCommands cmdUsers = new UserManagementCommands(this, service);
        cmdRoles.addCommand(cmdUsers);
        cm.addCommands(cmdUsers, this, new UserInformationCommands(this));
        cmdRoles.addCommand(new ManagementCommands(this));

        ValueProvider<SettableInvocationHandler> provider = getModularity().getProvider(SettableInvocationHandler.class);
        if (provider == null)
        {
            provider = new InvocationHandlerProvider(new SettableInvocationHandler());
            getModularity().registerProvider(SettableInvocationHandler.class, provider);
        }
        SettableInvocationHandler handler = getProvided(SettableInvocationHandler.class).with(service);
        handler.meta.forEach(service::registerContextCalculator); // readd contextcalculators

        if (!game.getServiceManager().provide(PermissionService.class).isPresent())
        {
            PermissionService proxy = (PermissionService)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{PermissionService.class}, handler);
            try
            {
                game.getServiceManager().setProvider(game.getPluginManager().getPlugin("CubeEngine").get().getInstance(), PermissionService.class, proxy);
            }
            catch (ProviderExistsException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    @Disable
    public void onDisable()
    {
        cm.removeCommands(this);
        em.removeListeners(this);
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
        public SettableInvocationHandler get(DependencyInformation info, Modularity modularity)
        {
            return handler;
        }
    }
}
