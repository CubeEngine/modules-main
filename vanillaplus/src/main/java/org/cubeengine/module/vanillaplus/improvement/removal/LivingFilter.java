package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.List;
import java.util.function.Predicate;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;

public class LivingFilter extends EntityFilter
{
    public LivingFilter(List<Predicate<Entity>> list)
    {
        super(list);
    }

    @Override
    public boolean test(Entity entity)
    {
        if (entity instanceof Living)
        {
            super.test(entity);
        }
        return false;
    }
}
