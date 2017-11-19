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
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

public abstract class AbstractSettingsCommand extends ContainerCommand {

    protected final I18n i18n;
    protected final SettingsListener psl;
    protected final PermissionService ps;

    public AbstractSettingsCommand(CommandManager base, Class owner, I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(base, owner);
        this.i18n = i18n;
        this.psl = psl;
        this.ps = ps;
    }

    protected void setPermission(CommandSource context, Tristate set, Region region, String role, String perm)
    {
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
