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
import org.spongepowered.api.world.gamerule.GameRule;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
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
 * TODO alias for worlds?
 * TODO recreate cmd
 */
@Singleton
@Command(name = "worlds", desc = "Worlds commands")
public class WorldsCommands extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WorldsCommands(I18n i18n, WorldsModifyCommands modify, WorldsTemplateCommands template)
    {
        super(modify, template);
        this.i18n = i18n;
    }

    @Command(desc = "Renames a world")
    public void rename(CommandCause context, ServerWorldProperties world, ResourceKey newName)
    {
        if (Sponge.server().worldManager().world(world.key()).isPresent())
        {
            i18n.send(context, POSITIVE, "The world must be unloaded to rename.");
            return;
        }
        Sponge.server().worldManager().moveWorld(world.key(), newName).whenComplete((b, t) -> {
            i18n.send(context, POSITIVE, "The world {name} was renamed to {name}", world.key().asString(), newName.asString());
        });
    }

    @Command(desc = "Creates a world based on a template")
    public void create(CommandCause context, WorldTemplate template)
    {
        final CompletableFuture<ServerWorld> futureWorld = Sponge.server().worldManager().loadWorld(template);
        if (futureWorld.isDone())
        {
            i18n.send(context, POSITIVE, "The world {world} is already loaded!", futureWorld.join());
            return;
        }
        i18n.send(context, NEUTRAL, "Loading {name}...", template.key().asString());
        futureWorld.whenComplete((w, t) -> {
            if (w != null)
            {
                i18n.send(context, POSITIVE, "World {world} loaded!", w);
            }
            else
            {
                i18n.send(context, NEGATIVE, "Could not load {name#world}", template.key().asString());
            }
        });
    }

    @Command(desc = "Loads a world")
    public void load(CommandCause context, ServerWorldProperties world)
    {
        final CompletableFuture<ServerWorld> futureWorld = Sponge.server().worldManager().loadWorld(world.key());
        if (futureWorld.isDone())
        {
            i18n.send(context, NEGATIVE, "The world {world} is already loaded!", futureWorld.join());
            return;
        }
        i18n.send(context, NEUTRAL, "Loading {name}...", world.key().asString());
        futureWorld.whenComplete((w, t) -> {
            if (w != null)
            {
                i18n.send(context, POSITIVE, "World {world} loaded!", w);
            }
            else
            {
                i18n.send(context, NEGATIVE, "Could not load {name#world}", world.key().asString());
            }
        });
    }

    @Command(desc = "Unload a loaded world")
    public void unload(CommandCause context, ServerWorld world, @Flag boolean force)
    {
        if (!force)
        {
            Collection<ServerPlayer> players = world.players();
            if (!players.isEmpty())
            {
                int amount = players.size();
                i18n.sendN(context, NEGATIVE, amount, "There is still one player on that map!",
                        "There are still {amount} players on that map!", amount);
                return;
            }
        }

        final WorldManager wm = Sponge.server().worldManager();

        final ServerWorld defWorld = wm.defaultWorld();

        ServerWorld evacuation = wm.world(defWorld.key()).get();
        if (evacuation == world)
        {
            world.players().forEach(p -> p.kick(i18n.translate(p, NEGATIVE, "Main world unloading. Flee!")));
        }
        else
        {
            final Vector3i pos = evacuation.properties().spawnPosition();
            final ServerLocation loc = evacuation.location(pos);
            if (!world.players().isEmpty())
            {
                world.players().forEach(p -> p.setLocation(Sponge.server().teleportHelper().findSafeLocation(loc).orElse(loc)));
                i18n.send(context, POSITIVE, "Teleported all players out of {world}", world);
            }
        }

        i18n.send(context, NEUTRAL, "Unloading {name}...", world.key().asString());
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
        final WorldManager wm = Sponge.server().worldManager();
        final Optional<ServerWorld> loadedWorld = wm.world(world);
        if (loadedWorld.isPresent())
        {
            if (!unload)
            {
                i18n.send(context, NEGATIVE, "You have to unload the world first!");
                return;
            }
            final CompletableFuture<Boolean> unloadFuture = wm.unloadWorld(loadedWorld.get());

            unloadFuture.thenCompose(ub -> {
                if (!ub)
                {
                    i18n.send(context, NEGATIVE, "Could not unload {world}", world);
                    return CompletableFuture.completedFuture(false);
                }
                if (!folder)
                {
                    final WorldTemplate build = WorldTemplate.builder().from(wm.loadTemplate(world).join().get()).loadOnStartup(false).build();
                    wm.saveTemplate(build);
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
        return Sponge.server().worldManager().deleteWorld(world);
    }

    @Alias("listworlds")
    @Command(desc = "Lists all worlds")
    public void list(CommandCause context)
    {
        i18n.send(context, POSITIVE, "The following worlds do exist:");
        Component tNotLoaded = i18n.translate(context, "not loaded");
        Component tNotEnabled = i18n.translate(context, "not enabled");

        final WorldManager wm = Sponge.server().worldManager();
        wm.worldKeys().stream().sorted(Comparator.comparing(ResourceKey::asString)).forEach(worldKey -> {
            Component builder =
                Component.text(" - ").append(Component.text(worldKey.asString(), NamedTextColor.GOLD)) // TODO worldname
                .append(Component.space())
                .append(Component.text(worldKey.asString(), NamedTextColor.DARK_AQUA));

            final TextComponent infoText = Component.text("(?)", NamedTextColor.YELLOW)
                                                    .clickEvent(ClickEvent.runCommand("/worlds info" + worldKey.asString()))
                                                    .hoverEvent(HoverEvent.showText(i18n.translate(context, "Click to show world info")));

            if (!wm.world(worldKey).isPresent())
            {
                builder.append(Component.space());

                final TextComponent loadText = loadWorldText(context, worldKey);
                builder.append(tNotEnabled.color(NamedTextColor.RED).append(Component.space()).append(loadText));
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

        if (!Sponge.server().worldManager().world(world.key()).isPresent())
        {
            Component load = loadWorldText(context, world.key());
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
        if (!world.commands() && Sponge.platform().type() == Type.CLIENT)
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
        i18n.send(context, NEUTRAL, "This worlds spawn is at {vector}", new Vector3i(spawn.x(), spawn.y(), spawn.z()));
        if (showGameRules && !world.gameRules().isEmpty()) // Show gamerules
        {
            i18n.send(context, NEUTRAL, "The following game-rules are set:");
            for (Entry<GameRule<?>, ?> entry : world.gameRules().entrySet())
            {

                context.sendMessage(Identity.nil(), Component.text(entry.getKey().name(), NamedTextColor.YELLOW).append(Component.text(": "))
                                                             .append(Component.text(entry.getValue().toString(), NamedTextColor.GOLD)));
            }
        }
    }

    @Command(desc = "Lists the players in a world")
    public void listplayers(CommandCause context, @Default ServerWorld world)
    {
        Collection<ServerPlayer> players = world.players();
        if (players.isEmpty())
        {
            i18n.send(context, NEUTRAL, "There are no players in {world}", world);
            return;
        }
        i18n.send(context, POSITIVE, "The following players are in {world}", world);
        for (ServerPlayer player : players)
        {
            context.sendMessage(Identity.nil(), Component.text(" - ", NamedTextColor.YELLOW).append(Component.text(player.name(), NamedTextColor.DARK_GREEN)));
        }
    }
}
