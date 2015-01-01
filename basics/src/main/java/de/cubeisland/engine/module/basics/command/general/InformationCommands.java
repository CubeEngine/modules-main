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
package de.cubeisland.engine.module.basics.command.general;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.parametric.Default;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.command.parameter.TooFewArgumentsException;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.Direction;
import de.cubeisland.engine.core.util.Pair;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.core.util.math.BlockVector2;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.MathHelper;
import de.cubeisland.engine.module.basics.Basics;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static de.cubeisland.engine.core.util.ChatFormat.*;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static java.util.Locale.ENGLISH;

public class InformationCommands
{
    private final PeriodFormatter formatter;
    private final Basics module;

    public InformationCommands(Basics module)
    {
        this.module = module;
        this.formatter = new PeriodFormatterBuilder().appendWeeks().appendSuffix(" week"," weeks").appendSeparator(" ")
                                                     .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
                                                     .appendHours().appendSuffix(" hour"," hours").appendSeparator(" ")
                                                     .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
                                                     .appendSeconds().appendSuffix(" second", " seconds").toFormatter();
    }

    @Command(desc = "Displays the biome type you are standing in.")
    public void biome(CommandContext context,
                      @Label("world") @Optional World world,
                      @Label("block-x") @Optional Integer x,
                      @Label("block-z") @Optional Integer z)
    {
        if (!context.isSource(User.class) && (!context.hasPositional(2) || world == null))
        {
            context.sendTranslated(NEGATIVE, "Please provide a world and x and z coordinates!");
            return;
        }
        if (!context.hasPositional(2) && context.isSource(User.class))
        {
            User user = (User)context.getSource();
            Location loc = user.getLocation();
            world = loc.getWorld();
            x = loc.getBlockX();
            z = loc.getBlockZ();
        }
        Biome biome = world.getBiome(x, z);
        context.sendTranslated(NEUTRAL, "Biome at {vector:x\\=:z\\=}: {biome}", new BlockVector2(x, z), biome);
    }

    @Command(desc = "Displays the seed of a world.")
    public void seed(CommandContext context, @Label("world") @Optional World world)
    {
        if (world == null)
        {
            if (!context.isSource(User.class))
            {
                throw new TooFewArgumentsException();
            }
            world = ((User)context.getSource()).getWorld();
        }
        context.sendTranslated(NEUTRAL, "Seed of {world} is {long#seed}", world, world.getSeed());
    }

    @Command(desc = "Displays the direction in which you are looking.")
    @Restricted(value = User.class, msg = "{text:ProTip}: I assume you are looking right at your screen, right?")
    public void compass(CommandContext context)
    {
        int direction = Math.round(((User)context.getSource()).getLocation().getYaw() + 180f + 360f) % 360;
        context.sendTranslated(NEUTRAL, "You are looking to {input#direction}!", Direction.matchDirection(direction).name()); // TODO translate direction
    }

