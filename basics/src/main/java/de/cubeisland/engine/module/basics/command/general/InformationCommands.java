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
import java.util.Locale;
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

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Flag;
import de.cubeisland.engine.core.command.reflected.context.Flags;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.Direction;
import de.cubeisland.engine.core.util.Pair;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.matcher.Match;
import de.cubeisland.engine.core.util.math.BlockVector2;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.core.util.math.MathHelper;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

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
    @IParams({@Grouped(value = @Indexed(label = "world"), req = false),
              @Grouped(value = @Indexed(label = "block-x"), req = false),
              @Grouped(value = @Indexed(label = "block-z"), req = false)})
    public void biome(CubeContext context)
    {
        World world;
        Integer x;
        Integer z;
        if (context.hasIndexed(2))
        {
            world = context.getArg(0, null);
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "Unknown world {input#world}!", context.getArg(0));
                return;
            }
            x = context.getArg(1, null);
            z = context.getArg(2, null);
            if (x == null || z == null)
            {
                context.sendTranslated(NEGATIVE, "Please provide valid whole number x and/or z coordinates!");
                return;
            }
        }
        else if (context.getSender() instanceof User)
        {
            User user = (User)context.getSender();
            Location loc = user.getLocation();
            world = loc.getWorld();
            x = loc.getBlockX();
            z = loc.getBlockZ();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Please provide a world and x and z coordinates!");
            return;
        }
        Biome biome = world.getBiome(x, z);
        context.sendTranslated(NEUTRAL, "Biome at {vector:x\\=:z\\=}: {biome}", new BlockVector2(x, z), biome);
    }

    @Command(desc = "Displays the seed of a world.")
    @IParams(@Grouped(value = @Indexed(label = "world"), req = false))
    public void seed(CubeContext context)
    {
        World world = null;
        if (context.hasIndexed(0))
        {
            world = context.getArg(0, null);
            if (world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input#world} not found!", context.getArg(0));
                return;
            }
        }
        if (world == null)
        {
            if (context.getSender() instanceof User)
            {
                world = ((User)context.getSender()).getWorld();
            }
            else
            {
                context.sendTranslated(NEGATIVE, "No world specified!");
                return;
            }
        }
        context.sendTranslated(NEUTRAL, "Seed of {world} is {long#seed}", world, world.getSeed());
    }

    @Command(desc = "Displays the direction in which you are looking.")
    public void compass(CubeContext context)
    {
        CommandSender sender = context.getSender();
        if (sender instanceof User)
        {
            int direction = Math.round(((User)sender).getLocation().getYaw() + 180f + 360f) % 360;
            String dir;
            dir = Direction.matchDirection(direction).name();
            sender.sendTranslated(NEUTRAL, "You are looking to {input#direction}!", dir); // TODO translate direction
        }
        else
        {
            context.sendTranslated(NEUTRAL, "{text:ProTip}: I assume you are looking right at your screen, right?");
        }
    }

    @Command(desc = "Displays your current depth.")
    public void depth(CubeContext context)
    {
        if (context.getSender() instanceof User)
        {
            final int height = ((User)context.getSender()).getLocation().getBlockY();
            if (height > 62)
            {
                context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} above sealevel)", height, height - 62);
            }
            else
            {
                context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} below sealevel)", height, 62 - height);
            }
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You dug too deep!");
        }
    }

    @Command(desc = "Displays your current location.")
    public void getPos(CubeContext context)
    {
        if (context.getSender() instanceof User)
        {
            final Location loc = ((User)context.getSender()).getLocation();
            context.sendTranslated(NEUTRAL, "Your position is {vector:x\\=:y\\=:z\\=}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Your position: {text:Right in front of your screen!:color=RED}");
        }
    }

    @Command(desc = "Displays near players(entities/mobs) to you.")
    @IParams({@Grouped(value = @Indexed(label = "radius"), req = false),
              @Grouped(value = @Indexed(label = "player", type = User.class), req = false)})
    @Flags({@Flag(longName = "entity", name = "e"),
            @Flag(longName = "mob", name = "m")})
    public void near(CubeContext context)
    {
        User user;
        if (context.hasIndexed(1))
        {
            user = context.getArg(1);
        }
        else if (context.getSender() instanceof User)
        {
            user = (User)context.getSender();
        }
        else
        {
            context.sendTranslated(NEUTRAL, "I am right {text:behind:color=RED} you!");
            return;
        }
        int radius = this.module.getConfiguration().commands.nearDefaultRadius;
        if (context.hasIndexed(0))
        {
            radius = context.getArg(0, radius);
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
            if (!entityLocation.equals(userLocation))
            {
                if (distance < squareRadius)
                {
                    if (context.hasFlag("e") || (context.hasFlag("m") && entity instanceof LivingEntity) || entity instanceof Player)
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
                        key = ChatFormat.DARK_GREEN + "player";
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
            groupedOutput.append("\n").append(ChatFormat.GOLD).append(groupedEntities.get(key).getRight()).append("x ")
                         .append(key).append(ChatFormat.WHITE).append(" (").append(ChatFormat.GOLD)
                         .append(MathHelper.round(groupedEntities.get(key).getLeft())).append("m")
                         .append(ChatFormat.WHITE).append(")");
        }
        if (outputlist.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "Nothing detected nearby!");
        }
        else
        {
            String result;
            result = StringUtils.implode(ChatFormat.WHITE + ", ", outputlist);
            result += groupedOutput.toString();
            if (context.getSender().equals(user))
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
            s = ChatFormat.DARK_GREEN + ((Player)entity).getName();
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
        s += ChatFormat.WHITE + " (" + ChatFormat.GOLD + distance + "m" + ChatFormat.WHITE + ")";
        list.add(s);
    }

    @Command(alias = "pong", desc = "Pong!")
    public void ping(CubeContext context)
    {
        final String label = context.getLabel().toLowerCase(Locale.ENGLISH);
        if (context.getSender() instanceof ConsoleCommandSender)
        {
            context.sendTranslated(NEUTRAL, label + " in the console?");
        }
        else
        {
            context.sendTranslated(NONE, ("ping".equals(label) ? "pong" : "ping") + "! Your latency: {integer#ping}", ((User)context.getSender()).getPing());
        }
    }

    @Command(desc = "Displays chunk, memory and world information.")
    @Flags(@Flag(longName = "reset" , name = "r"))
    public void lag(CubeContext context)
    {
        if (context.hasFlag("r"))
        {
            if (module.perms().COMMAND_LAG_RESET.isAuthorized(context.getSender()))
            {
                this.module.getLagTimer().resetLowestTPS();
                context.sendTranslated(POSITIVE, "Reset lowest TPS!");
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to do this!");
            }
            return;
        }
        //Uptime:
        context.sendTranslated(POSITIVE, "[{text:CubeEngine-Basics:color=RED}]");
        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
                     context.getSender().getLocale());
        Date start = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
        Duration dura = new Duration(start.getTime(), System.currentTimeMillis());
        context.sendTranslated(POSITIVE, "Server has been running since {input#uptime}", df.format(start));
        context.sendTranslated(POSITIVE, "Uptime: {input#uptime}", formatter.print(dura.toPeriod()));
        //TPS:
        float tps = this.module.getLagTimer().getAverageTPS();
        String color = tps == 20 ? ChatFormat.DARK_GREEN.toString() : tps > 17 ? ChatFormat.YELLOW.toString() : tps > 10 ? ChatFormat.RED.toString() : tps == 0 ? (ChatFormat.YELLOW.toString() + "NaN") : ChatFormat.DARK_RED.toString();
        context.sendTranslated(POSITIVE, "Current TPS: {input#color}{decimal#tps:1}", color, tps);
        Pair<Long, Float> lowestTPS = this.module.getLagTimer().getLowestTPS();
        if (lowestTPS.getRight() != 20)
        {
            color = ChatFormat.parseFormats(tps > 17 ? ChatFormat.YELLOW.toString() : tps > 10 ? ChatFormat.RED.toString() : ChatFormat.DARK_RED.toString());
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
                memused = ChatFormat.DARK_RED.toString();
            }
            else
            {
                memused = ChatFormat.RED.toString();
            }
        }
        else if (memUsePercent > 60)
        {
            memused = ChatFormat.YELLOW.toString();
        }
        else
        {
            memused = ChatFormat.DARK_GREEN.toString();
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
    public void listWorlds(CubeContext context)
    {
        context.sendTranslated(POSITIVE, "Loaded worlds:");
        String format = " " + ChatFormat.WHITE + "- " + ChatFormat.GOLD + "%s" + ChatFormat.WHITE + ":" + ChatFormat.INDIGO + "%s";
        for (World world : Bukkit.getServer().getWorlds())
        {
            context.sendMessage(String.format(format, world.getName(), world.getEnvironment().name()));
        }
    }
}