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
package de.cubeisland.engine.log.action.newaction.entity;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.log.action.newaction.LogListener;
import de.cubeisland.engine.log.action.newaction.entity.spawn.MonsterEggUse;
import de.cubeisland.engine.log.action.newaction.entity.spawn.NaturalSpawn;
import de.cubeisland.engine.log.action.newaction.entity.spawn.OtherSpawn;
import de.cubeisland.engine.log.action.newaction.entity.spawn.SpawnerSpawn;

import static org.bukkit.Material.MONSTER_EGG;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * A Listener for Entity Actions
 * <p>Events:
 * {@link CreatureSpawnEvent}
 * {@link PlayerInteractEvent}
 * <p>Actions:
 * {@link NaturalSpawn}
 * {@link SpawnerSpawn}
 * {@link OtherSpawn}
 * {@link MonsterEggUse}
 */
public class EntityListener extends LogListener
{
    public EntityListener(Module module)
    {
        super(module);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event)
    {
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();
        switch (event.getSpawnReason())
        {
        case NATURAL:
        case JOCKEY:
        case CHUNK_GEN:
        case VILLAGE_DEFENSE:
        case VILLAGE_INVASION:
            NaturalSpawn naturalSpawn = this.newAction(NaturalSpawn.class, world);
            if (naturalSpawn != null)
            {
                naturalSpawn.setLocation(entity.getLocation());
                naturalSpawn.setEntity(entity);
                this.logAction(naturalSpawn);
            }
            return;
        case SPAWNER:
            SpawnerSpawn spawnerSpawn = this.newAction(SpawnerSpawn.class, world);
            if (spawnerSpawn != null)
            {
                spawnerSpawn.setLocation(entity.getLocation());
                spawnerSpawn.setEntity(entity);
                this.logAction(spawnerSpawn);
            }
            return;
        case EGG:
        case BUILD_SNOWMAN:
        case BUILD_IRONGOLEM:
        case BUILD_WITHER:
        case BREEDING:
            OtherSpawn otherSpawn = this.newAction(OtherSpawn.class, world);
            if (otherSpawn != null)
            {
                otherSpawn.setLocation(entity.getLocation());
                otherSpawn.setEntity(entity);
                this.logAction(otherSpawn);
            }
            return;
        case SPAWNER_EGG:
            // TODO preplanned
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterEggUse(PlayerInteractEvent event)
    {
        if (event.getAction() == RIGHT_CLICK_BLOCK && event.getPlayer().getItemInHand().getType() == MONSTER_EGG)
        {
            if (this.isActive(MonsterEggUse.class, event.getPlayer().getWorld()))
            {

            }
        }
    }
    /*
    if (itemInHand.getType() == MONSTER_EGG)
        {
            MonsterEggUse monsterEggUse = this.manager.getActionType(MonsterEggUse.class);
            if (monsterEggUse.isActive(state.getWorld()))
            {
                monsterEggUse.logSimple(event.getClickedBlock().getRelative(event.getBlockFace()).getLocation(),
                                        event.getPlayer(),new ItemData(itemInHand).serialize(this.om));
            }
        }
     */
}
