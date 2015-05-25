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
package de.cubeisland.engine.module.basics.command.moderation;

import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.module.basics.Basics;

import org.spongepowered.api.data.manipulator.entity.TameableData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Bat;
import org.spongepowered.api.entity.living.Squid;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.animal.*;
import org.spongepowered.api.entity.living.complex.EnderDragon;
import org.spongepowered.api.entity.living.golem.Golem;
import org.spongepowered.api.entity.living.golem.IronGolem;
import org.spongepowered.api.entity.living.golem.SnowGolem;
import org.spongepowered.api.entity.living.monster.*;

public class EntityRemovals
{
    public EntityRemovals(Basics module)
    {
        GROUPED_ENTITY_REMOVAL.put("pet", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_PET, Tameable.class)
        {
            @Override
            public boolean extra(Entity entity)
            {
                return entity.getData(TameableData.class).isPresent();
            }
        });
        GROUPED_ENTITY_REMOVAL.put("golem", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_GOLEM, Golem.class));
        GROUPED_ENTITY_REMOVAL.put("animal", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Animal.class)
        {
            @Override
            public boolean extra(Entity entity)
            {
                return entity.getData(TameableData.class).isPresent();
            }
        });
        GROUPED_ENTITY_REMOVAL.put("other", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_OTHER, Ambient.class, Squid.class));
        GROUPED_ENTITY_REMOVAL.put("boss", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_BOSS, EnderDragon.class, Wither.class));
        GROUPED_ENTITY_REMOVAL.put("monster", new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Monster.class, Slime.class, Ghast.class));


        DIRECT_ENTITY_REMOVAL.put(EntityTypes.CREEPER, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Creeper.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SPIDER, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Spider.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.GIANT, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_BOSS, Giant.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.ZOMBIE, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Zombie.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SLIME, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Slime.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.GHAST, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Ghast.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.PIG_ZOMBIE, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, ZombiePigman.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.ENDERMAN, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Enderman.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.CAVE_SPIDER, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, CaveSpider.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SILVERFISH, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Silverfish.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.BLAZE, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Blaze.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.MAGMA_CUBE, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, MagmaCube.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.ENDER_DRAGON, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_BOSS, EnderDragon.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.WITHER, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_BOSS, Wither.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.BAT, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_OTHER, Bat.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.WITCH, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Witch.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.PIG, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Pig.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SHEEP, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Sheep.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.COW, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Cow.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.CHICKEN, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Chicken.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SKELETON, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_MONSTER, Skeleton.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SQUID, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_OTHER, Squid.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.WOLF, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Wolf.class)
        {
            @Override
            public boolean extra(Entity entity)
            {
                return !entity.getData(TameableData.class).isPresent();
            }
        });
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.MUSHROOM_COW, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Mooshroom.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.SNOWMAN, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_GOLEM, SnowGolem.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.OCELOT, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Ocelot.class)
        {
            @Override
            public boolean extra(Entity entity)
            {
                return !entity.getData(TameableData.class).isPresent();
            }
        });
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.IRON_GOLEM, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_GOLEM, IronGolem.class));
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.HORSE, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_ANIMAL, Horse.class)
        {
            @Override
            public boolean extra(Entity entity)
            {
                return !entity.getData(TameableData.class).isPresent();
            }
        });
        DIRECT_ENTITY_REMOVAL.put(EntityTypes.VILLAGER, new EntityRemoval(module.perms().COMMAND_BUTCHER_FLAG_NPC, Villager.class));
    }

    final Map<String, EntityRemoval> GROUPED_ENTITY_REMOVAL = new HashMap<>();
    final Map<EntityType, EntityRemoval> DIRECT_ENTITY_REMOVAL = new HashMap<>();
}
