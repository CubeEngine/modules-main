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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.module.protector.region.Region;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
public class RegionContextCalculator implements ContextCalculator<Subject>
{
    private RegionManager manager;

    @Inject
    public RegionContextCalculator(RegionManager manager)
    {
        this.manager = manager;
    }

    @Override
    public void accumulateContexts(Subject subject, Set<Context> set)
    {
        if (subject instanceof Locatable)
        {
            List<Region> regions = manager.getRegionsAt(((Locatable) subject).serverLocation());
            regions.stream().map(Region::getContext).forEach(set::add);
        }
        else
        {
            // TODO better way to get player
            if (subject.containingCollection() == Sponge.server().serviceProvider().permissionService().userSubjects())
            {
                final String playerId = subject.identifier();
                final UUID uuid = UUID.fromString(playerId);
                Sponge.server().player(uuid).ifPresent(player -> {
                    List<Region> regions = manager.getRegionsAt(player.serverLocation());
                    regions.stream().map(Region::getContext).forEach(set::add);
                });

            }
        }
    }

    @Override
    public boolean matches(Context context, Subject subject)
    {
        if ("region".equals(context.getKey()) && subject instanceof Locatable)
        {
            List<Region> regions = manager.getRegionsAt(((Locatable) subject).serverLocation());
            return regions.stream().map(Region::getContext)
                                   .map(Context::getValue)
                                   .anyMatch(m -> m.equals(context.getValue()));
        }
        return false;
    }
}