    @Command(desc = "Displays your current depth.")
    @Restricted(value = User.class, msg = "You dug too deep!")
    public void depth(CommandContext context)
    {
        final int height = ((User)context.getSource()).getLocation().getBlockY();
        if (height > 62)
        {
            context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} above sealevel)", height, height - 62);
            return;
        }
        context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} below sealevel)", height, 62 - height);
    }

    @Command(desc = "Displays your current location.")
    @Restricted(value = User.class, msg = "Your position: {text:Right in front of your screen!:color=RED}")
    public void getPos(CommandContext context)
    {
        final Location loc = ((User)context.getSource()).getLocation();
        context.sendTranslated(NEUTRAL, "Your position is {vector:x\\=:y\\=:z\\=}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Command(desc = "Displays near players(entities/mobs) to you.")
    public void near(CommandContext context,
                     @Optional @Label("radius") Integer radius,
                     @Default @Optional @Label("player") User user,
                     @Flag(longName = "entity", name = "e") boolean entityFlag,
                     @Flag(longName = "mob", name = "m") boolean mobFlag)
    {
        //new cmd system showing default message via @Default context.sendTranslated(NEUTRAL, "I am right {text:behind:color=RED} you!");
        if (radius == null)
        {
            radius = this.module.getConfiguration().commands.nearDefaultRadius; // TODO create defaultProvider for this
        }
        int squareRadius = radius * radius;
        Location userLocation = user.getLocation();
        List<Entity> list = userLocation.getWorld().getEntities();
        LinkedList<String> outputlist = new LinkedList<>();
        TreeMap<Double, List<Entity>> sortedMap = new TreeMap<>();
        final Location entityLocation = new Location(null, 0, 0, 0);
        for (Entity entity : list)
        {
            entity.getLocation(entityLocation);
            double distance = entityLocation.distanceSquared(userLocation);
            if (!entityLocation.equals(userLocation) && distance < squareRadius)
            {
                if (entityFlag || (mobFlag && entity instanceof LivingEntity) || entity instanceof Player)
                {
                    List<Entity> sublist = sortedMap.get(distance);
                    if (sublist == null)
                    {
                        sublist = new ArrayList<>();
                    }
                    sublist.add(entity);
                    sortedMap.put(distance, sublist);
                }
            }
        }
        int i = 0;
        LinkedHashMap<String, Pair<Double, Integer>> groupedEntities = new LinkedHashMap<>();
        for (double dist : sortedMap.keySet())
        {
            i++;
            for (Entity entity : sortedMap.get(dist))
            {
                if (i <= 10)
                {
                    this.addNearInformation(outputlist, entity, Math.sqrt(dist));
                }
                else
                {
                    String key;
                    if (entity instanceof Player)
                    {
                        key = DARK_GREEN + "player";
                    }
                    else if (entity instanceof LivingEntity)
                    {
                        key = ChatFormat.DARK_AQUA + Match.entity().getNameFor(entity.getType());
                    }
                    else if (entity instanceof Item)
                    {
                        key = ChatFormat.GREY + Match.material().getNameFor(((Item)entity).getItemStack());
                    }
                    else
                    {
                        key = ChatFormat.GREY + Match.entity().getNameFor(entity.getType());
                    }
                    Pair<Double, Integer> pair = groupedEntities.get(key);
                    if (pair == null)
                    {
                        pair = new Pair<>(Math.sqrt(dist), 1);
                        groupedEntities.put(key, pair);
                    }
                    else
                    {
                        pair.setRight(pair.getRight() + 1);
                    }
                }
            }
        }
        StringBuilder groupedOutput = new StringBuilder();
        for (String key : groupedEntities.keySet())
        {
            groupedOutput.append("\n").append(GOLD).append(groupedEntities.get(key).getRight()).append("x ")
                         .append(key).append(WHITE).append(" (").append(GOLD)
                         .append(MathHelper.round(groupedEntities.get(key).getLeft())).append("m")
                         .append(WHITE).append(")");
        }
        if (outputlist.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "Nothing detected nearby!");
        }
        else
        {
            String result;
            result = StringUtils.implode(WHITE + ", ", outputlist);
            result += groupedOutput.toString();
            if (context.getSource().equals(user))
            {
                context.sendTranslated(NEUTRAL, "Found those nearby you:\n{}", result);
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Found those nearby {user}:\n{}", user, result);
            }
        }
    }

    private void addNearInformation(List<String> list, Entity entity, double distance)
    {
        String s;
        if (entity instanceof Player)
        {
            s = DARK_GREEN + ((Player)entity).getName();
        }
        else if (entity instanceof LivingEntity)
        {
            s = ChatFormat.DARK_AQUA + Match.entity().getNameFor(entity.getType());
        }
        else
        {
            if (entity instanceof Item)
            {
                s = ChatFormat.GREY + Match.material().getNameFor(((Item)entity).getItemStack());
            }
            else
            {
                s = ChatFormat.GREY + Match.entity().getNameFor(entity.getType());
            }
        }
        s += WHITE + " (" + GOLD + distance + "m" + WHITE + ")";
        list.add(s);
    }

    @Command(alias = "pong", desc = "Pong!")
    public void ping(CommandContext context)
    {
        final String label = context.getInvocation().getLabels().get(0).toLowerCase(ENGLISH);
        if (context.isSource(User.class))
        {
            context.sendTranslated(NONE, ("ping".equals(label) ? "pong" : "ping") + "! Your latency: {integer#ping}", ((User)context.getSource()).getPing());
            return;
        }
        context.sendTranslated(NEUTRAL, label + " in the console?");
    }

    @Command(desc = "Displays chunk, memory and world information.")
    public void lag(CommandContext context, @Flag(name = "r", longName = "reset") boolean reset)
    {
        if (reset)
        {
            if (module.perms().COMMAND_LAG_RESET.isAuthorized(context.getSource()))
            {
                this.module.getLagTimer().resetLowestTPS();
                context.sendTranslated(POSITIVE, "Reset lowest TPS!");
                return;
            }
            context.sendTranslated(NEGATIVE, "You are not allowed to do this!");
            return;
        }
        //Uptime:
        context.sendTranslated(POSITIVE, "[{text:CubeEngine-Basics:color=RED}]");
        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
                     context.getSource().getLocale());
        Date start = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
        Duration dura = new Duration(start.getTime(), System.currentTimeMillis());
        context.sendTranslated(POSITIVE, "Server has been running since {input#uptime}", df.format(start));
        context.sendTranslated(POSITIVE, "Uptime: {input#uptime}", formatter.print(dura.toPeriod()));
        //TPS:
        float tps = this.module.getLagTimer().getAverageTPS();
        String color = tps == 20 ? DARK_GREEN.toString() :
                       tps > 17 ?  YELLOW.toString() :
                       tps > 10 ?  RED.toString() :
                       tps == 0 ?  (YELLOW.toString() + "NaN") :
                                   DARK_RED.toString();
        context.sendTranslated(POSITIVE, "Current TPS: {input#color}{decimal#tps:1}", color, tps);
        Pair<Long, Float> lowestTPS = this.module.getLagTimer().getLowestTPS();
        if (lowestTPS.getRight() != 20)
        {
            color = ChatFormat.parseFormats(tps > 17 ? YELLOW.toString() : tps > 10 ? RED.toString() : DARK_RED.toString());
            Date date = new Date(lowestTPS.getLeft());
            context.sendTranslated(POSITIVE, "Lowest TPS was {}{decimal#tps:1} ({input#date})", color, lowestTPS.getRight(), df.format(date));
            long timeSinceLastLowTPS = System.currentTimeMillis() - this.module.getLagTimer().getLastLowTPS();
            if (tps == 20 && TimeUnit.MINUTES.convert(timeSinceLastLowTPS,TimeUnit.MILLISECONDS) < 1)
            {
                context.sendTranslated(NEGATIVE, "TPS was low in the last minute!");
            }
        }
        //Memory
        long memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1048576;
        long memCom = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() / 1048576;
        long memMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / 1048576;
        String memused;
        long memUsePercent = 100 * memUse / memMax;
        if (memUsePercent > 90)
        {
            if (memUsePercent > 95)
            {
                memused = DARK_RED.toString();
            }
            else
            {
                memused = RED.toString();
            }
        }
        else if (memUsePercent > 60)
        {
            memused = YELLOW.toString();
        }
        else
        {
            memused = DARK_GREEN.toString();
        }
        memused += memUse;
        context.sendTranslated(POSITIVE, "Memory Usage: {input#memused}/{integer#memcom}/{integer#memMax} MB", memused, memCom, memMax);
        //Worlds with loaded Chunks / Entities
        for (World world : Bukkit.getServer().getWorlds())
        {
            String type = world.getEnvironment().name();
            int loadedChunks = world.getLoadedChunks().length;
            int entities = world.getEntities().size();
            context.sendTranslated(POSITIVE, "{world} ({input#environment}): {amount} chunks {amount} entities", world, type, loadedChunks, entities);
        }
    }


    @Command(desc = "Displays all loaded worlds", alias = {"worldlist","worlds"})
    public void listWorlds(CommandContext context)
    {
        context.sendTranslated(POSITIVE, "Loaded worlds:");
        String format = " " + WHITE + "- " + GOLD + "%s" + WHITE + ":" + INDIGO + "%s";
        for (World world : Bukkit.getServer().getWorlds())
        {
            context.sendMessage(String.format(format, world.getName(), world.getEnvironment().name()));
        }
    }
}
