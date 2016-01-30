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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.matcher.EntityMatcher;
import org.cubeengine.module.core.util.matcher.MaterialMatcher;
import org.cubeengine.module.core.util.matcher.StringMatcher;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsConfiguration;
import org.spongepowered.api.world.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.spongepowered.api.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.EntityType;

import org.cubeengine.module.core.util.ChatFormat.GOLD;
import org.cubeengine.module.core.util.ChatFormat.YELLOW;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.core.util.ChatFormat.GOLD;
import static org.cubeengine.module.core.util.ChatFormat.YELLOW;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static java.util.stream.Collectors.toList;
import static org.bukkit.entity.EntityType.*;
import static org.spongepowered.api.entity.EntityTypes.*;

/**
 * Commands controlling / affecting worlds. /weather /remove /butcher
 */
public class RemovalCommands
{
    public static final int RADIUS_INFINITE = -1;
    private final BasicsConfiguration config;
    private final Basics module;
    private EntityMatcher entityMatcher;
    private MaterialMatcher materialMatcher;
    private StringMatcher stringMatcher;
    private final EntityRemovals entityRemovals;

    public RemovalCommands(Basics module, EntityMatcher entityMatcher, MaterialMatcher materialMatcher,
                           StringMatcher stringMatcher)
    {
        this.module = module;
        this.entityMatcher = entityMatcher;
        this.materialMatcher = materialMatcher;
        this.stringMatcher = stringMatcher;
        this.config = module.getConfiguration();
        this.entityRemovals = new EntityRemovals(module);
    }

    @Command(desc = "Removes entities in a world")
    public void removeAll(CommandSender context, @Label("entityType[:itemMaterial]") String entityStrings, @Default @Named("in") World world)
    {
        this.remove(context, entityStrings, RADIUS_INFINITE, world);
    }

    @Command(desc = "Removes entity in a radius")
    public void remove(CommandSender context, @Label("entityType[:itemMaterial]") String entities,
                       @Optional Integer radius, @Default @Named("in") World world)
    {
        radius = radius == null ? this.config.commands.removeDefaultRadius : radius;
        if (radius <= 0 && radius != RADIUS_INFINITE)
        {
            context.sendTranslated(NEGATIVE, "The radius has to be a whole number greater than 0!");
            return;
        }
        Location loc = context instanceof User ? ((User)context).getLocation() : null;
        if (loc != null && !loc.getExtent().equals(world))
        {
            loc = world.getSpawnLocation();
        }
        int entitiesRemoved;
        if ("*".equals(entities))
        {
            List<Entity> list = world.getEntities().stream().filter(e -> !(e instanceof Living)).collect(toList());
            entitiesRemoved = this.removeEntities(list, loc, radius, false);
        }
        else
        {
            Collection<Entity> list = world.getEntities(); // All entites remaining in that list will not get deleted!
            List<EntityType> types = new ArrayList<>();
            for (String entityString : StringUtils.explode(",", entities))
            {
                EntityType type;
                if (entityString.contains(":"))
                {
                    type = entityMatcher.any(entityString.substring(0, entityString.indexOf(":")));
                }
                else
                {
                    type = entityMatcher.any(entityString);
                }
                if (type == null)
                {
                    context.sendTranslated(NEGATIVE, "Invalid entity-type!");
                    context.sendTranslated(NEUTRAL, "Use one of those instead:");
                    context.sendMessage(DROPPED_ITEM.toString() + YELLOW + ", " +
                                            GOLD + ARROW.getName() + YELLOW + ", " +
                                            GOLD + BOAT.getName() + YELLOW + ", " +
                                            GOLD + RIDEABLE_MINECART.getName() + YELLOW + ", " +
                                            GOLD + PAINTING.getName() + YELLOW + ", " +
                                            GOLD + ITEM_FRAME.getName() + YELLOW + " or " +
                                            GOLD + EXPERIENCE_ORB.getName());
                    return;
                }
                if (Living.class.isAssignableFrom(type.getEntityClass()))
                {
                    context.sendTranslated(NEGATIVE, "To kill living entities use the {text:/butcher} command!");
                    return;
                }
                if (entityString.contains(":"))
                {
                    if (!DROPPED_ITEM.equals(type))
                    {
                        context.sendTranslated(NEGATIVE, "You can only specify data for removing items!");
                        return;
                    }
                    ItemType itemtype = materialMatcher.material(entityString.substring(entityString.indexOf(":") + 1));
                    List<Entity> removeList = new ArrayList<>();
                    for (Entity entity : list)
                    {
                        if (entity.getType().equals(DROPPED_ITEM) && ((Item)entity).getItemData().getValue().getItem().equals(itemtype))
                        {
                            removeList.add(entity);
                        }
                    }
                    list.removeAll(removeList);
                }
                else
                {
                    types.add(type);
                }
            }
            Collection<Entity> remList = new ArrayList<>();
            for (Entity e : list)
            {
                if (types.contains(e.getType()))
                {
                    remList.add(e);
                }
            }
            list.removeAll(remList);
            remList = world.getEntities();
            remList.removeAll(list);
            entitiesRemoved = this.removeEntities(remList, loc, radius, false);
        }
        if (entitiesRemoved == 0)
        {
            context.sendTranslated(NEUTRAL, "No entities to remove!");
            return;
        }
        if ("*".equals(entities))
        {
            if (radius == RADIUS_INFINITE)
            {
                context.sendTranslated(POSITIVE, "Removed all entities in {world}! ({amount})", world, entitiesRemoved);
                return;
            }
            context.sendTranslated(POSITIVE, "Removed all entities around you! ({amount})", entitiesRemoved);
            return;
        }
        if (radius == RADIUS_INFINITE)
        {
            context.sendTranslated(POSITIVE, "Removed {amount} entities in {world}!", entitiesRemoved, world);
            return;
        }
        context.sendTranslatedN(POSITIVE, entitiesRemoved, "Removed one entity nearby!",
                                "Removed {amount} entities nearby!", entitiesRemoved);
    }

