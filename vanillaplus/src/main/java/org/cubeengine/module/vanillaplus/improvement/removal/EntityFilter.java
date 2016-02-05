package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.List;
import java.util.function.Predicate;
import org.spongepowered.api.entity.Entity;

public class EntityFilter implements Predicate<Entity>
{
    private List<Predicate<Entity>> list;

    public EntityFilter(List<Predicate<Entity>> list)
    {
        this.list = list;
    }

    @Override
    public boolean test(Entity entity)
    {
        return list.stream().anyMatch(p -> p.test(entity));
    }
}
