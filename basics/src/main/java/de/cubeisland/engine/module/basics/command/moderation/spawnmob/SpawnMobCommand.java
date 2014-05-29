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
package de.cubeisland.engine.module.basics.command.moderation.spawnmob;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsConfiguration;
import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.matcher.Match;

import static de.cubeisland.engine.module.basics.command.moderation.spawnmob.SpawnMob.spawnMobs;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

/**
 * The /spawnmob command.
 */
public class SpawnMobCommand
{
    private final BasicsConfiguration config;

    public SpawnMobCommand(Basics basics)
    {
        config = basics.getConfiguration();
    }

    @Command(desc = "Spawns the specified Mob")
    @IParams({@Grouped(@Indexed(label = "<mob>[:data][,<ridingmob>[:data]]")),
              @Grouped(value = @Indexed(label = "amount", type = Integer.class), req = false),
              @Grouped(value = @Indexed(label = "player", type = User.class), req = false)})
    public void spawnMob(CubeContext context)
    {
        User sender = null;
        if (context.getSender() instanceof User)
        {
            sender = (User)context.getSender();
        }
        Location loc;
        if (context.hasIndexed(2))
        {
            User user = context.getArg(2);
            if (user == null)
            {
                context.sendTranslated(NEGATIVE, "User {user} not found!", context.getArg(2));
                return;
            }
            loc = user.getLocation();
        }
        else if (sender == null)
        {
            context.sendTranslated(NEUTRAL, "Succesfully spawned some {text:bugs:color=RED} inside your server!");
            return;
        }
        else
        {
            loc = sender.getTargetBlock(null, 200).getLocation().add(new Vector(0, 1, 0));
        }
        Integer amount = 1;
        if (context.hasIndexed(1))
        {
            amount = context.getArg(1);
            if (amount <= 0)
            {
                context.sendTranslated(NEUTRAL, "And how am i supposed to know which mobs to despawn?");
                return;
            }
        }
        if (amount > config.commands.spawnmobLimit)
        {
            context.sendTranslated(NEGATIVE, "The serverlimit is set to {amount}, you cannot spawn more mobs at once!", config.commands.spawnmobLimit);
            return;
        }
        loc.add(0.5, 0, 0.5);
        Entity[] entitiesSpawned = spawnMobs(context, context.getString(0), loc, amount);
        if (entitiesSpawned == null)
        {
            return;
        }
        Entity entitySpawned = entitiesSpawned[0];
        if (entitySpawned.getPassenger() == null)
        {
            context.sendTranslated(POSITIVE, "Spawned {amount} {input#entity}!", amount, Match.entity().getNameFor(entitySpawned.getType()));
        }
        else
        {
            String message = Match.entity().getNameFor(entitySpawned.getType());
            while (entitySpawned.getPassenger() != null)
            {
                entitySpawned = entitySpawned.getPassenger();
                message = context.getSender().getTranslation(NONE, "{input#entity} riding {input}", Match.entity().getNameFor(entitySpawned.getType()), message);
            }
            message = context.getSender().getTranslation(POSITIVE, "Spawned {amount} {input#message}!", amount, message);
            context.sendMessage(message);
        }
    }


}