    @Command(desc = "Gets rid of mobs close to you. Valid types are:\n" +
        "monster, animal, pet, golem, boss, other, creeper, skeleton, spider etc.")
    public void butcher(CommandSender context, @Label("types...") @Optional String types,
                        @Optional Integer radius,
                        @Named("in") World world,
                        @Flag boolean lightning, // die with style
                        @Flag boolean all) // infinite radius
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        Location loc;
        radius = radius == null ? this.config.commands.butcherDefaultRadius : radius;
        if (radius < 0 && !(radius == -1 && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context)))
        {
            context.sendTranslated(NEGATIVE, "The radius has to be a number greater than 0!");
            return;
        }
        int removed;
        if (sender == null)
        {
            radius = -1;
            loc = this.config.mainWorld.getWorld().getSpawnLocation();
        }
        else
        {
            loc = sender.getLocation();
        }
        if (all && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context))
        {
            radius = -1;
        }
        lightning = lightning && module.perms().COMMAND_BUTCHER_FLAG_LIGHTNING.isAuthorized(context);
        Collection<Entity> list;
        if (context instanceof User && !(radius == -1))
        {
            list = ((User)context).getNearbyEntities(radius);
        }
        else
        {
            list = loc.getExtent().getEntities();
        }
        String[] s_types = { "monster" };
        boolean allTypes = false;
        if (types != null)
        {
            if ("*".equals(types))
            {
                allTypes = true;
                if (!module.perms().COMMAND_BUTCHER_FLAG_ALLTYPE.isAuthorized(context))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to butcher all types of living entities at once!");
                    return;
                }
            }
            else
            {
                s_types = StringUtils.explode(",", types);
            }
        }
        List<Entity> remList = new ArrayList<>();
        if (!allTypes)
        {
            for (String s_type : s_types)
            {
                String match = stringMatcher.matchString(s_type, this.entityRemovals.GROUPED_ENTITY_REMOVAL.keySet());
                EntityType directEntityMatch = null;
                if (match == null)
                {
                    directEntityMatch = entityMatcher.mob(s_type);
                    if (directEntityMatch == null)
                    {
                        context.sendTranslated(NEGATIVE, "Unknown entity {input#entity}", s_type);
                        return;
                    }
                    if (this.entityRemovals.DIRECT_ENTITY_REMOVAL.get(directEntityMatch) == null) throw new IllegalStateException("Missing Entity? " + directEntityMatch);
                }
                EntityRemoval entityRemoval;
                if (directEntityMatch != null)
                {
                    entityRemoval = this.entityRemovals.DIRECT_ENTITY_REMOVAL.get(directEntityMatch);
                }
                else
                {
                    entityRemoval = this.entityRemovals.GROUPED_ENTITY_REMOVAL.get(match);
                }
                remList.addAll(list.stream().filter(entity -> entityRemoval.doesMatch(entity)
                    && entityRemoval.isAllowed(context)).collect(toList()));
            }
        }
        else
        {
            remList.addAll(list);
        }
        list = new ArrayList<>();
        for (Entity entity : remList)
        {
            if (entity instanceof Living)
            {
                list.add(entity);
            }
        }
        removed = this.removeEntities(list, loc, radius, lightning);
        if (removed == 0)
        {
            context.sendTranslated(NEUTRAL, "Nothing to butcher!");
        }
        else
        {
            context.sendTranslated(POSITIVE, "You just slaughtered {amount} living entities!", removed);
        }

    }

    private int removeEntities(Collection<Entity> remList, Location loc, int radius, boolean lightning)
    {
        int removed = 0;

        if (radius != -1 && loc == null)
        {
            throw new IllegalStateException("Unknown Location with radius");
        }
        boolean all = radius == -1;
        int radiusSquared = radius * radius;
        for (Entity entity : remList)
        {
            if (entity instanceof Player)
            {
                continue;
            }
            Location eLoc = entity.getLocation();
            if (!all)
            {

                int distance = (int)(eLoc.getPosition().sub(loc.getPosition())).lengthSquared();
                if (radiusSquared < distance)
                {
                    continue;
                }
            }
            if (lightning)
            {
                ((World)eLoc.getExtent()).strikeLightningEffect(eLoc);
            }
            entity.remove();
            removed++;
        }
        return removed;
    }
}
