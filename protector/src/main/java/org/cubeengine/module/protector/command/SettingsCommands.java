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
package org.cubeengine.module.protector.command;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import com.google.common.collect.ImmutableSet;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.listener.PlayerSettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionReader;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

@Command(name = "settings", alias = "set", desc = "Manages the region settings")
public class SettingsCommands extends ContainerCommand
{
    private I18n i18n;
    private PermissionService ps;
    private PlayerSettingsListener psl;

    public SettingsCommands(RegionManager manager, I18n i18n, PermissionService ps, PermissionManager pm, EventManager em, CommandManager cm)
    {
        super(cm, Protector.class);
        cm.getProviderManager().register(this, new RegionReader(manager, i18n), Region.class);
        this.i18n = i18n;
        this.ps = ps;
        this.psl = new PlayerSettingsListener(manager, pm.getBasePermission(Protector.class), pm, i18n);
        em.registerListener(Protector.class, this.psl);
    }

    @Command(desc = "Controls movement")
    public void move(CommandSource context, PlayerSettingsListener.MoveType type, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            if (!ps.getGroupSubjects().hasRegistered(role))
            {
                i18n.sendTranslated(context, NEGATIVE, "This role does not exist");
                return;
            }
            Subject subject = ps.getGroupSubjects().get(role);
            //for (MoveListener.MoveType type : types)
            {
                subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.movePerms.get(type).getId(), set);
            }
            i18n.sendTranslated(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
        }
        else
        {
            //for (MoveListener.MoveType type : types)
            {
                if (set == Tristate.UNDEFINED)
                {
                    region.getSettings().move.remove(type);
                }
                else
                {
                    region.getSettings().move.put(type, set);
                }
            }

            region.save();
            i18n.sendTranslated(context, POSITIVE,"Region {name}: Move Settings updated", region.getName());
        }
    }

    @Command(desc = "Controls player building")
    public void build(CommandSource context, Tristate set,
            @Default @Named("in") Region region,
            @Named("bypass") String role) // TODO role completer/reader
    {
        if (role != null)
        {
            if (!ps.getGroupSubjects().hasRegistered(role))
            {
                i18n.sendTranslated(context, NEGATIVE, "This role does not exist");
                return;
            }
            Subject subject = ps.getGroupSubjects().get(role);
            subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.buildPerm.getId(), set);
            i18n.sendTranslated(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
        }
        else
        {
            region.getSettings().build = set;
            region.save();
            i18n.sendTranslated(context, POSITIVE,"Region {name}: Build Settings updated", region.getName());
        }
    }
}
