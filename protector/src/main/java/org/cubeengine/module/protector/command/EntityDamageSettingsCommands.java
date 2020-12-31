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

import com.google.inject.Inject;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.protector.command.parser.TristateParser;
import org.cubeengine.module.protector.listener.SettingsListener;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionParser;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.protector.region.RegionConfig.setOrUnset;

@Using({RegionParser.class, TristateParser.class})
@Command(name = "entityDamage", alias = "entity", desc = "Manages the region entity-damage settings")
public class EntityDamageSettingsCommands extends AbstractSettingsCommand
{

    @Inject
    public EntityDamageSettingsCommands(I18n i18n, SettingsListener psl, PermissionService ps)
    {
        super(i18n, psl, ps);
    }

    @Command(desc = "Controls entity damage")
    public void all(CommandCause context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.entityDamageAll.getId());
            return;
        }
        region.getSettings().entityDamage.all = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: All EntityDamage Settings updated", region);
    }

    @Command(desc = "Controls damage by living entities")
    public void living(CommandCause context, Tristate set, @Default @Named("in") Region region, @Named("bypass") String role)
    {
        if (role != null)
        {
            this.setPermission(context, set, region, role, psl.entityDamageLiving.getId());
            return;
        }
        region.getSettings().entityDamage.byLiving = set;
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: EntityDamage by Living Settings updated", region);
    }

    @Command(desc = "Controls damage by entities")
    public void entity(CommandCause context, EntityType<?> type, Tristate set, @Default @Named("in") Region region)
    {
        setOrUnset(region.getSettings().entityDamage.byEntity, type, set);
        region.save();
        i18n.send(context, POSITIVE,"Region {region}: EntityDamage by Entity Settings updated", region);
    }
}
