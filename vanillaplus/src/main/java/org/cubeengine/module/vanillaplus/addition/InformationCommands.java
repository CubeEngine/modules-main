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
package org.cubeengine.module.vanillaplus.addition;

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
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.Pair;
import org.cubeengine.module.core.util.math.BlockVector2;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.cubeengine.service.matcher.MaterialMatcher;
import org.cubeengine.service.permission.PermissionContainer;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;

import static java.util.Locale.ENGLISH;
import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.text.format.TextColors.*;
import static org.spongepowered.api.util.Direction.getClosest;

public class InformationCommands extends PermissionContainer<VanillaPlus>
{
    private final PeriodFormatter formatter;
    private MaterialMatcher materialMatcher;
    private I18n i18n;

    public InformationCommands(VanillaPlus module, MaterialMatcher materialMatcher, I18n i18n)
    {
        super(module);
        this.materialMatcher = materialMatcher;
        this.i18n = i18n;
        this.formatter = new PeriodFormatterBuilder().appendWeeks().appendSuffix(" week"," weeks").appendSeparator(" ")
                                                     .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
                                                     .appendHours().appendSuffix(" hour"," hours").appendSeparator(" ")
                                                     .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
                                                     .appendSeconds().appendSuffix(" second", " seconds").toFormatter();
    }


    @Command(desc = "Displays the biome type you are standing in.")
    public void biome(CommandSource context,
                      @Optional World world,
                      @Label("block-x") @Optional Integer x,
                      @Label("block-z") @Optional Integer z)
    {
        if (!(context instanceof Player) && (world == null || z == null))
        {
            i18n.sendTranslated(context, NEGATIVE, "Please provide a world and x and z coordinates!");
            return;
        }
        if (z == null)
        {
            Location loc = ((Player)context).getLocation();
            world = (World)loc.getExtent();
            x = loc.getBlockX();
            z = loc.getBlockZ();
        }
        BiomeType biome = world.getBiome(x, z);
        i18n.sendTranslated(context, NEUTRAL, "Biome at {vector:x\\=:z\\=}: {biome}", new BlockVector2(x, z), biome);
    }

    @Command(desc = "Displays the seed of a world.")
    public void seed(CommandSource context, @Optional World world)
    {
        if (world == null)
        {
            if (!(context instanceof Player))
            {
                throw new TooFewArgumentsException();
            }
            world = ((Player)context).getWorld();
        }
        i18n.sendTranslated(context, NEUTRAL, "Seed of {world} is {long#seed}", world, world.getProperties().getSeed());
    }

    @Command(desc = "Displays the direction in which you are looking.")
    @Restricted(value = Player.class, msg = "{text:ProTip}: I assume you are looking right at your screen, right?")
    public void compass(Player context)
    {
        i18n.sendTranslated(context, NEUTRAL, "You are looking to {input#direction}!", getClosest(
            context.getRotation()).name()); // TODO translation of direction
    }

