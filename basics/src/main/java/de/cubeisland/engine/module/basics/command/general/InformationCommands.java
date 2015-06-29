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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parameter.TooFewArgumentsException;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.core.util.ChatFormat;
import de.cubeisland.engine.module.core.util.Pair;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.formatter.MessageType;
import de.cubeisland.engine.module.core.util.matcher.MaterialMatcher;
import de.cubeisland.engine.module.core.util.math.BlockVector2;
import de.cubeisland.engine.module.core.util.math.BlockVector3;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.world.WorldManager;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;

import static de.cubeisland.engine.module.core.util.ChatFormat.*;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.*;
import static java.util.Locale.ENGLISH;
import static org.spongepowered.api.util.Direction.*;

public class InformationCommands
{
    private final PeriodFormatter formatter;
    private final Basics module;
    private WorldManager wm;
    private MaterialMatcher materialMatcher;

    public InformationCommands(Basics module, WorldManager wm, MaterialMatcher materialMatcher)
    {
        this.module = module;
        this.wm = wm;
        this.materialMatcher = materialMatcher;
        this.formatter = new PeriodFormatterBuilder().appendWeeks().appendSuffix(" week"," weeks").appendSeparator(" ")
                                                     .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
                                                     .appendHours().appendSuffix(" hour"," hours").appendSeparator(" ")
                                                     .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
                                                     .appendSeconds().appendSuffix(" second", " seconds").toFormatter();
    }

    @Command(desc = "Displays the biome type you are standing in.")
    public void biome(CommandSender context,
                      @Optional World world,
                      @Label("block-x") @Optional Integer x,
                      @Label("block-z") @Optional Integer z)
    {
        if (!(context instanceof User) && (world == null || z == null))
        {
            context.sendTranslated(NEGATIVE, "Please provide a world and x and z coordinates!");
            return;
        }
        if (z == null)
        {
            Location loc = ((User)context).getLocation();
            world = (World)loc.getExtent();
            x = loc.getBlockX();
            z = loc.getBlockZ();
        }
        BiomeType biome = world.getBiome(x, z);
        context.sendTranslated(NEUTRAL, "Biome at {vector:x\\=:z\\=}: {biome}", new BlockVector2(x, z), biome);
    }

    @Command(desc = "Displays the seed of a world.")
    public void seed(CommandSender context, @Optional World world)
    {
        if (world == null)
        {
            if (!(context instanceof User))
            {
                throw new TooFewArgumentsException();
            }
            world = ((User)context).getWorld();
        }
        context.sendTranslated(NEUTRAL, "Seed of {world} is {long#seed}", world, world.getWorldStorage().getWorldProperties().getSeed());
    }

    @Command(desc = "Displays the direction in which you are looking.")
    @Restricted(value = User.class, msg = "{text:ProTip}: I assume you are looking right at your screen, right?")
    public void compass(User context)
    {
        context.sendTranslated(NEUTRAL, "You are looking to {input#direction}!", getClosest(
            context.asPlayer().getRotation()).name()); // TODO translation of direction
    }

