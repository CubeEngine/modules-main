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

import java.util.Collections;
import java.util.Set;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

public class ContextMirrorCalculator implements ContextCalculator<Subject>
{
    private ContextmirrorConfig config;

    public ContextMirrorCalculator(ContextmirrorConfig config)
    {
        this.config = config;
    }

    @Override
    public void accumulateContexts(Subject calculable, Set<Context> accumulator)
    {
        config.contextMirrors.entrySet().stream()
             .filter(entry -> !Collections.disjoint(entry.getValue(), accumulator))
             .forEach(entry -> accumulator.add(entry.getKey()));
    }

    @Override
    public boolean matches(Context context, Subject subject)
    {
        return config.contextMirrors.keySet().contains(context);
    }
}
