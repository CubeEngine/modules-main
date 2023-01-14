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

import java.util.Random;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ComponentUtil;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.WorldType;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.server.WorldManager;
import org.spongepowered.api.world.server.WorldTemplate;
import org.spongepowered.api.world.server.WorldTemplate.Builder;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "template", desc = "Worlds modify template commands")
@Alias("wtemplate")
public class WorldsTemplateCommands extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WorldsTemplateCommands(I18n i18n)
    {
        this.i18n = i18n;
    }


    @Command(desc = "Creates a new world template")
    public void create(CommandCause context,
                         ResourceKey worldKey,
                         @Default WorldType type,
                         @Option String displayName,
                         @Flag boolean loadOnStartup)
    {
        final WorldManager wm = Sponge.server().worldManager();
        if (wm.worldExists(worldKey))
        {
            i18n.send(context, NEGATIVE, "A world {name} already exists!", worldKey.asString());
            return;
        }

        final WorldGenerationConfig wgenCfg = WorldGenerationConfig.builder().seed(new Random().nextLong())
                                                                           .generateStructures(true).generateBonusChest(false).build();
        final Builder builder = WorldTemplate.builder();
        if (displayName != null)
        {
            final Component display = ComponentUtil.fromLegacy(displayName);
           builder.add(Keys.DISPLAY_NAME, display);
        }

        ChunkGenerator generator = ChunkGenerator.overworld();
        if (type == WorldTypes.THE_NETHER.get())
        {
            generator = ChunkGenerator.theNether();
        }
        else if (type == WorldTypes.THE_END.get())
        {
            generator = ChunkGenerator.theEnd();
        }

        final WorldTemplate template = builder
                        .add(Keys.WORLD_TYPE, type)
                        .add(Keys.CHUNK_GENERATOR, generator)
                        .add(Keys.WORLD_GEN_CONFIG, wgenCfg)
                        .add(Keys.GAME_MODE, GameModes.SURVIVAL.get())
                        .add(Keys.WORLD_DIFFICULTY, Difficulties.NORMAL.get())
                        .add(Keys.SERIALIZATION_BEHAVIOR, SerializationBehavior.AUTOMATIC)
                        .add(Keys.IS_LOAD_ON_STARTUP, loadOnStartup)
                        .add(Keys.PERFORM_SPAWN_LOGIC, true)
                        .add(Keys.HARDCORE, false)
                        .add(Keys.COMMANDS, true)
                        .add(Keys.PVP, true)
//                .viewDistance(distance)
//                .spawnPosition(pos)
                        .key(worldKey)
                        .build();
        Sponge.server().dataPackManager().save(template).whenComplete((b, t) -> {
            i18n.send(context, POSITIVE, "World Template {name#key} {txt#display} created!", worldKey.asString(), displayName == null ? worldKey.asString() : displayName);
            i18n.send(context, NEUTRAL, "Use {name#command} commands to further modify your world template", "/worlds template");
            i18n.send(context, NEUTRAL, "Use {name#command} to create the world and load it", "/worlds create " + worldKey.asString());
            // TODO show info
        });
    }

    @Command(desc = "Sets world spawn")
    public void spawn(CommandCause context, WorldTemplate world, Vector3i spawnPoint)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.SPAWN_POSITION, spawnPoint).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} world spawn changed to {vector}", world.key().asString(), spawnPoint);
    }

    @Command(desc = "Sets view distance")
    public void viewDistance(CommandCause context, WorldTemplate world, int viewDistance)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.VIEW_DISTANCE, viewDistance).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} view distance changed to {number}", world.key().asString(), viewDistance);
    }

    @Command(desc = "Sets load on startup")
    public void autoload(CommandCause context, WorldTemplate world, boolean loadOnStartup)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.IS_LOAD_ON_STARTUP, loadOnStartup).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} load on startup changed to {name}", world.key().asString(), String.valueOf(loadOnStartup));
    }

    @Command(desc = "Sets spawnchunks loaded")
    public void spawnChunks(CommandCause context, WorldTemplate world, boolean loaded)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.PERFORM_SPAWN_LOGIC, loaded).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} spawn-chunks loaded changed to {name}", world.key().asString(), String.valueOf(loaded));
    }

    @Command(desc = "Sets hardcore mode")
    public void hardcore(CommandCause context, WorldTemplate world, boolean hardcore)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.HARDCORE, hardcore).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} hardcore mode changed to {name}", world.key().asString(), String.valueOf(hardcore));
    }

    @Command(desc = "Sets command usage")
    public void commands(CommandCause context, WorldTemplate world, boolean allowed)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.COMMANDS, allowed).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} command usage changed to {name}", world.key().asString(), String.valueOf(allowed));
    }

    @Command(desc = "Sets pvp")
    public void pvp(CommandCause context, WorldTemplate world, boolean pvp)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.PVP, pvp).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} pvp changed to {name}", world.key().asString(), String.valueOf(pvp));
    }

    @Command(desc = "Sets the seed")
    public void seed(CommandCause context, WorldTemplate world, String seed)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.SEED, (long) seed.hashCode()).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} seed changed to {name}", world.key().asString(), seed);
    }

    @Command(desc = "Sets the serialization behavior")
    public void serialize(CommandCause context, WorldTemplate world, SerializationBehavior serializationBehavior)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.SERIALIZATION_BEHAVIOR, serializationBehavior).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} serialization behavior changed to {name}", world.key().asString(), serializationBehavior.name());
    }

    @Command(desc = "Sets the difficulty")
    public void difficulty(CommandCause context, WorldTemplate world, Difficulty difficulty)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.WORLD_DIFFICULTY, difficulty).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} difficulty changed to {name}", world.key().asString(), difficulty.key(RegistryTypes.DIFFICULTY).asString());
    }

    @Command(desc = "Sets the default gamemode")
    public void gamemode(CommandCause context, WorldTemplate world, GameMode gameMode)
    {
        final WorldTemplate template = world.asBuilder().add(Keys.GAME_MODE, gameMode).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} gamemode changed to {name}", world.key().asString(), gameMode.key(RegistryTypes.GAME_MODE).asString());
    }

    @Command(desc = "Sets the world type and generator")
    public void worldtype(CommandCause context, WorldTemplate world, WorldType type, @Option String chunkGenerator)
    {
        ChunkGenerator generator = ChunkGenerator.overworld();
        if (type == WorldTypes.THE_NETHER.get())
        {
            generator = ChunkGenerator.theNether();
        }
        else if (type == WorldTypes.THE_END.get())
        {
            generator = ChunkGenerator.theEnd();
        }
        final WorldTemplate template = world.asBuilder().add(Keys.WORLD_TYPE, type).add(Keys.CHUNK_GENERATOR, generator).build();
        Sponge.server().dataPackManager().save(template);
        i18n.send(context, POSITIVE, "Template {name} worldtype changed to {name}", world.key().asString(), template.worldType().key(RegistryTypes.WORLD_TYPE).asString());
        if (chunkGenerator != null)
        {
            i18n.send(context, CRITICAL, "Custom ChunkGenerators not available yet!");
        }
    }
}
