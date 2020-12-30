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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.List;
import java.util.function.Predicate;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;

public class EntityFilter implements Predicate<Entity>
{
    private List<Predicate<Entity>> list;
    private boolean all = false;

    public EntityFilter(List<Predicate<Entity>> list)
    {
        this.list = list;
    }

    public EntityFilter(List<Predicate<Entity>> list, boolean all)
    {
        this.list = list;
        this.all = all;
    }

    @Override
    public boolean test(Entity entity)
    {
        if (entity instanceof Living)
        {
            return false;
        }
        return list.stream().anyMatch(p -> p.test(entity));
    }

    public boolean isAll()
    {
        return all;
    }
}
