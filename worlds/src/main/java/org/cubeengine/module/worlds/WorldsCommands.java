/*
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
package org.cubeengine.module.worlds;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.gamerule.GameRule;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

/**
 * WorldsCommands includes the following sub-commands:
 *
 * - create
 * - load
 * - unload
 * - remove
 * - list
 * - info
 * - listplayers
 *
 * TODO autosave?
 * TODO alias for worlds
 */
@Singleton
@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WorldsCommands(I18n i18n, WorldsModifyCommands modify)
    {
        super(modify);
        this.i18n = i18n;
    }

//    @Command(desc = "Creates a new world")
//    public void create(CommandCause context,
//                       String name,
//                       @Default @Named({"dimension", "dim"}) DimensionType dimension,
//                       @Named("seed") String seed,
//                       @Default @Named({"type"}) GeneratorType type,
//                       @Default @Label("generate") @Named({"structure", "struct"}) boolean generateStructures,
//                       @Default @Named({"gamemode", "mode"}) GameMode gamemode,
//                       @Default @Named({"difficulty", "diff"}) Difficulty difficulty,
//                       @Option @Label("name") @Named({"generator", "gen"}) WorldGeneratorModifier generator,
//                       @Flag boolean recreate,
//                       @Flag boolean noload,
//                       @Flag boolean spawnInMemory)
//    {
//        Optional<World> world = Sponge.getServer().getWorld(name);
//        if (world.isPresent())
//        {
//            if (recreate)
//            {
//                i18n.send(context, NEGATIVE, "You have to unload a world before recreating it!");
//                return;
//            }
//            i18n.send(context, NEGATIVE, "A world named {world} already exists and is loaded!", world.get());
//            return;
//        }
//        Optional<WorldProperties> worldProperties = Sponge.getServer().getWorldProperties(name);
//        if (worldProperties.isPresent())
//        {
//            if (!recreate)
//            {
//                i18n.send(context, NEGATIVE, "A world named {name#world} already exists but is not loaded!", name);
//                return;
//            }
//            worldProperties.get().setEnabled(false);
//            String newName = name + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
//            Sponge.getServer().renameWorld(worldProperties.get(), newName);
//            i18n.send(context, POSITIVE, "Old world moved to {name#folder}", newName);
//        }
//
//        WorldArchetype.Builder builder = WorldArchetype.builder().from(WorldArchetypes.OVERWORLD);
//        builder.keepsSpawnLoaded(spawnInMemory);
//        builder.loadsOnStartup(!noload);
//        if (seed != null)
//        {
//            try
//            {
//                builder.seed(Long.parseLong(seed));
//            }
//            catch (NumberFormatException ignore)
//            {
//                builder.seed(seed.hashCode());
//            }
//        }
//
//        builder.generator(type);
//        builder.dimension(dimension);
//        builder.usesMapFeatures(generateStructures);
//        builder.gameMode(gamemode);
//        if (generator != null)
//        {
//            builder.generatorModifiers(generator);
//        }
//        builder.difficulty(difficulty);
//        try
//        {
//            WorldProperties properties = Sponge.getServer().createWorldProperties(name, builder.build("org.cubeengine.customworld:" + UUID.randomUUID().toString(), name));
//            i18n.send(context, POSITIVE, "World {name} successfully created!", name);
//            i18n.send(context, NEUTRAL, "This world is not yet loaded! Click {txt#here} to load.",
//                    i18n.translate(context, TextFormat.NONE, "here").toBuilder().onClick(TextActions.runCommand("/worlds load " + name)).build());
//        }
//        catch (IOException e)
//        {
//            i18n.send(context, NEGATIVE, "Could not create world!");
//            throw new IllegalStateException(e); // TODO handle exception better
//        }
//    }

