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
package org.cubeengine.module.protector.command;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import com.google.common.collect.ImmutableSet;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

public abstract class AbstractSettingsCommand extends DispatcherCommand
{

    protected final I18n i18n;
    protected final SettingsListener psl;

    public AbstractSettingsCommand(I18n i18n, SettingsListener psl, Object... subCommands)
    {
        super(subCommands);
        this.i18n = i18n;
        this.psl = psl;
    }

    protected void setPermission(CommandCause context, Tristate set, Region region, String role, String perm)
    {
        PermissionService ps = Sponge.getServer().getServiceProvider().permissionService();
        ps.getGroupSubjects().hasSubject(role).thenAccept(b -> {
            if (!b)
            {
                i18n.send(context, NEGATIVE, "This role does not exist");
                return;
            }
            Subject subject = ps.getGroupSubjects().loadSubject(role).join();
            subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), perm, set);
            i18n.send(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
        });
    }
}
