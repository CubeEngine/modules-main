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

import java.util.HashMap;
import java.util.Map;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;

@SuppressWarnings("all")
public class WorldControlConfig extends ReflectedYaml
{
    public Map<ConfigWorld, WorldSection> worldSettings = new HashMap<>();

    public static class WorldSection implements Section
    {
        @Comment("If false PvP is disabled in this world")
        public boolean pvp = true;

        @Comment("Mob Spawing Settings of this world")
        public Spawning spawning = new Spawning();

        public class Spawning implements Section
        {
            @Comment("If true completely disable animal spawning")
            public boolean disable_animals = false;
            @Comment("If true completely disable monster spawning")
            public boolean disable_monster = false;  // ^ world.setSpawnFlags();
        }

        @Comment({"The gamemode to enforce in this world\n" +
                  "grant cubeengine.worldcontrol.ignoreEnforceGamemode permission to ignore this"})
        public GameMode enforceGameMode = null;

        @Comment("If false sleeping in a bed will not set a players spawn.")
        public boolean allowBedRespawn = true;

    }

/* Stuffs from WorldGuard:
physics:
    no-physics-gravel: false
    no-physics-sand: false
ignition:
    block-tnt: false
    block-tnt-block-damage: true
    block-lighter: false
fire:
    disable-lava-fire-spread: true
    disable-all-fire-spread: true
mobs:
    block-creeper-explosions: false
    block-creeper-block-damage: true
    block-wither-explosions: false
    block-wither-block-damage: true
    block-wither-skull-explosions: true
    block-wither-skull-block-damage: true
    block-enderdragon-block-damage: false
    block-enderdragon-portal-creation: false
    block-fireball-explosions: false
    block-fireball-block-damage: true
    anti-wolf-dumbness: false // preventing running into fire/lava?
    disable-enderman-griefing: true

    block-painting-destroy: true
    block-item-frame-destroy: true
    block-above-ground-slimes: false
    block-other-explosions: false
    block-zombie-door-destruction: false
    disable-snowman-trails: false
player-damage:
    disable-fall-damage: false
    disable-lava-damage: false
    disable-fire-damage: false
    disable-lightning-damage: false
    disable-drowning-damage: false
    disable-suffocation-damage: false
    disable-contact-damage: false
    disable-void-damage: false
    // TODO void tp to another world?
    disable-explosion-damage: false
    disable-mob-damage: false
crops:
    disable-creature-trampling: true
    disable-player-trampling: true
weather:
    disable-lightning-strike-fire: true
    disable-thunderstorm: false
    disable-weather: false
    disable-pig-zombification: false
    disable-powered-creepers: false
    always-raining: false
    always-thundering: false
dynamics:
    disable-mushroom-spread: false
    disable-ice-melting: false
    disable-snow-melting: false
    disable-snow-formation: false
    disable-ice-formation: false
    disable-leaf-decay: false
    disable-grass-growth: false
    disable-mycelium-spread: false
    disable-vine-growth: false
    disable-soil-dehydration: false
    snow-fall-blocks: []
*/
}
