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

import java.util.Collection;
import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.ParameterRegistry;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.service.matcher.MaterialMatcher;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * A command to remove non-living entities
 */
@Singleton
public class RemoveCommands
{
    public static final int RADIUS_INFINITE = -1;
    private final VanillaPlus module;
    private I18n i18n;

    @Inject
    public RemoveCommands(VanillaPlus module, EntityMatcher em, MaterialMatcher mm, I18n i18n)
    {
        this.i18n = i18n;
        this.module = module;
        ParameterRegistry.register(EntityFilter.class, new EntityFilterParser(i18n, em, mm));
    }

    @Command(desc = "Removes entities in a world")
    public void removeAll(CommandCause context, @Label("entityType[:itemMaterial]") EntityFilter filters, @Default @Named("in") ServerWorld world)
    {
        this.remove(context, filters, RADIUS_INFINITE, world);
    }

    @Command(desc = "Removes entities in a radius")
    public void remove(CommandCause context, @Label("entityType[:itemMaterial]") EntityFilter filters, @Option Integer radius, @Default @Named("in") ServerWorld world)
    {
        final boolean isPlayer = context.getSubject() instanceof ServerPlayer;
        radius = radius == null ? isPlayer ? (module.getConfig().improve.commandRemoveDefaultRadius ) : - 1 : radius;
        if (radius <= 0 && radius != RADIUS_INFINITE)
        {
            i18n.send(context, NEGATIVE, "The radius has to be a whole number greater than 0!");
            return;
        }
        ServerLocation loc = isPlayer ? ((ServerPlayer)context.getSubject()).getServerLocation() : null;
        if (loc != null && !loc.getWorld().equals(world))
        {
            loc = world.getLocation(world.getProperties().spawnPosition());
        }
        int entitiesRemoved;
        List<Entity> list = world.getEntities().stream().filter(filters).collect(toList());
        entitiesRemoved = removeEntities(list, loc, radius);

        if (entitiesRemoved == 0)
        {
            i18n.send(context, NEUTRAL, "No entities to remove!");
            return;
        }
        if (filters.isAll())
        {
            if (radius == RADIUS_INFINITE)
            {
                i18n.send(context, POSITIVE, "Removed all entities in {world}! ({amount})", world, entitiesRemoved);
                return;
            }
            i18n.send(context, POSITIVE, "Removed all entities around you! ({amount})", entitiesRemoved);
            return;
        }
        if (radius == RADIUS_INFINITE)
        {
            i18n.send(context, POSITIVE, "Removed {amount} entities in {world}!", entitiesRemoved, world);
            return;
        }
        i18n.sendN(context, POSITIVE, entitiesRemoved, "Removed one entity nearby!", "Removed {amount} entities nearby!", entitiesRemoved);
    }


    private int removeEntities(Collection<Entity> list, ServerLocation loc, int radius)
    {
        if (radius != -1 && loc == null)
        {
            throw new IllegalStateException("Unknown Location with radius");
        }
        boolean all = radius == -1;
        int radiusSquared = radius * radius;
        return (int)list.stream().filter(e -> {
            if (!all)
            {
                ServerLocation eLoc = e.getServerLocation();
                int distance = (int)(eLoc.getPosition().sub(loc.getPosition())).lengthSquared();
                return radiusSquared >= distance;
            }
            return true;
        }).peek(Entity::remove).count();
    }
}
