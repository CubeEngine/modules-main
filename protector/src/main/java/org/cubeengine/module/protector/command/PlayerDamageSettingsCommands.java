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
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

@Command(name = "playerDamage", alias = "player", desc = "Manages the region player-damage settings")
public class PlayerDamageSettingsCommands extends ContainerCommand
{
    private I18n i18n;
    private SettingsListener psl;
    private PermissionService ps;

    public PlayerDamageSettingsCommands(CommandManager base, I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(base, Protector.class);
        this.i18n = i18n;
        this.psl = psl;
        this.ps = ps;
    }

    @Command(desc = "Controls player damage")
    public void all(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            ps.getGroupSubjects().hasSubject(role).thenAccept(b -> {
                if (!b)
                {
                    i18n.send(context, NEGATIVE, "This role does not exist");
                    return;
                }
                Subject subject = ps.getGroupSubjects().loadSubject(role).join();
                subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.playerDamgeAll.getId(), set);
                i18n.send(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
            });
            return;
        }
        region.getSettings().entityDamage.all = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: All PlayerDamage Settings updated", region);
    }

    @Command(desc = "Controls player damage by living entities")
    public void living(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            ps.getGroupSubjects().hasSubject(role).thenAccept(b -> {
                if (!b)
                {
                    i18n.send(context, NEGATIVE, "This role does not exist");
                    return;
                }
                Subject subject = ps.getGroupSubjects().loadSubject(role).join();
                subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.playerDamgeLiving.getId(), set);
                i18n.send(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
            });
            return;
        }
        region.getSettings().entityDamage.byLiving = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: PlayerDamage by Living Settings updated", region);
    }

    @Command(desc = "Controls pvp damage")
    public void pvp(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            ps.getGroupSubjects().hasSubject(role).thenAccept(b -> {
                if (!b)
                {
                    i18n.send(context, NEGATIVE, "This role does not exist");
                    return;
                }
                Subject subject = ps.getGroupSubjects().loadSubject(role).join();
                subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.playerDamgePVP.getId(), set);
                i18n.send(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
            });
            return;
        }
        region.getSettings().playerDamage.pvp = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: PVP Settings updated", region);
    }
}
