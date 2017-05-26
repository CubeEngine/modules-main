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

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.netherportals.NetherportalsConfig.WorldSection;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.PortalAgent;
import org.spongepowered.api.world.World;

import javax.inject.Inject;

@ModuleInfo(name = "Netherportals", description = "Modifies Vanilla Portal behaviours")
public class Netherportals extends Module
{
    @ModuleConfig private NetherportalsConfig config;

    @Enable
    @Inject
    public void onEnable(CommandManager cm, I18n i18n)
    {
        cm.addCommand(new NetherportalsCommand(this, cm, i18n));
    }

    @Listener
    public void onPortal(MoveEntityEvent.Teleport.Portal event, @First TeleportCause cause)
    {
        WorldSection section = config.worldSettings.get(new ConfigWorld(event.getFromTransform().getExtent()));
        if (section != null && section.enablePortalRouting)
        {
            Transform<World> to = event.getToTransform();
            TeleportType type = cause.getTeleportType();
            if (type == TeleportTypes.PORTAL)
            {
                if (section.netherTarget != null)
                {
                    to = to.setExtent(section.netherTarget.getWorld());

                    event.setPortalAgent(to.getExtent().getPortalAgent());

                    // TODO netherPortalScale

                    event.setToTransform(to);
                    // TODO PortalCreation?
                }
            }
            else if (type == TeleportTypes.PORTAL)
            {
                if (section.endTarget != null)
                {
                    to = to.setExtent(section.endTarget.getWorld());
                    // TODO endPortalTargetLocation?
                    event.setPortalAgent(to.getExtent().getPortalAgent());

                    event.setToTransform(to);
                    // TODO cancel PortalCreation if not in end?
                }
            }
        }
    }

    public NetherportalsConfig getConfig()
    {
        return config;
    }
}
