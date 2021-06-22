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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class UniverseCompleter implements ValueCompleter
{
    private final Multiverse multiverse;

    @Inject
    public UniverseCompleter(Multiverse multiverse)
    {
        this.multiverse = multiverse;
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        return multiverse.getConfig().universes.keySet().stream()
               .filter(universe -> StringUtils.startsWithIgnoreCase(universe, currentInput))
               .map(universe -> CommandCompletion.of(universe)).collect(Collectors.toList());
    }
}
