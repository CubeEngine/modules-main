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
package org.cubeengine.module.netherportals;

import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.netherportals.NetherportalsConfig.WorldSection;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.MovementType;
import org.spongepowered.api.event.cause.entity.MovementTypes;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.portal.PortalType;
import org.spongepowered.api.world.portal.PortalTypes;

@Singleton
@Module
public class Netherportals
{
    @ModuleConfig private NetherportalsConfig config;
    @ModuleCommand private NetherportalsCommand netherportalsCommand;

    @Listener
    public void onPortal(ChangeEntityWorldEvent.Pre event, @First PortalType portalType)
    {
        final MovementType type = event.getContext().get(EventContextKeys.MOVEMENT_TYPE).orElse(null);
        if (type != MovementTypes.PORTAL.get())
        {
            return;
        }
        WorldSection section = config.worldSettings.get(new ConfigWorld(event.getOriginalWorld()));
        if (section == null || !section.enablePortalRouting)
        {
            return;
        }
        if (portalType == PortalTypes.END.get())
        {
            if (section.endTarget != null)
            {
                event.setDestinationWorld(section.endTarget.getWorld());
                // TODO endPortalTargetLocation?
                // TODO cancel PortalCreation if not in end?
            }
        }
        else if (portalType == PortalTypes.NETHER.get())
        {
            if (section.netherTarget != null)
            {
                event.setDestinationWorld(section.netherTarget.getWorld());
                // TODO netherPortalScale/location
                // TODO PortalCreation?
            }
        }
    }

    public NetherportalsConfig getConfig()
    {
        return config;
    }
}
