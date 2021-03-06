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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

import com.google.inject.Inject;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerWorld;

@Command(name = "netherportals", alias = "np", desc = "Manages Netherportals")
public class NetherportalsCommand extends DispatcherCommand
{

    private Netherportals module;
    private I18n i18n;

    @Inject
    public NetherportalsCommand(Netherportals module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(alias = "nether", desc = "Sets the NetherPortal Target")
    public void setNetherTarget(CommandCause context, ServerWorld world, ServerWorld target)
    {
        ConfigWorld source = new ConfigWorld(world);
        NetherportalsConfig.WorldSection section =
                module.getConfig().worldSettings.computeIfAbsent(source, k -> new NetherportalsConfig.WorldSection());
        section.netherTarget = new ConfigWorld(target);
        section.enablePortalRouting = true;
        module.getConfig().save();
        i18n.send(context, POSITIVE, "NetherPortals in {world} now lead to {world}", world, target);
    }

    @Command(alias = "end", desc = "Sets the EndPortal Target")
    public void setEndTarget(CommandCause context, ServerWorld world, ServerWorld target)
    {
        ConfigWorld source = new ConfigWorld(world);
        NetherportalsConfig.WorldSection section =
                module.getConfig().worldSettings.computeIfAbsent(source, k -> new NetherportalsConfig.WorldSection());
        section.endTarget = new ConfigWorld(target);
        section.enablePortalRouting = true;
        module.getConfig().save();
        i18n.send(context, POSITIVE, "EndPortals in {world} now lead to {world}", world, target);
    }

    // TODO disable portal routing
}
