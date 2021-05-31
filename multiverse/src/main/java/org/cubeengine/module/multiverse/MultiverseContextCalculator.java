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

import java.util.function.Consumer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.world.Locatable;

public class MultiverseContextCalculator implements ContextCalculator // TODO for User is should be possible to get the world too
{
    private static final String TYPE_UNIVERSE = "universe";
    private Multiverse module;

    public MultiverseContextCalculator(Multiverse module)
    {
        this.module = module;
    }

    @Override
    public void accumulateContexts(Cause source, Consumer<Context> consumer)
    {
        source.first(Locatable.class).ifPresent(locatable -> {
            consumer.accept(new Context(TYPE_UNIVERSE, module.getUniverse(locatable.serverLocation().world())));
        });
    }

//    @Override
//    public boolean matches(Context context, Subject subject)
//    {
//        if (subject instanceof Locatable && context.getKey().equals(TYPE_UNIVERSE))
//        {
//            return module.getUniverse(((Locatable)subject).serverLocation().world()).equals(context.getValue());
//        }
//        return false;
//    }
}
