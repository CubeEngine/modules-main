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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.LocationUtil;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager.StackFrame;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

public class SpawnMob
{
    static Entity createMob(Audience context, EntityType type, List<String> data, ServerLocation loc, I18n i18n)
    {
        Entity entity = loc.world().createEntity(type, loc.position());
        if (!(entity instanceof Living))
        {
            i18n.send(context, NEGATIVE, "Invalid mob-type: {input#entityname} is not living!", type.asComponent());
            return null;
        }

        applyDataToMob(data, entity);
        return entity;
    }

    /**
     * Applies a list of data in Strings onto given entities
     *
     * @param datas the data to apply
     * @param entity one or multiple entities of the same type
     */
    @SuppressWarnings("unchecked")
    static void applyDataToMob(List<String> datas, Entity entity)
    {
        Map<EntityDataChanger, Object> changers = new HashMap<>();
        for (String data : datas)
        {
            for (EntityDataChanger entityDataChanger : EntityDataChanger.entityDataChangers)
            {
                if (entityDataChanger.canApply(entity))
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
        for (Entry<EntityDataChanger, Object> entry : changers.entrySet())
        {
            entry.getKey().changer.applyEntity(entity, entry.getValue());
        }
    }

    static void spawnMob(ServerPlayer context, List<Entity> toSpawn, Integer amount, ServerLocation loc, I18n i18n, VanillaPlus module)
    {
        amount = amount == null ? 1 : amount;
        if (amount <= 0)
        {
            i18n.send(context, NEUTRAL, "And how am i supposed to know which mobs to despawn?");
            return;
        }
        if (amount > module.getConfig().improve.spawnmobLimit)
        {
            i18n.send(context, NEGATIVE, "The serverlimit is set to {amount}, you cannot spawn more mobs at once!", module.getConfig().improve.spawnmobLimit);
            return;
        }
        try (StackFrame frame = Sponge.server().causeStackManager().pushCauseFrame())
        {
            frame.pushCause(context).addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLUGIN);
            for (int i = 0; i < amount; i++)
            {
                Entity previousEntity = toSpawn.get(0).copy();
                loc.spawnEntity(previousEntity);
                for (int j = 1; j < toSpawn.size(); j++)
                {
                    final Entity entity = toSpawn.get(j).copy();
                    loc.spawnEntity(entity);
                    previousEntity.offer(Keys.PASSENGERS, Arrays.asList(entity));
                    previousEntity = entity;
                }
            }
        }
    }

    static ServerLocation getSpawnLoc(ServerPlayer sourcePlayer, ServerPlayer at, I18n i18n)
    {
        if (at != null)
        {
            return at.serverLocation();
        }

        final ServerLocation result = LocationUtil.getBlockInSight(sourcePlayer);
        if (result == null)
        {
            i18n.send(sourcePlayer, NEGATIVE, "Cannot find Targetblock");
            return null;
        }
        return Sponge.server().teleportHelper().findSafeLocation(result).orElse(result);
    }
}
