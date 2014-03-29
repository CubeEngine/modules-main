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
package de.cubeisland.engine.log.action.newaction.block.entity.explosion;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import de.cubeisland.engine.core.bukkit.BukkitUtils;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.log.Log;
import de.cubeisland.engine.log.action.newaction.LogListener;

import static org.bukkit.Material.AIR;

/**
 * A Listener for EntityBlock Actions
 * <p>Events:
 * {@link EntityExplodeEvent}
 * <p>Actions:
 * {@link CreeperExplode}
 * {@link TntExplode}
 * {@link WitherExplode}
 * {@link FireballExplode}
 * {@link EnderdragonExplode}
 * {@link EntityExplode}
 */
public class ExplodeListener extends LogListener
{
    public ExplodeListener(Log module)
    {
        super(module);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        final List<Block> blocks = event.blockList();
        if (blocks.isEmpty())
        {
            return;
        }

        final Entity entity = event.getEntity();
        final World world;
        if (entity != null)
        {
            world = entity.getWorld();
        }
        else
        {
            world = blocks.get(0).getWorld();
        }

        Player player = null;
        Class<? extends ExplosionActionType> actionClazz;
        if (entity instanceof Creeper)
        {
            actionClazz = CreeperExplode.class;
            if (((Creeper)entity).getTarget() instanceof Player)
            {
                player = (Player)((Creeper)entity).getTarget();
            }
        }
        else if (entity instanceof TNTPrimed)
        {
            actionClazz = TntExplode.class;
            if (((TNTPrimed)entity).getSource() instanceof Player)
            {
                player = (Player)((TNTPrimed)entity).getSource();
            }
        }
        else if (entity instanceof WitherSkull)
        {
            actionClazz = WitherExplode.class;
            if (((WitherSkull)entity).getShooter() instanceof Wither && ((Wither)((WitherSkull)entity).getShooter())
                .getTarget() instanceof Player)
            {
                player = (Player)((Wither)((WitherSkull)entity).getShooter()).getTarget();
            }
        }
        else if (entity instanceof Fireball)
        {
            // TODO other shooter than ghast
            actionClazz = FireballExplode.class;
            if (((Fireball)entity).getShooter() instanceof Ghast)
            {
                LivingEntity target = BukkitUtils.getTarget((Ghast)((Fireball)entity).getShooter());
                if (target != null && target instanceof Player)
                {
                    player = (Player)target;
                }
            }
        }
        else if (entity instanceof EnderDragon)
        {
            actionClazz = EnderdragonExplode.class;
            LivingEntity target = BukkitUtils.getTarget((LivingEntity)entity);
            if (target != null && target instanceof Player)
            {
                player = (Player)target;
            }
        }
        else
        {
            if (this.isActive(EntityExplode.class, world))
            {
                for (Block block : blocks)
                {
                    EntityExplode eAction = this.newAction(EntityExplode.class);
                    eAction.setLocation(block.getLocation());
                    eAction.setEntity(entity);
                    this.logAction(eAction);

                    // TODO attached / falling / ignore blocks exploded
                    actionType.logAttachedBlocks(block.getState());
                    actionType.logFallingBlocks(block.getState());
                }
            }
            return;
        }

        if (this.isActive(actionClazz, world))
        {
            for (Block block : blocks)
            {
                if ((block.getType().equals(Material.WOODEN_DOOR) || block.getType()
                                                                          .equals(Material.IRON_DOOR_BLOCK)) && block
                    .getData() >= 8)
                {
                    continue; // ignore upper door halves
                }
                this.setInfoAndLog(this.newAction(actionClazz), entity, block, player);
                // TODO attached / falling / ignore blocks exploded
                actionType.logAttachedBlocks(block.getState(), player);
                actionType.logFallingBlocks(block.getState(), player);
            }
        }
    }

    private void setInfoAndLog(ExplosionActionType action, Entity entity, Block block, Player player)
    {
        action.setLocation(block.getLocation());
        action.setOldBlock(block.getState());
        action.setNewBlock(AIR);
        action.setEntity(entity);
        action.setPlayer(player);
        this.logAction(action);
    }
}
