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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Hostile;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.aquatic.Aquatic;
import org.spongepowered.api.entity.living.aquatic.Squid;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.monster.boss.Boss;
import org.spongepowered.api.entity.living.trader.Trader;
import org.spongepowered.api.entity.living.trader.Villager;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.permission.Subject;

public class LivingFilter extends EntityFilter
{
    private Subject subject;
    private LivingFilterParser perm;

    public LivingFilter(List<Predicate<Entity>> list, Subject subject, LivingFilterParser perm)
    {
        super(list);
        this.subject = subject;
        this.perm = perm;
    }

    @Override
    public boolean test(Entity entity)
    {
        if (entity instanceof Living)
        {
            if (entity instanceof Hostile)
            {
                if (entity instanceof Boss)
                {
                    if (!perm.PERM_BOSS.check(subject))
                    {
                        return false;
                    }
                }
                else
                {
                    if (!perm.PERM_MONSTER.check(subject))
                    {
                        return false;
                    }
                }
            }
            else if (entity instanceof Animal || entity instanceof Aquatic)
            {
                if (!perm.PERM_ANIMAL.check(subject))
                {
                    return false;
                }
            }
            else if (entity instanceof Golem)
            {
                if (!perm.PERM_GOLEM.check(subject))
                {
                    return false;
                }
            }
            else if (entity instanceof Trader)
            {
                if (!perm.PERM_NPC.check(subject))
                {
                    return false;
                }
            }
            else if (entity instanceof Ambient)
            {
                if (!perm.PERM_AMBIENT.check(subject))
                {
                    return false;
                }
            }
            else
            {
                if (!perm.PERM_ALLTYPE.check(subject))
                {
                    return false;
                }
            }

            return this.list.stream().anyMatch(p -> p.test(entity));
        }
        return false;
    }
}
