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
package org.cubeengine.module.vanillaplus.improvement.summon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.world.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class SpawnMob
{
    static Entity[] spawnMobs(CommandCause context, String mobString, ServerLocation loc, int amount, EntityMatcher em,
                              I18n i18n)
    {
        String[] mobStrings = StringUtils.explode(",", mobString);
        Entity[] mobs = spawnMob(context, mobStrings[0], loc, amount, null, em, i18n); // base mobs
        Entity[] ridingMobs = mobs;
        for (int i = 1; i < mobStrings.length; ++i)
        {
            ridingMobs = spawnMob(context, mobStrings[i], loc, amount, ridingMobs, em, i18n);
        }
        return mobs;
    }

    static Entity[] spawnMob(CommandCause context, String mobString, ServerLocation loc, int amount, Entity[] ridingOn,
                             EntityMatcher em, I18n i18n)
    {
        String entityName = mobString;
        EntityType entityType;
        List<String> entityData = new ArrayList<>();
        if (entityName.isEmpty())
        {
            return null;
        }
        final Locale locale = context.getAudience() instanceof ServerPlayer ? ((ServerPlayer)context.getAudience()).getLocale() : Locale.getDefault();
        if (entityName.contains(":"))
        {
            entityData = Arrays.asList(StringUtils.explode(":", entityName.substring(entityName
                                                                                         .indexOf(":") + 1, entityName
                                                                                         .length())));
            entityName = entityName.substring(0, entityName.indexOf(":"));
            entityType = em.mob(entityName, locale);
        }
        else
        {
            entityType = em.mob(entityName, locale);
        }
        if (entityType == null)
        {
            i18n.send(context, NEGATIVE, "Unknown mob-type: {input#entityname} not found!", entityName);
            return null;
        }
        Entity[] spawnedMobs = new Entity[amount];
        Sponge.getServer().getCauseStackManager().pushCause(context).addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLUGIN);
        for (int i = 0; i < amount; ++i)
        {
            //CreatureSpawnEvent
            Entity entity = loc.getWorld().createEntity(entityType, loc.getPosition());
            loc.getWorld().spawnEntity(entity);
            spawnedMobs[i] = entity;
            if (ridingOn != null)
            {
                final List<Entity> list = ridingOn[i].get(Keys.PASSENGERS).orElse(new ArrayList<>());
                list.add(spawnedMobs[i]);
                ridingOn[i].offer(Keys.PASSENGERS, list);
            }
        }
        applyDataToMob(entityData, spawnedMobs);
        return spawnedMobs;
    }

    /**
     * Applies a list of data in Strings onto given entities
     *
     * @param datas the data to apply
     * @param entities one or multiple entities of the same type
     */
    @SuppressWarnings("unchecked")
    static void applyDataToMob(List<String> datas, Entity... entities)
    {
        if (entities.length == 0) throw new IllegalArgumentException("You need to provide at least one entity to apply the data to!");
        for (Entity entity : entities)
        {
            if (!entities[0].getType().equals(entity.getType())) throw new IllegalArgumentException("All the entities need to be of the same type");
        }
        Map<EntityDataChanger, Object> changers = new HashMap<>();
        for (String data : datas)
        {
            for (EntityDataChanger entityDataChanger : EntityDataChanger.entityDataChangers)
            {
                if (entityDataChanger.canApply(entities[0]))
                {
                    Object typeValue = entityDataChanger.changer.getTypeValue(data);
                    if (typeValue != null) // valid typeValue for given data?
                    {
                        changers.put(entityDataChanger, typeValue); // save to apply later to all entities
                        break;
                    }
                }
            }
        }
        for (Entity entity : entities)
        {
            for (Entry<EntityDataChanger, Object> entry : changers.entrySet())
            {
                entry.getKey().changer.applyEntity(entity, entry.getValue());
            }
        }
    }
}