//    @Command(desc = "Renames a world")
//    public void rename(CommandCause context, WorldProperties world, String newName)
//    {
//        Optional<World> theWorld = Sponge.getServer().getWorld(world.getUniqueId());
//        if (theWorld.isPresent())
//        {
//            i18n.send(context, POSITIVE, "The world must be unloaded to rename.");
//            return;
//        }
//        String oldName = world.getWorldName();
//        Sponge.getServer().renameWorld(world, newName);
//        i18n.send(context, POSITIVE, "The world {name} was renamed to {name}", oldName, newName);
//    }

    @Command(desc = "Loads a world")
    public void load(CommandCause context, ResourceKey world)
    {
        final CompletableFuture<ServerWorld> futureWorld = Sponge.getServer().getWorldManager().loadWorld(world);
        if (futureWorld.isDone())
        {
            i18n.send(context, POSITIVE, "The world {world} is already loaded!", futureWorld.join());
            return;
        }
        else
        {
            i18n.send(context, NEGATIVE, "Loading...", world);
            futureWorld.whenComplete((w, e) -> {
                if (w != null)
                {
                    i18n.send(context, POSITIVE, "World {world} loaded!", w);
                }
                else
                {
                    i18n.send(context, NEGATIVE, "Could not load {name#world}", world);
                }
            });
        }
    }

    @Command(desc = "Unload a loaded world")
    public void unload(CommandCause context, ServerWorld world, @Flag boolean force)
    {
        if (!force)
        {
            Collection<ServerPlayer> entities = world.getPlayers();
            if (!entities.isEmpty())
            {
                int amount = entities.size();
                i18n.sendN(context, NEUTRAL, amount, "There is still one player on that map!",
                        "There are still {amount} players on that map!", amount);
                return;
            }
            i18n.send(context, NEGATIVE, "Could not unload {world}", world);
            return;
        }

        final WorldManager wm = Sponge.getServer().getWorldManager();

        final ServerWorld defWorld = wm.defaultWorld();

        ServerWorld evacuation = wm.world(defWorld.getKey()).get();
        if (evacuation == world)
        {
            world.getPlayers().forEach(p -> p.kick(i18n.translate(p, NEGATIVE, "Main world unloading. Flee!")));
        }
        else
        {
            final Vector3i pos = evacuation.getProperties().spawnPosition();
            final ServerLocation loc = evacuation.getLocation(pos);
            world.getPlayers().forEach(p -> p.setLocation(Sponge.getServer().getTeleportHelper().getSafeLocation(loc).orElse(loc)));
        }

        i18n.send(context, POSITIVE, "Teleported all players out of {world}", world);

        final CompletableFuture<Boolean> futureNoWorld = wm.unloadWorld(world);
        futureNoWorld.thenAccept(b -> {
            if (b)
            {
                i18n.send(context, POSITIVE, "Unloaded the world {world}!", world);
            }
            else
            {
                i18n.send(context, NEGATIVE, "Could not unload {world}", world);
            }
        });
    }

    @Command(desc = "Remove a world", alias = "delete")
    public void remove(CommandCause context, ResourceKey world,
                       @Flag @ParameterPermission // TODO (value = "remove-worldfolder", desc = "Allows deleting the world folder")
                           boolean folder, @Flag boolean unload)
    {
        final Optional<ServerWorld> loadedWorld = Sponge.getServer().getWorldManager().world(world);
        if (loadedWorld.isPresent())
        {
            if (!unload)
            {
                i18n.send(context, NEGATIVE, "You have to unload the world first!");
                return;
            }
            final CompletableFuture<Boolean> unloadFuture = Sponge.getServer().getWorldManager().unloadWorld(loadedWorld.get());

            unloadFuture.thenCompose(ub -> {
                if (!ub)
                {
                    i18n.send(context, NEGATIVE, "Could not unload {world}", world);
                    return CompletableFuture.completedFuture(false);
                }
                if (!folder)
                {
                    final WorldTemplate build = WorldTemplate.builder().from(Sponge.getServer().getWorldManager().loadTemplate(world).join().get()).enabled(false).build();
                    Sponge.getServer().getWorldManager().saveTemplate(build);
                    i18n.send(context, POSITIVE, "The world {world} is now disabled and will not load by itself.", world);
                    return CompletableFuture.completedFuture(false);
                }
                return this.deleteUnloadedWorld(context, world);
            }).thenAccept(deleted -> {
                if (deleted)
                {
                    i18n.send(context, POSITIVE, "Finished deleting the world {world} from disk", world);
                }
            })
            ;
        }
        this.deleteUnloadedWorld(context, world).thenAccept(b -> i18n.send(context, POSITIVE, "Finished deleting the world {world} from disk", world));
    }

    private CompletableFuture<Boolean> deleteUnloadedWorld(CommandCause context, ResourceKey world)
    {
        i18n.send(context, POSITIVE, "Deleting the world {world} from disk...", world);
        return Sponge.getServer().getWorldManager().deleteWorld(world);
    }

    @Alias("listworlds")
    @Command(desc = "Lists all worlds")
    public void list(CommandCause context)
    {
        i18n.send(context, POSITIVE, "The following worlds do exist:");
        Component tNotLoaded = i18n.translate(context, "not loaded");
        Component tNotEnabled = i18n.translate(context, "not enabled");

        Sponge.getServer().getWorldManager().worldKeys().stream().sorted(Comparator.comparing(o -> o.asString())).forEach(worldKey -> {
            Component builder =
                Component.text(" - ").append(Component.text(worldKey.asString(), NamedTextColor.GOLD)) // TODO worldname
                .append(Component.space())
                .append(Component.text(worldKey.asString(), NamedTextColor.DARK_AQUA));

            final TextComponent infoText = Component.text("(?)", NamedTextColor.YELLOW)
                                                    .clickEvent(ClickEvent.runCommand("/worlds info" + worldKey.asString()))
                                                    .hoverEvent(HoverEvent.showText(i18n.translate(context, "Click to show world info")));

            if (!Sponge.getServer().getWorldManager().world(worldKey).isPresent())
            {
                builder.append(Component.space());

                if (Sponge.getServer().getWorldManager().loadTemplate(worldKey).join().get().enabled())
                {
                    final TextComponent loadText = loadWorldText(context, worldKey);
                    builder.append(tNotEnabled.color(NamedTextColor.RED).append(Component.space()).append(loadText));
                }
                else
                {
                    builder.append(tNotEnabled.color(NamedTextColor.RED));
                }
            }
            builder.append(Component.space()).append(infoText);
            context.sendMessage(Identity.nil(), builder);
        });
    }

    private TextComponent loadWorldText(CommandCause context, ResourceKey worldKey)
    {
        return Component.text("(", NamedTextColor.YELLOW).append(i18n.translate(context, "load")).append(Component.text(")"))
                 .clickEvent(ClickEvent.runCommand("/worlds load " + worldKey.asString()));
    }

    @Command(desc = "Show info about a world")
    public void info(CommandCause context, @Default ServerWorldProperties world, @Flag boolean showGameRules)
    {
        context.sendMessage(Identity.nil(), Component.empty());
        i18n.send(context, POSITIVE, "World information for {world}:", world);

        if (!world.enabled())
        {
            i18n.send(context, NEUTRAL, "This world is disabled.");
        }
        else if (!Sponge.getServer().getWorldManager().world(world.getKey()).isPresent())
        {
            Component load = loadWorldText(context, world.getKey());
            i18n.send(context, NEGATIVE, "This world is not loaded. {txt#load}", load);
        }
        if (!world.initialized())
        {
            i18n.send(context, NEUTRAL, "This world has not been initialized.");
        }
        i18n.send(context, NEUTRAL, "Gamemode: {text}", world.gameMode().asComponent());
        i18n.send(context, NEUTRAL, "DimensionType: {input}", world.worldType());
        if (world.worldGenerationConfig().generateFeatures())
        {
//            i18n.send(context, NEUTRAL, "WorldType: {input} with features", world.getGeneratorType().getName());
        }
        else
        {
//            i18n.send(context, NEUTRAL, "WorldType: {input} no features", world.getGeneratorType().getName());
        }

        i18n.send(context, NEUTRAL, "Difficulty {text}", world.difficulty().asComponent());
        if (world.hardcore())
        {
            i18n.send(context, NEUTRAL, "Hardcoremode active");
        }
        if (!world.pvp())
        {
            i18n.send(context, NEUTRAL, "PVP disabled");
        }
        if (!world.commands() && Sponge.getPlatform().getType() == Type.CLIENT)
        {
            i18n.send(context, NEUTRAL, "Commands are not allowed");
        }
        i18n.send(context, NEUTRAL, "Seed: {long}", world.worldGenerationConfig().seed());
//        if (!world.getGeneratorModifiers().isEmpty())
//        {
//            i18n.send(context, NEUTRAL, "Generation is modified by:");
//            for (WorldGeneratorModifier modifier : world.getGeneratorModifiers())
//            {
//                context.sendMessage(Text.of(YELLOW, " - ", GOLD, modifier.getName()).toBuilder().onHover(TextActions.showText(Text.of(GOLD, modifier.getId()))).build());
//            }
//        }
        if (!world.loadOnStartup())
        {
            i18n.send(context, NEUTRAL, "This world will not load automatically on startup!");
        }
        Vector3i spawn = world.spawnPosition();
        i18n.send(context, NEUTRAL, "This worlds spawn is at {vector}", new Vector3i(spawn.getX(), spawn.getY(), spawn.getZ()));
        if (showGameRules && !world.getGameRules().isEmpty()) // Show gamerules
        {
            i18n.send(context, NEUTRAL, "The following game-rules are set:");
            for (Entry<GameRule<?>, ?> entry : world.getGameRules().entrySet())
            {

                context.sendMessage(Identity.nil(), Component.text(entry.getKey().getName(), NamedTextColor.YELLOW).append(Component.text(": "))
                                                             .append(Component.text(entry.getValue().toString(), NamedTextColor.GOLD)));
            }
        }
    }

    @Command(desc = "Lists the players in a world")
    public void listplayers(CommandCause context, @Default ServerWorld world)
    {
        Collection<ServerPlayer> players = world.getPlayers();
        if (players.isEmpty())
        {
            i18n.send(context, NEUTRAL, "There are no players in {world}", world);
            return;
        }
        i18n.send(context, POSITIVE, "The following players are in {world}", world);
        for (ServerPlayer player : players)
        {
            context.sendMessage(Identity.nil(), Component.text(" - ", NamedTextColor.YELLOW).append(Component.text(player.getName(), NamedTextColor.DARK_GREEN)));
        }
    }
}
