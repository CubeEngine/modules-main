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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.matcher.EntityMatcher;
import org.cubeengine.service.matcher.MaterialMatcher;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static java.util.stream.Collectors.toList;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

/**
 * Commands controlling / affecting worlds. /weather /remove /butcher
 */
public class RemoveCommands
{
    public static final int RADIUS_INFINITE = -1;
    private final VanillaPlus module;
    private I18n i18n;

    public RemoveCommands(VanillaPlus module, EntityMatcher em, MaterialMatcher mm, I18n i18n, CommandManager cm)
    {
        this.i18n = i18n;
        this.module = module;
        cm.getProviderManager().register(module, new EntityFilterReader(i18n, em, mm), EntityFilter.class);
    }

    @Command(desc = "Removes entities in a world")
    public void removeAll(CommandSource context, @Label("entityType[:itemMaterial]") EntityFilter filters, @Default @Named("in") World world)
    {
        this.remove(context, filters, RADIUS_INFINITE, world);
    }

    @Command(desc = "Removes entities in a radius")
    public void remove(CommandSource context, @Label("entityType[:itemMaterial]") EntityFilter filters, @Optional Integer radius, @Default @Named("in") World world)
    {
        radius = radius == null ? module.getConfig().improve.commandRemoveDefaultRadius : radius;
        if (radius <= 0 && radius != RADIUS_INFINITE)
        {
            i18n.sendTranslated(context, NEGATIVE, "The radius has to be a whole number greater than 0!");
            return;
        }
        Location loc = context instanceof Player ? ((Player)context).getLocation() : null;
        if (loc != null && !loc.getExtent().equals(world))
        {
            loc = world.getSpawnLocation();
        }
        int entitiesRemoved;
        List<Entity> list;
        if ("*".equals(filters))
        {
            list = world.getEntities().stream().filter(e -> !(e instanceof Living)).collect(toList());
        }
        else
        {
            list = world.getEntities().stream().filter(filters).collect(toList());
        }
        entitiesRemoved = removeEntities(list, loc, radius);

        if (entitiesRemoved == 0)
        {
            i18n.sendTranslated(context, NEUTRAL, "No entities to remove!");
            return;
        }
        if ("*".equals(filters))
        {
            if (radius == RADIUS_INFINITE)
            {
                i18n.sendTranslated(context, POSITIVE, "Removed all entities in {world}! ({amount})", world, entitiesRemoved);
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "Removed all entities around you! ({amount})", entitiesRemoved);
            return;
        }
        if (radius == RADIUS_INFINITE)
        {
            i18n.sendTranslated(context, POSITIVE, "Removed {amount} entities in {world}!", entitiesRemoved, world);
            return;
        }
        i18n.sendTranslatedN(context, POSITIVE, entitiesRemoved, "Removed one entity nearby!", "Removed {amount} entities nearby!", entitiesRemoved);
    }


    private int removeEntities(Collection<Entity> list, Location loc, int radius)
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
                Location<World> eLoc = e.getLocation();
                int distance = (int)(eLoc.getPosition().sub(loc.getPosition())).lengthSquared();
                if (radiusSquared < distance)
                {
                    return false;
                }
            }
            return true;
        }).map(e -> {
            e.remove();
            return e;
        }).count();
    }
}
