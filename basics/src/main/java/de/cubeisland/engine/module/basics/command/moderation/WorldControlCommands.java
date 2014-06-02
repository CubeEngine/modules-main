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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.exception.IncorrectUsageException;
import de.cubeisland.engine.core.command.exception.MissingParameterException;
import de.cubeisland.engine.core.command.parameterized.completer.WorldCompleter;
import de.cubeisland.engine.core.command.readers.IntegerOrAllReader;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Flag;
import de.cubeisland.engine.core.command.reflected.context.Flags;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.command.reflected.context.NParams;
import de.cubeisland.engine.core.command.reflected.context.Named;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.basics.BasicsConfiguration;

import static de.cubeisland.engine.core.util.ChatFormat.GOLD;
import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static org.bukkit.entity.EntityType.DROPPED_ITEM;

/**
 * Commands controlling / affecting worlds. /weather /remove /butcher
 */
public class WorldControlCommands
{
    private final BasicsConfiguration config;
    private final Basics module;
    private final EntityRemovals entityRemovals;

    public WorldControlCommands(Basics module)
    {
        this.module = module;
        this.config = module.getConfiguration();
        this.entityRemovals = new EntityRemovals(module);
    }

    @Command(desc = "Changes the weather")
    @IParams({@Grouped(@Indexed(label = {"!sun","!rain","!storm"})),
              @Grouped(req = false, value = @Indexed(label = "duration"))})
    @NParams(@Named(names = "in", label = "world", type = World.class))
    public void weather(CubeContext context)
    {
        World world;
        if (context.hasNamed("in"))
        {
            world = context.getArg("in", null);
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input#world} not found!", context.getArg(1));
                return;
            }
        }
        else if (context.isSender(User.class))
        {
            world = ((User)context.getSender()).getWorld();
        }
        else
        {
            throw new MissingParameterException("in", context.getSender().getTranslation(NEGATIVE, "If not used ingame you have to specify a world!"));
        }
        boolean sunny = true;
        boolean noThunder = true;
        int duration = 10000000;
        String weather = Match.string().matchString(context.getString(0), "sun", "rain", "storm");
        if (weather == null)
        {
            context.sendTranslated(NEGATIVE, "Invalid weather! {input}", context.getArg(0));
            context.sendTranslated(NEUTRAL, "Use {name#sun}, {name#rain} or {name#storm}!");
            return;
        }
        switch (weather)
        {
            case "sun":
                sunny = true;
                noThunder = true;
                break;
            case "rain":
                sunny = false;
                noThunder = true;
                break;
            case "storm":
                sunny = false;
                noThunder = false;
                break;
        }
        if (context.hasIndexed(1))
        {
            duration = context.getArg(1, 0);
            if (duration == 0)
            {
                context.sendTranslated(NEGATIVE, "The given duration is invalid!");
                return;
            }
            duration *= 20;
        }

