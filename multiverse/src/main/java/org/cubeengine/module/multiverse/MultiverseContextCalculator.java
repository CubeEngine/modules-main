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
package org.cubeengine.module.multiverse;

import java.util.Set;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.Locatable;

public class MultiverseContextCalculator implements ContextCalculator<Subject> // TODO for User is should be possible to get the world too
{
    private static final String TYPE = "universe";
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
            set.add(new Context(TYPE, module.getUniverse(((Locatable)subject).getWorld())));
        }
    }

    @Override
    public boolean matches(Context context, Subject subject)
    {
        if (subject instanceof Locatable && context.getType().equals(TYPE))
        {
            return module.getUniverse(((Locatable)subject).getWorld()).equals(context.getValue());
        }
        return false;
    }
}
