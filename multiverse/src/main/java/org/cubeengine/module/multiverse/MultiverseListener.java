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
package org.cubeengine.module.multiverse;

import java.util.HashMap;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.multiverse.player.MultiverseData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;

public class MultiverseListener
{
    private Multiverse module;

    public MultiverseListener(Multiverse module)
    {
        this.module = module;
    }

    @Listener
    public void onWorldChange(DisplaceEntityEvent event)
    {
        World from = event.getFromTransform().getExtent();
        World to = event.getToTransform().getExtent();
        Entity target = event.getTargetEntity();

        if (from.equals(to) || module.getUniverse(from).equals(module.getUniverse(to)))
        {
            return;
        }

        if (target instanceof Player)
        {
            MultiverseData data = target.get(MultiverseData.class).get();
            data.from(module.getUniverse(from), from).applyFromPlayer(((Player)target)); // save playerdata
            data.from(module.getUniverse(to), to).applyToPlayer(((Player)target)); // load playerdata
            target.offer(data);
            module.getProvided(Log.class).info("{} entered the universe {}", ((Player)target).getName(), module.getUniverse(to));
        }
        else
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        MultiverseData data = player.get(MultiverseData.class).orElse(new MultiverseData(null, new HashMap<>()));
        World world = player.getWorld();
        String loginUniverse = module.getUniverse(world);
        if (data.getCurrentUniverse() == null)
        {
            data.setCurrentUniverse(loginUniverse); // set universe
        }
        else if (!data.getCurrentUniverse().equals(loginUniverse)) // Player is not in the expected universe
        {
            data.setCurrentUniverse(loginUniverse); // update universe
            data.from(loginUniverse, world).applyToPlayer(player); // load playerdata
        }
        player.offer(data);
    }


    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event)
    {
        Player player = event.getTargetEntity();
        MultiverseData data = player.get(MultiverseData.class).get();
        String universe = module.getUniverse(player.getWorld());
        data.from(universe, player.getWorld()).applyFromPlayer(player);
        player.offer(data);
    }

    @Listener
    public void onRespawn(RespawnPlayerEvent event)
    {
        World from = event.getFromTransform().getExtent();
        World to = event.getToTransform().getExtent();
        Entity target = event.getTargetEntity();

        if (from.equals(to) || module.getUniverse(from).equals(module.getUniverse(to)))
        {
            return;
        }

        MultiverseData data = target.get(MultiverseData.class).get();
        data.from(module.getUniverse(from), from).applyFromPlayer(((Player)target)); // save playerdata
        data.from(module.getUniverse(to), to).applyToPlayer(((Player)target)); // load playerdata
        target.offer(data);
    }
}
