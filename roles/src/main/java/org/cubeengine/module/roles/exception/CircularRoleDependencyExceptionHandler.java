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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.TextComponent.Builder;
import org.cubeengine.libcube.service.command.CommandExceptionHandler;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.parameter.CommandContext;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

@Singleton
public class CircularRoleDependencyExceptionHandler implements CommandExceptionHandler
{
    private I18n i18n;

    @Inject
    public CircularRoleDependencyExceptionHandler(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Override
    public boolean handleException(Throwable t, CommandContext context, Builder builder)
    {
        if (t instanceof CircularRoleDependencyException)
        {
            int depth = ((CircularRoleDependencyException) t).getDepth();
            if (depth == 0)
            {
                builder.append(i18n.translate(context.cause(), NEGATIVE, "Cannot assign role to itself"));
            }
            else
            {
                builder.append(i18n.translate(context.cause(), NEGATIVE, "Circular Dependency detected! Depth: {}", depth));
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
