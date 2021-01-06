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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * The /spawnmob command.
 */
@Command(name = "mobstack", desc = "Spawning stacks of mobs")
@Singleton
public class SpawnMobStackCommand extends DispatcherCommand
{
    private VanillaPlus module;
    private I18n i18n;
    private Map<UUID, List<Entity>> mobStacks = new HashMap<>();

    @Inject
    public SpawnMobStackCommand(VanillaPlus module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Builds mobstack")
    public void build(ServerPlayer context, EntityType type, @Option String data) // TODO data completer and/or parser
    {
        final Entity entity = SpawnMob.createMob(context, type, Arrays.asList(StringUtils.explode(":", data == null ? "" : data)), context.getServerLocation(), i18n);
        final List<Entity> mobStack = mobStacks.computeIfAbsent(context.getUniqueId(), k -> new ArrayList<>());
        mobStack.add(entity);
        i18n.send(ChatType.ACTION_BAR, context, POSITIVE, "Added {name} to your mobstack.", type.asComponent());
    }

    @Command(desc = "Clears the mobstack")
    public void clear(ServerPlayer context)
    {
        mobStacks.remove(context.getUniqueId());
        i18n.send(ChatType.ACTION_BAR, context, POSITIVE, "Cleared your mobstack.");
    }

    @Command(desc = "Spawns a mobstack")
    public void spawn(ServerPlayer context, @Option ServerPlayer at)
    {
        ServerLocation loc = SpawnMob.getSpawnLoc(context, at, i18n);
        if (loc == null)
        {
            return;
        }
        final List<Entity> stack = this.mobStacks.get(context.getUniqueId());
        if (stack.isEmpty())
        {
            i18n.send(context, NEGATIVE, "Your mobstack is empty.");
        }

        Component message = stack.get(0).getType().asComponent();
        for (int i = 1; i < stack.size(); i++)
        {
            message = i18n.translate(context, "{input#entity} riding {input}", stack.get(i).getType().asComponent(), message);
        }

        loc = loc.add(0.5, 0, 0.5);
        SpawnMob.spawnMob(context, stack, 1, loc, i18n, module);
        i18n.send(context, POSITIVE, "Spawned {input#message}!", message);
    }


}
