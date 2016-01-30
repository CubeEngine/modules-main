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
package org.cubeengine.module.vanillaplus.improvement.summon;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.service.user.User;
import org.spongepowered.api.data.manipulator.entity.PassengerData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;

import static org.spongepowered.api.block.BlockTypes.AIR;

/**
 * The /spawnmob command.
 */
public class SpawnMobCommand
{
    private VanillaPlus module;

    public SpawnMobCommand(VanillaPlus module)
    {
        this.module = module;
    }

    @Command(desc = "Spawns the specified Mob")
    public void spawnMob(CommandSender context, @Label("<mob>[:data][,<ridingmob>[:data]]") String data,
                         @Optional Integer amount, @Optional User player)
    {
        User sender = null;
        if (context instanceof User)
        {
            sender = (User)context;
        }
        Location loc;
        if (player != null)
        {
            loc = player.getLocation();
        }
        else if (sender == null)
        {
            i18n.sendTranslated(context, NEUTRAL, "Succesfully spawned some {text:bugs:color=RED} inside your server!");
            return;
        }
        else
        {
            loc = sender.getTargetBlock(200, AIR).add(new Vector3d(0, 1, 0));
        }
        amount = amount == null ? 1 : amount;

        if (amount <= 0)
        {
            i18n.sendTranslated(context, NEUTRAL, "And how am i supposed to know which mobs to despawn?");
            return;
        }
        if (amount > module.getConfig().spawnmobLimit)
        {
            i18n.sendTranslated(context, NEGATIVE, "The serverlimit is set to {amount}, you cannot spawn more mobs at once!", module.getConfig().spawnmobLimit);
            return;
        }
        loc = loc.add(0.5, 0, 0.5);
        Entity[] entitiesSpawned = SpawnMob.spawnMobs(context, data, loc, amount);
        if (entitiesSpawned == null)
        {
            return;
        }
        Entity entitySpawned = entitiesSpawned[0];
        if (!entitySpawned.getData(PassengerData.class).isPresent())
        {
            i18n.sendTranslated(context, POSITIVE, "Spawned {amount} {input#entity}!", amount, entitySpawned.getType().getName());
        }
        else
        {
            String message = entitySpawned.getType().getName();
            while (entitySpawned.getData(PassengerData.class).isPresent())
            {
                entitySpawned = entitySpawned.getData(PassengerData.class).get().getVehicle();
                message = context.getTranslation(NONE, "{input#entity} riding {input}", entitySpawned.getType().getName(), message).getTranslation().get(context.getLocale());
            }
            message = context.getTranslation(POSITIVE, "Spawned {amount} {input#message}!", amount, message).getTranslation().get(
                context.getLocale());
            context.sendMessage(message);
        }
    }
}