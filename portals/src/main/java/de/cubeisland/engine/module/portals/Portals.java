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
package de.cubeisland.engine.module.portals;

import javax.inject.Inject;
import de.cubeisland.engine.butler.ProviderManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.portals.config.Destination;
import de.cubeisland.engine.module.portals.config.Destination.DestinationReader;
import de.cubeisland.engine.module.portals.config.DestinationConverter;
import de.cubeisland.engine.module.service.Selector;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.task.TaskManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.reflect.Reflector;

@ModuleInfo(name = "Portals", description = "Create and use portals")
public class Portals extends Module
{
    private PortalManager portalManager;

    public PortalManager getPortalManager()
    {
        return portalManager;
    }

    @Inject private Reflector reflector;
    @Inject private CommandManager cm;
    @Inject private WorldManager wm;
    @Inject private Selector selector;
    @Inject private EventManager em;
    @Inject private TaskManager tm;
    @Inject private Log logger;

    @Enable
    public void onEnable()
    {
        reflector.getDefaultConverterManager().registerConverter(new DestinationConverter(wm), Destination.class);
        ProviderManager rManager = cm.getProviderManager();
        rManager.register(this, new PortalReader(this), Portal.class);
        rManager.register(this, new DestinationReader(this), Destination.class);
        this.portalManager = new PortalManager(this, selector, reflector, wm, em, tm, cm, logger);
    }
}
