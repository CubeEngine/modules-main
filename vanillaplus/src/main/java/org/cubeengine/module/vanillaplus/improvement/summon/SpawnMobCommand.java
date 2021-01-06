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
package org.cubeengine.module.vanillaplus.improvement.summon;

import java.util.Arrays;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * The /spawnmob command.
 */
@Singleton
public class SpawnMobCommand
{
    private VanillaPlus module;
    private I18n i18n;

    @Inject
    public SpawnMobCommand(VanillaPlus module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Spawns the specified Mob")
    public void spawnMob(ServerPlayer context, EntityType type, @Option Integer amount, @Option String data, @Option ServerPlayer at)  // TODO data completer and/or parser
    {
        ServerLocation loc = SpawnMob.getSpawnLoc(context, at, i18n);
        if (loc == null)
        {
            return;
        }
        loc = loc.add(0.5, 0, 0.5);
        final Entity entity = SpawnMob.createMob(context, type, Arrays.asList(StringUtils.explode(":", data == null ? "" : data)), loc, i18n);
        if (entity == null)
        {
            return;
        }
        SpawnMob.spawnMob(context, Arrays.asList(entity), amount, loc, i18n, module);
        i18n.send(context, POSITIVE, "Spawned {amount} {name#entity}!", amount == null ? 1 : amount, type.asComponent());
    }
}
