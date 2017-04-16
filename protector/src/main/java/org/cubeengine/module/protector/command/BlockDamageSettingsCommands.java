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
import static org.cubeengine.module.protector.region.RegionConfig.setOrUnset;

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
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

@Command(name = "blockdamage", alias = "block", desc = "Manages the region block-damage settings")
public class BlockDamageSettingsCommands extends ContainerCommand
{
    private I18n i18n;
    private SettingsListener psl;
    private PermissionService ps;

    public BlockDamageSettingsCommands(CommandManager base, I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(base, Protector.class);
        this.i18n = i18n;
        this.psl = psl;
        this.ps = ps;
    }

    @Command(desc = "Controls entities breaking blocks")
    public void monster(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().blockDamage.monster = set;
        region.save();
        i18n.sendTranslated(context, POSITIVE,"Region {name}: BlockDamage by Entity Settings updated", region.getName());
    }

    @Command(desc = "Controls blocks breaking blocks")
    public void block(CommandSource context, BlockType by, Tristate set, @Default @Named("in") Region region)
    {
        setOrUnset(region.getSettings().blockDamage.block, by, set);
        region.save();
        i18n.sendTranslated(context, POSITIVE,"Region {name}: BlockDamage by Block Settings updated", region.getName());
    }

    @Command(desc = "Controls explosions breaking blocks")
    public void explosion(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        region.getSettings().blockDamage.allExplosion = set;
        region.save();
        i18n.sendTranslated(context, POSITIVE,"Region {name}: BlockDamage by Explosion Settings updated", region.getName());
    }

    @Command(desc = "Controls explosions caused by players breaking blocks")
    public void playerExplosion(CommandSource context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
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
                subject.getSubjectData().setPermission(ImmutableSet.of(region.getContext()), psl.explodePlayer.getId(), set);
            }
            i18n.sendTranslated(context, POSITIVE, "Bypass permissions set for the role {name}!", role);
            return;
        }
        region.getSettings().blockDamage.playerExplosion = set;
        region.save();
        i18n.sendTranslated(context, POSITIVE,"Region {name}: BlockDamage by Player-Explosion Settings updated", region.getName());
    }

    @Command(desc = "Controls fire breaking blocks")
    public void fire(CommandSource context, Tristate set, @Default @Named("in") Region region)
    {
        this.block(context, BlockTypes.FIRE, set, region);
    }
}