    @Command(desc = "Displays your current depth.")
    @Restricted(value = User.class, msg = "You dug too deep!")
    public void depth(User context)
    {
        final int height = context.getLocation().getBlockY();
        if (height > 62)
        {
            context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} above sealevel)", height, height - 62);
            return;
        }
        context.sendTranslated(POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} below sealevel)", height, 62 - height);
    }

    @Command(desc = "Displays your current location.")
    @Restricted(value = User.class, msg = "Your position: {text:Right in front of your screen!:color=RED}")
    public void getPos(User context)
    {
        final Location loc = context.getLocation();
        context.sendTranslated(NEUTRAL, "Your position is {vector:x\\=:y\\=:z\\=}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Command(desc = "Displays near players(entities/mobs) to you.")
    public void near(CommandSender context, @Optional Integer radius, @Default User player, @Flag boolean entity, @Flag boolean mob)
    {
        if (radius == null)
        {
            radius = this.module.getConfiguration().commands.nearDefaultRadius;
        }
        int squareRadius = radius * radius;
        Location userLocation = player.getLocation();
        Collection<Entity> list = ((World)userLocation.getExtent()).getEntities();
        LinkedList<String> outputlist = new LinkedList<>();
        TreeMap<Double, List<Entity>> sortedMap = new TreeMap<>();
        for (Entity e : list)
        {
            Location entityLocation = e.getLocation();
            double distance = entityLocation.getPosition().distanceSquared(userLocation.getPosition());
            if (!entityLocation.equals(userLocation) && distance < squareRadius)
            {
                if (entity || (mob && e instanceof Living) || e instanceof Player)
                {
                    List<Entity> sublist = sortedMap.get(distance);
                    if (sublist == null)
                    {
                        sublist = new ArrayList<>();
                    }
                    sublist.add(e);
                    sortedMap.put(distance, sublist);
                }
            }
        }
        int i = 0;
        LinkedHashMap<String, Pair<Double, Integer>> groupedEntities = new LinkedHashMap<>();
        for (double dist : sortedMap.keySet())
        {
            i++;
            for (Entity e : sortedMap.get(dist))
            {
                if (i <= 10)
                {
                    this.addNearInformation(outputlist, e, Math.sqrt(dist));
                    continue;
                }
                String key;
                if (e instanceof Player)
                {
                    key = DARK_GREEN + "player";
                }
                else if (e instanceof Living)
                {
                    key = ChatFormat.DARK_AQUA + e.getType().getName();
                }
                else if (e instanceof Item)
                {
                    key = ChatFormat.GREY + materialMatcher.getNameFor(((Item)e).getItemData().getValue());
                }
                else
                {
                    key = ChatFormat.GREY + e.getType().getName();
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
        StringBuilder groupedOutput = new StringBuilder();
        for (String key : groupedEntities.keySet())
        {
            groupedOutput.append("\n").append(GOLD).append(groupedEntities.get(key).getRight()).append("x ")
                         .append(key).append(WHITE).append(" (").append(GOLD)
                         .append(Math.round(groupedEntities.get(key).getLeft())).append("m")
                         .append(WHITE).append(")");
        }
        if (outputlist.isEmpty())
        {
            context.sendTranslated(NEGATIVE, "Nothing detected nearby!");
            return;
        }
        String result;
        result = StringUtils.implode(WHITE + ", ", outputlist);
        result += groupedOutput.toString();
        if (context.equals(player))
        {
            context.sendTranslated(NEUTRAL, "Found those nearby you:");
            context.sendMessage(result);
            return;
        }
        context.sendTranslated(NEUTRAL, "Found those nearby {user}:", player);
        context.sendMessage(result);
    }

    private void addNearInformation(List<String> list, Entity entity, double distance)
    {
        String s;
        if (entity instanceof Player)
        {
            s = DARK_GREEN + ((Player)entity).getName();
        }
        else if (entity instanceof Living)
        {
            s = ChatFormat.DARK_AQUA + entity.getType().getName();
        }
        else
        {
            if (entity instanceof Item)
            {
                s = ChatFormat.GREY + materialMatcher.getNameFor(((Item)entity).getItemData().getValue());
            }
            else
            {
                s = ChatFormat.GREY + entity.getType().getName();
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
            context.sendTranslated(MessageType.NONE, ("ping".equals(label) ? "pong" : "ping") + "! Your latency: {integer#ping}", ((User)context.getSource()).asPlayer().getConnection().getPing());
            return;
        }
        context.sendTranslated(NEUTRAL, label + " in the console?");
    }

    @Command(desc = "Displays chunk, memory and world information.")
    public void lag(CommandSender context, @Flag boolean reset)
    {
        if (reset)
        {
            if (module.perms().COMMAND_LAG_RESET.isAuthorized(context))
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
                                                             context.getLocale());
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
        for (World world : wm.getWorlds())
        {
            String type = world.getWorldStorage().getWorldProperties().getDimensionType().getName();
            int loadedChunks;
            if (world.getLoadedChunks() instanceof Collection)
            {
                loadedChunks = ((Collection)world.getLoadedChunks()).size();
            }
            else
            {
                loadedChunks = 0;
                for (Chunk chunk : world.getLoadedChunks())
                {
                    loadedChunks++;
                }
            }
            int entities = world.getEntities().size();
            context.sendTranslated(POSITIVE, "{world} ({input#environment}): {amount} chunks {amount} entities", world, type, loadedChunks, entities);
        }
    }


    @Command(desc = "Displays all loaded worlds", alias = {"worldlist","worlds"})
    public void listWorlds(CommandSender context)
    {
        context.sendTranslated(POSITIVE, "Loaded worlds:");
        String format = " " + WHITE + "- " + GOLD + "%s" + WHITE + ":" + INDIGO + "%s";
        for (World world : wm.getWorlds())
        {
            context.sendMessage(String.format(format, world.getName(), world.getWorldStorage().getWorldProperties().getDimensionType().getName()));
        }
    }
}
