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
package org.cubeengine.module.roles.exception;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import org.cubeengine.butler.CommandBase;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.exception.PriorityExceptionHandler;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class RolesExceptionHandler implements PriorityExceptionHandler
{
    private I18n i18n;

    public RolesExceptionHandler(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public boolean handleException(Throwable t, CommandBase command, CommandInvocation invocation)
    {
        if (t instanceof InvocationTargetException || t instanceof ExecutionException)
        {
            t = t.getCause();
        }

        CommandSource sender = (CommandSource) invocation.getCommandSource();


        if (t instanceof CircularRoleDependencyException)
        {
            int depth = ((CircularRoleDependencyException) t).getDepth();
            if (depth == 0)
            {
                i18n.send(sender, NEGATIVE, "Cannot assign role to itself");
            }
            else
            {
                i18n.send(sender, NEGATIVE, "Circular Dependency detected! Depth: {}", depth);
            }
            return true;
        }
        return false;
    }

    @Override
    public int priority()
    {
        return 1;
    }
}
