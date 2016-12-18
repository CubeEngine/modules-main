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
package org.cubeengine.module.conomy;

import javax.inject.Inject;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import org.spongepowered.api.service.economy.EconomyService;

@ServiceProvider(EconomyService.class)
public class EconomyServiceProvider implements Provider<EconomyService>
{
    @Inject private Conomy module;

    @Override
    public EconomyService get()
    {
        // Make sure Conomy is enabled
        module.getModularity().getLifecycle(module.getInformation().getIdentifier()).enable();
        return module.getService();
    }
}
