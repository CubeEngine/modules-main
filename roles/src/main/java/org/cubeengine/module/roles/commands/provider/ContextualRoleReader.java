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
package org.cubeengine.module.roles.commands.provider;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.roles.sponge.RolesPermissionService;
import org.cubeengine.module.roles.sponge.subject.RoleSubject;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.world.World;

public class ContextualRoleReader implements ArgumentReader<ContextualRole>, Completer
{
    private RolesPermissionService service;
    private Game game;

    public ContextualRoleReader(RolesPermissionService service, Game game)
    {
        this.service = service;
        this.game = game;
    }

    @Override
    public ContextualRole read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.consume(1);
        String[] split = token.toLowerCase().split("\\|");
        ContextualRole role = new ContextualRole();
        if (split.length == 3)
        {
            role.contextType = split[0];
            role.contextName = split[1];
            role.roleName = split[2];
        }
        else if (split.length == 2)
        {
            if ("global".equals(split[0]))
            {
                role.contextType = split[0];
                role.contextName = "";
                role.roleName = split[1];
            }
            else if (game.getServer().getWorld(split[0]).isPresent())
            {
                role.contextType = "world";
                role.contextName = split[0];
                role.roleName = split[1];
            }
            else
            {
                role = null;
            }
        }
        else if (split.length == 1)
        {
            for (Subject subject : service.getGroupSubjects().getAllSubjects())
            {
                if (split[0].equals(((RoleSubject)subject).getName()))
                {
                    Context ctx = subject.getActiveContexts().iterator().next();
                    role.contextType = ctx.getType();
                    role.contextName = ctx.getName();
                    role.roleName = ((RoleSubject)subject).getName();
                    return role;
                }
            }
            role = null;
        }
        if (role == null || !service.getGroupSubjects().hasRegistered(role.getIdentifier())) // check role exists?l
        {
            throw new ReaderException("Could not find the role: {input#role}", token);
        }
        return role;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        ArrayList<String> result = new ArrayList<>();
        String token = invocation.consume(1).toLowerCase();
        if (!token.contains("|") && invocation.getCommandSource() instanceof Player)
        {
            World world = ((Player)invocation.getCommandSource()).getWorld();
            Context context = new Context("world", world.getName());
            for (Subject subject : service.getGroupSubjects().getAllSubjects())
            {
                if (((RoleSubject)subject).getName().startsWith(token))
                {
                    if (!subject.getActiveContexts().isEmpty() && context.equals(
                        subject.getActiveContexts().iterator().next()))
                    {
                        result.add(((RoleSubject)subject).getName());
                    }
                }
            }
        }
        for (Subject subject : service.getGroupSubjects().getAllSubjects())
        {
            if (subject.getIdentifier().startsWith("role:" + token))
            {
                result.add(subject.getIdentifier().substring(5));
            }
        }
        return result;
    }
}