        if (world.isThundering() != noThunder && world.hasStorm() != sunny) // weather is not changing
        {
            context.sendTranslated(POSITIVE, "Weather in {world} is already set to {input#weather}!", world, weather);
        }
        else
        {
            context.sendTranslated(POSITIVE, "Changed weather in {world} to {input#weather}!", world, weather);
        }
        world.setStorm(!sunny);
        world.setThundering(!noThunder);
        world.setWeatherDuration(duration);
    }

    @Command(alias = "playerweather", desc = "Changes your weather")
    @IParams(@Grouped(@Indexed(label = {"!clear","!downfall","!reset"})))
    @NParams(@Named(names = "player", label = "player", type = User.class))
    public void pweather(CubeContext context)
    {
        User user;
        if (context.hasNamed("player"))
        {
            user = context.getArg("player");
        }
        else if (context.isSender(User.class))
        {
            user = (User)context.getSender();
        }
        else
        {
            throw new MissingParameterException("player", context.getSender().getTranslation(NEGATIVE, "If not used ingame you have to specify a player!"));
        }
        if (!user.isOnline())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", user);
            return;
        }
        String weather = Match.string().matchString(context.getString(0), "clear", "downfall", "reset");
        if (weather == null)
        {
            context.sendTranslated(NEGATIVE, "Invalid weather! {input}", context.getArg(0));
            context.sendTranslated(NEUTRAL, "Use {name#clear}, {name#downfall} or {name#reset}!");
            return;
        }
        switch (weather)
        {
            case "clear":
                user.setPlayerWeather(WeatherType.CLEAR);
                if (user == context.getSender())
                {
                    context.sendTranslated(POSITIVE, "Your weather is now clear!");
                }
                else
                {
                    user.sendTranslated(POSITIVE, "Your weather is now clear!");
                    context.sendTranslated(POSITIVE, "{user}s weather is now clear!", user);
                }
                return;
            case "downfall":
                user.setPlayerWeather(WeatherType.DOWNFALL);
                if (user == context.getSender())
                {
                    context.sendTranslated(POSITIVE, "Your weather is now not clear!");
                }
                else
                {
                    user.sendTranslated(POSITIVE, "Your weather is now not clear!");
                    context.sendTranslated(POSITIVE, "{user}s weather is now not clear!", user);
                }
                return;
            case "reset":
                user.resetPlayerWeather();
                if (user == context.getSender())
                {
                    context.sendTranslated(POSITIVE, "Your weather is now reset to server weather!");
                }
                else
                {
                    user.sendTranslated(POSITIVE, "Your weather is now reset to server weather!");
                    context.sendTranslated(POSITIVE, "{user}s weather is now reset to server weather!", user);
                }
                return;
        }
        throw new IncorrectUsageException("You did something wrong!");
    }

    @Command(desc = "Removes entity")
    @IParams({@Grouped(@Indexed(label = "entityType[:itemMaterial]")),
              @Grouped(req = false, value = @Indexed(label = {"radius","!*"}, type = IntegerOrAllReader.class))})
    @NParams(@Named(names = "in", label = "world", type = World.class))
    public void remove(CubeContext context)
    {
        User sender = null;
        if (context.getSender() instanceof User)
        {
            sender = (User)context.getSender();
        }
        World world;
        if (context.hasNamed("in"))
        {
            world = context.getArg("in");
        }
        else
        {
            if (sender == null)
            {
                context.sendTranslated(NEGATIVE, "The butcher will come for YOU tonight!");
                return;
            }
            world = sender.getWorld();
        }
        int radius = this.config.commands.removeDefaultRadius;
        if (context.hasIndexed(1))
        {
            if ("*".equals(context.getArg(1)))
            {
                radius = -1;
            }
            else if (sender == null)
            {
                context.sendTranslated(NEGATIVE, "If not used ingame you can only remove all!");
                return;
            }
            else
            {
                radius = context.getArg(1, 0);
            }
            if (radius <= 0)
            {
                context.sendTranslated(NEGATIVE, "The radius has to be a whole number greater than 0!");
                return;
            }
        }
        Location loc = null;
        if (sender != null)
        {
            loc = sender.getLocation();
        }
        int entitiesRemoved;
        if ("*".equals(context.getArg(0)))
        {
            List<Entity> list = new ArrayList<>();
            for (Entity entity : world.getEntities())
            {
                if (!(entity instanceof LivingEntity))
                {
                    list.add(entity);
                }
            }
            entitiesRemoved = this.removeEntities(list, loc, radius, false);
        }
        else
        {
            List<Entity> list = world.getEntities(); // All entites remaining in that list will not get deleted!
            String[] s_entityTypes = StringUtils.explode(",", context.getString(0));
            List<org.bukkit.entity.EntityType> types = new ArrayList<>();
            for (String s_entityType : s_entityTypes)
            {
                if (s_entityType.contains(":"))
                {
                    EntityType type = Match.entity().any(s_entityType.substring(0, s_entityType.indexOf(":")));
                    if (!DROPPED_ITEM.equals(type))
                    {
                        context.sendTranslated(NEGATIVE, "You can only specify data for removing items!");
                        return;
                    }
                    Material itemtype = Match.material().material(s_entityType.substring(s_entityType.indexOf(":") + 1));
                    List<Entity> remList = new ArrayList<>();
                    for (Entity entity : list)
                    {
                        if (entity.getType().equals(DROPPED_ITEM) && ((Item)entity).getItemStack().getType().equals(itemtype))
                        {
                            remList.add(entity);
                        }
                    }
                    list.removeAll(remList);
                }
                else
                {
                    EntityType type = Match.entity().any(s_entityType);
                    if (type == null)
                    {
                        context.sendTranslated(NEGATIVE, "Invalid entity-type!");
                        context.sendTranslated(NEUTRAL, "Use one of those instead:");
                        context.sendMessage(DROPPED_ITEM.toString() + YELLOW + ", " +
                                                GOLD + EntityType.ARROW + YELLOW + ", " +
                                                GOLD + EntityType.BOAT + YELLOW + ", " +
                                                GOLD + EntityType.MINECART + YELLOW + ", " +
                                                GOLD + EntityType.PAINTING + YELLOW + ", " +
                                                GOLD + EntityType.ITEM_FRAME + YELLOW + " or " +
                                                GOLD + EntityType.EXPERIENCE_ORB);
                        return;
                    }
                    if (type.isAlive())
                    {
                        context.sendTranslated(NEGATIVE, "To kill living entities use the {text:/butcher} command!");
                        return;
                    }
                    types.add(type);
                }
            }
            List<Entity> remList = new ArrayList<>();
            for (Entity entity : list)
            {
                if (types.contains(entity.getType()))
                {
                    remList.add(entity);
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
        }
        else if ("*".equals(context.getArg(0)))
        {
            if (radius == -1)
            {
                context.sendTranslated(POSITIVE, "Removed all entities in {world}! ({amount})", world, entitiesRemoved);
                return;
            }
            context.sendTranslated(POSITIVE, "Removed all entities around you! ({amount})", entitiesRemoved);
        }
        else
        {
            if (radius == -1)
            {
                context.sendTranslated(POSITIVE, "Removed {amount} entities in {world}!", entitiesRemoved, world);
                return;
            }
            context.sendTranslated(POSITIVE, "Removed {amount} entities nearby!", entitiesRemoved); // TODO a non-plural version if there is only 1 entity
        }
    }

    @Command(desc = "Gets rid of mobs close to you. Valid types are:\n" +
        "monster, animal, pet, golem, boss, other, creeper, skeleton, spider etc.")
    @IParams({@Grouped(value = @Indexed(label = "types..."), req = false),
              @Grouped(value = @Indexed(label = "radius"), req = false)})
    @NParams(@Named(names = "in", type = World.class, completer = WorldCompleter.class))
    @Flags({@Flag(longName = "lightning", name = "l"), // die with style
            @Flag(longName = "all", name = "a")})// infinite radius
    public void butcher(CubeContext context)
    {
        User sender = null;
        if (context.getSender() instanceof User)
        {
            sender = (User)context.getSender();
        }
        Location loc;
        int radius = this.config.commands.butcherDefaultRadius;
        int removed;
        if (sender == null)
        {
            radius = -1;
            loc = this.config.mainWorld.getSpawnLocation();
        }
        else
        {
            loc = sender.getLocation();
        }
        if (context.hasIndexed(1))
        {
            radius = context.getArg(1, 0);
            if (radius < 0 && !(radius == -1 && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context
                                                                                                         .getSender())))
            {
                context.sendTranslated(NEGATIVE, "The radius has to be a number greater than 0!");
                return;
            }
        }
        if (context.hasFlag("a") && module.perms().COMMAND_BUTCHER_FLAG_ALL.isAuthorized(context.getSender()))
        {
            radius = -1;
        }
        boolean lightning = false;
        if (context.hasFlag("l") && module.perms().COMMAND_BUTCHER_FLAG_LIGHTNING.isAuthorized(context.getSender()))
        {
            lightning = true;
        }
        List<Entity> list;
        if (context.getSender() instanceof User && !(radius == -1))
        {
            list = ((User)context.getSender()).getNearbyEntities(radius, radius, radius);
        }
        else
        {
            list = loc.getWorld().getEntities();
        }
        String[] s_types = { "monster" };
        boolean allTypes = false;
        if (context.hasIndexed(0))
        {
            if (context.getArg(0).equals("*"))
            {
                allTypes = true;
                if (!module.perms().COMMAND_BUTCHER_FLAG_ALLTYPE.isAuthorized(context.getSender()))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to butcher all types of living entities at once!");
                    return;
                }
            }
            else
            {
                s_types = StringUtils.explode(",", context.getString(0));
            }
        }
        List<Entity> remList = new ArrayList<>();
        if (!allTypes)
        {
            for (String s_type : s_types)
            {
                String match = Match.string().matchString(s_type, this.entityRemovals.GROUPED_ENTITY_REMOVAL.keySet());
                EntityType directEntityMatch = null;
                if (match == null)
                {
                    directEntityMatch = Match.entity().living(s_type);
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
                for (Entity entity : list)
                {
                    if (entityRemoval.doesMatch(entity) && entityRemoval.isAllowed(context.getSender()))
                    {
                        remList.add(entity);
                    }
                }
            }
        }
        else
        {
            remList.addAll(list);
        }
        list = new ArrayList<>();
        for (Entity entity : remList)
        {
            if (entity.getType().isAlive())
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

    private int removeEntities(List<Entity> remList, Location loc, int radius, boolean lightning)
    {
        int removed = 0;

        if (radius != -1 && loc == null)
        {
            throw new IllegalStateException("Unknown Location with radius");
        }
        boolean all = radius == -1;
        int radiusSquared = radius * radius;
        final Location entityLocation = new Location(null, 0, 0, 0);
        for (Entity entity : remList)
        {
            if (entity instanceof Player)
            {
                continue;
            }
            entity.getLocation(entityLocation);
            if (!all)
            {

                int distance = (int)(entityLocation.subtract(loc)).lengthSquared();
                if (radiusSquared < distance)
                {
                    continue;
                }
            }
            if (lightning)
            {
                entityLocation.getWorld().strikeLightningEffect(entityLocation);
            }
            entity.remove();
            removed++;
        }
        return removed;
    }
}