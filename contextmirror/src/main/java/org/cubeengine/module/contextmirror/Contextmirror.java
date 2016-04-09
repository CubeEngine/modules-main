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
package org.cubeengine.module.contextmirror;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;

@ModuleInfo(name = "ContextMirror", description = "Provides grouping for contexts")
public class Contextmirror extends Module
{
    @ModuleConfig private ContextmirrorConfig config;

    @Inject private PermissionService service;

    @Inject
    public Contextmirror(Reflector reflector)
    {
        reflector.getDefaultConverterManager().registerConverter(new ContextConverter(), Context.class);
    }

    @Enable
    public void onEnable()
    {
        service.registerContextCalculator(new ContextMirrorCalculator(config));
    }
}
