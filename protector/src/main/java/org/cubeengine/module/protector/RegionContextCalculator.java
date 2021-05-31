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
package org.cubeengine.module.protector;

import java.util.List;
import java.util.function.Consumer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.world.Locatable;

@Singleton
public class RegionContextCalculator implements ContextCalculator
{
    private RegionManager manager;

    @Inject
    public RegionContextCalculator(RegionManager manager)
    {
        this.manager = manager;
    }

    @Override
    public void accumulateContexts(Cause source, Consumer<Context> accumulator)
    {
        source.first(Locatable.class).ifPresent(locatable -> {
            List<Region> regions = manager.getRegionsAt(locatable.serverLocation());
            regions.stream().map(Region::getContext).forEach(accumulator::accept);
        });
    }

//    @Override
//    public boolean matches(Context context, Subject subject)
//    {
//        if ("region".equals(context.getKey()) && subject instanceof Locatable)
//        {
//            List<Region> regions = manager.getRegionsAt(((Locatable) subject).serverLocation());
//            return regions.stream().map(Region::getContext)
//                                   .map(Context::getValue)
//                                   .anyMatch(m -> m.equals(context.getValue()));
//        }
//        return false;
//    }
}