    @Command(desc = "Displays your current depth.")
    @Restricted(value = Player.class, msg = "You dug too deep!")
    public void depth(Player context)
    {
        final int height = context.getLocation().getBlockY();
        if (height > 62)
        {
            i18n.sendTranslated(context, POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} above sealevel)", height, height - 62);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "You are on heightlevel {integer#blocks} ({amount#blocks} below sealevel)", height, 62 - height);
    }

    @Command(desc = "Displays your current location.")
    @Restricted(value = Player.class, msg = "Your position: {text:Right in front of your screen!:color=RED}")
    public void getPos(Player context)
    {
        final Location loc = context.getLocation();
        i18n.sendTranslated(context, NEUTRAL, "Your position is {vector:x\\=:y\\=:z\\=}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Command(desc = "Displays near players(entities/mobs) to you.")
    public void near(CommandSource context, @Optional Integer radius, @Default Player player, @Flag boolean entity, @Flag boolean mob)
    {
        if (radius == null)
        {
            radius = this.module.getConfig().add.commandNearDefaultRadius;
        }
        int squareRadius = radius * radius;
        Location userLocation = player.getLocation();
        Collection<Entity> list = userLocation.getExtent().getEntities();
        LinkedList<Text> outputlist = new LinkedList<>();
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
        LinkedHashMap<Text, Pair<Double, Integer>> groupedEntities = new LinkedHashMap<>();
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
                Text key;
                if (e instanceof Player)
                {
                    key = Text.of(TextColors.DARK_GREEN, i18n.getTranslation(context, TextFormat.NONE, "player"));
                }
                else if (e instanceof Living)
                {
                    key = Text.of(TextColors.DARK_AQUA, e.getType().getTranslation());
                }
                else if (e instanceof Item)
                {
                    key = Text.of(GRAY, e.get(Keys.REPRESENTED_ITEM).get().createStack().getTranslation());
                }
                else
                {
                    key = Text.of(GRAY, e.getType().getTranslation());
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
        Builder builder = Text.builder();
        for (Text key : groupedEntities.keySet())
        {
            builder.append(Text.NEW_LINE)
                   .append(Text.of(GOLD, groupedEntities.get(key).getRight())).append(Text.of("x ")).append(key)
                   .append(Text.of(WHITE, " (", GOLD, Math.round(groupedEntities.get(key).getLeft()), "m", WHITE, ")"));
        }
        if (outputlist.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "Nothing detected nearby!");
            return;
        }

        Text result = Text.of(Text.joinWith(Text.of(WHITE, ", "), outputlist), builder.build());
        if (context.equals(player))
        {
            i18n.sendTranslated(context, NEUTRAL, "Found those nearby you:");
            context.sendMessage(result);
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "Found those nearby {user}:", player);
        context.sendMessage(result);
    }

    private void addNearInformation(List<Text> list, Entity entity, double distance)
    {
        Text s;
        if (entity instanceof Player)
        {
            s = Text.of(TextColors.DARK_GREEN, ((Player)entity).getName());
        }
        else if (entity instanceof Living)
        {
            s = Text.of(TextColors.DARK_AQUA, entity.getType().getName());
        }
        else
        {
            if (entity instanceof Item)
            {
                s = Text.of(GRAY, entity.get(Keys.REPRESENTED_ITEM).get().createStack().getTranslation());
            }
            else
            {
                s = Text.of(GRAY, entity.getType().getTranslation());
            }
        }
        list.add(Text.of(s, WHITE, " (" + GOLD, distance + "m", WHITE + ")"));
    }

    @Command(alias = "pong", desc = "Pong!")
    public void ping(CommandContext context)
    {
        final String label = context.getInvocation().getLabels().get(0).toLowerCase(ENGLISH);
        if (context.isSource(Player.class))
        {
            i18n.sendTranslated(context.getSource(), MessageType.NONE, ("ping".equals(label) ? "pong" : "ping") + "! Your latency: {integer#ping}",
                                ((Player)context.getSource()).getConnection().getLatency());
            return;
        }
        i18n.sendTranslated(context.getSource(), NEUTRAL, label + " in the console?");
    }

    @Command(desc = "Displays chunk, memory and world information.")
    public void lag(CommandSource context)
    {
        //Uptime:
        i18n.sendTranslated(context, POSITIVE, "[{text:CubeEngine-Basics:color=RED}]");
        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
                                                             context.getLocale());
        Date start = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
        Duration dura = new Duration(start.getTime(), System.currentTimeMillis());
        i18n.sendTranslated(context, POSITIVE, "Server has been running since {input#uptime}", df.format(start));
        i18n.sendTranslated(context, POSITIVE, "Uptime: {input#uptime}", formatter.print(dura.toPeriod()));
        //TPS:
        double tps = Sponge.getServer().getTicksPerSecond();
        TextColor color = tps == 20 ? DARK_GREEN :
                       tps > 17 ?  YELLOW :
                       tps > 10 ?  RED :
                       tps == 0 ?  YELLOW :
                                   DARK_RED;
        i18n.sendTranslated(context, POSITIVE, "Current TPS: {txt}", Text.of(color, tps));
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
        i18n.sendTranslated(context, POSITIVE, "Memory Usage: {input#memused}/{integer#memcom}/{integer#memMax} MB", memused, memCom, memMax);
        //Worlds with loaded Chunks / Entities
        for (World world : Sponge.getServer().getWorlds())
        {
            String type = world.getProperties().getDimensionType().getName();
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
            i18n.sendTranslated(context, POSITIVE, "{world} ({input#environment}): {amount} chunks {amount} entities", world, type, loadedChunks, entities);
        }
    }

    /* TODO handle duplicate cmd registrations
    @Command(desc = "Displays all loaded worlds", alias = {"worldlist","worlds"})
    public void listWorlds(CommandSource context)
    {
        i18n.sendTranslated(context, POSITIVE, "Loaded worlds:");
        for (World world : Sponge.getServer().getWorlds())
        {
            context.sendMessage(Text.of(" ", TextColors.WHITE, "- ", GOLD, world.getName(), WHITE, ":", BLUE,
                                        world.getProperties().getDimensionType().getName()));
        }
    }
    */
}
