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
package org.cubeengine.module.worldcontrol;

import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.module.core.util.WorldLocation;
import org.cubeengine.module.worlds.Worlds;
import org.cubeengine.service.world.ConfigWorld;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.difficulty.Difficulty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

@SuppressWarnings("all")
public class WorldConfig extends ReflectedYaml
{
    //TODO perms
    // public final PermissionDescription KEEP_GAMEMODE = register("keep-gamemode", "Keeps the gamemode in between worlds", null);
    //public final PermissionDescription KEEP_FLYMODE = register("keep-flymode", "Keeps the flymode in between worlds", null);

    @Comment("The scale of this world (used for netherportals by default NetherWorlds are at 8.0 others at 1.0)")
    public double scale = 1.0;

    public Spawn spawn = new Spawn();

    public class Spawn implements Section
    {
        @Comment("The world a player will respawn in when dying and not having a bedspawn set\n" +
                     "Empty means main world of this universe")
        public ConfigWorld respawnWorld; // empty means main universe world
        @Comment("If false sleeping in a bed will not set a players spawn. Not implemented yet")
        public boolean allowBedRespawn = true;
        @Comment("This worlds spawn")
        public WorldLocation spawnLocation; // TODO rotation
    }
    public Access access = new Access();

    public class Access implements Section
    {
        @Comment("If true players wont need permission to access this world")
        public boolean free = true;
    }

    @Comment("Mob Spawing Settings of this world")
    public Spawning spawning = new Spawning();

    public class Spawning implements Section
    {
        @Comment("If true completely disable animal spawning")
        public boolean disable_animals = false;
        @Comment("If true completely disable monster spawning")
        public boolean disable_monster = false;  // ^ world.setSpawnFlags();

        @Comment("SpawnLimits for ambient mobs per chunk (default: 15)")
        public Integer spawnLimit_ambient = 15;
        @Comment("SpawnLimits for animals per chunk (default: 15)")
        public Integer spawnLimit_animal = 15;
        @Comment("SpawnLimits for monster per chunk (default: 70)")
        public Integer spawnLimit_monster = 70;
        @Comment("SpawnLimits for water animals per chunk (default: 5)")
        public Integer spawnLimit_waterAnimal = 5;

        @Comment("SpawnRates for Animals (default: 400 ticks)")
        public Integer spawnRate_animal = 400;
        @Comment("SpawnRates for Monster (default: 1 ticks)")
        public Integer spawnRate_monster = 1;
    }

    @Comment("If false PvP is disabled in this world")
    public boolean pvp = true;

    @Comment("The world where NetherPortals will lead to. (This won't work in an end world)")
    public String netherTarget;
    @Comment("The world where EndPortals will lead to. (This won't work in a nether world)")
    public String endTarget;

    public void applyToWorld(World world) // TODO how can this be implemented in sponge?
    {// TODO if anything is null take from world ; update inheritance & save
        /*
        boolean save = false;

        world.setSpawnFlags(!this.spawning.disable_monster, !this.spawning.disable_animals);
        world.setAmbientSpawnLimit(this.spawning.spawnLimit_ambient);
        world.setAnimalSpawnLimit(this.spawning.spawnLimit_animal);
        world.setMonsterSpawnLimit(this.spawning.spawnLimit_monster);
        world.setWaterAnimalSpawnLimit(this.spawning.spawnLimit_waterAnimal);
        world.getWorldStorage().getWorldProperties().setWorldBorderWarningTime();
        world.setTicksPerAnimalSpawns(this.spawning.spawnRate_animal);
        world.setTicksPerMonsterSpawns(this.spawning.spawnRate_monster);

        world.setPVP(this.pvp);
        world.setAutoSave(this.autosave);

        if (save)
        {
            this.updateInheritance();
            this.save();
        }
        */
    }
}
