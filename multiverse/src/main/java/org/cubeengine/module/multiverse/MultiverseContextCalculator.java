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
package org.cubeengine.module.multiverse;

import java.util.Set;
import java.util.UUID;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;

public class MultiverseContextCalculator implements ContextCalculator<Subject> // TODO for User is should be possible to get the world too
{
    private static final String TYPE_UNIVERSE = "universe";
    private Multiverse module;

    public MultiverseContextCalculator(Multiverse module)
    {
        this.module = module;
    }

    @Override
    public void accumulateContexts(Subject subject, Set<Context> set)
    {
        if (subject instanceof Locatable)
        {
            set.add(new Context(TYPE_UNIVERSE, module.getUniverse(((Locatable)subject).serverLocation().world())));
        }
        else
        {
            // TODO better way to get player
            if (subject.containingCollection() == Sponge.server().serviceProvider().permissionService().userSubjects())
            {
                final String playerId = subject.identifier();
                final UUID uuid = UUID.fromString(playerId);
                Sponge.server().player(uuid).ifPresent(player -> set.add(new Context(TYPE_UNIVERSE, module.getUniverse(player.world()))));
            }
        }
    }

    @Override
    public boolean matches(Context context, Subject subject)
    {
        if (subject instanceof Locatable && context.getKey().equals(TYPE_UNIVERSE))
        {
            return module.getUniverse(((Locatable)subject).serverLocation().world()).equals(context.getValue());
        }
        return false;
    }
}
