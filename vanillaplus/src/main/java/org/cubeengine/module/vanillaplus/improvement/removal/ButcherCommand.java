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
package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.Collection;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.ParameterRegistry;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.cause.entity.SpawnTypes;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Singleton
public class ButcherCommand extends PermissionContainer
{
    private VanillaPlus module;
    private I18n i18n;

    @Inject
    public ButcherCommand(PermissionManager pm, VanillaPlus module, I18n i18n, StringMatcher sm)
    {
        super(pm, VanillaPlus.class);
        this.module = module;
        this.i18n = i18n;
        ParameterRegistry.register(LivingFilter.class, new LivingFilterParser(pm, i18n, sm));
    }

    public final Permission COMMAND_BUTCHER_FLAG_LIGHTNING = register("command.butcher.lightning", "", null);
    public final Permission COMMAND_BUTCHER_FLAG_ALL = register("command.butcher.all", "", null);


    @Command(desc = "Gets rid of mobs close to you. Valid types are:\n" +
        "monster, animal, pet, golem, boss, other, creeper, skeleton, spider etc.")
    public void butcher(CommandCause context, @Label("types...") @Default LivingFilter types,
                        @Option Integer radius,
                        @Default @Named("in") ServerWorld world,
                        @Flag boolean lightning, // die with style
                        @Flag boolean all) // infinite radius
    {
        radius = radius == null ? module.getConfig().improve.commandButcherDefaultRadius : radius;
        if (radius < 0 && !(radius == -1 && context.hasPermission(COMMAND_BUTCHER_FLAG_ALL.getId())))
        {
            i18n.send(context, NEGATIVE, "The radius has to be a number greater than 0!");
            return;
        }
        if (all && context.hasPermission(COMMAND_BUTCHER_FLAG_ALL.getId()))
        {
            radius = -1;
        }
        lightning = lightning && context.hasPermission(COMMAND_BUTCHER_FLAG_LIGHTNING.getId());

        Collection<? extends Entity> remove;
        if (context.getSubject() instanceof ServerPlayer && ((ServerPlayer)context.getSubject()).getWorld() == world) {
            remove = ((ServerPlayer)context.getSubject()).getNearbyEntities(radius, types);
        } else {
            final Vector3i spawn = world.getProperties().getSpawnPosition();
            remove = world.getEntities(AABB.of(spawn.sub(Vector3i.ONE.mul(radius)), spawn.sub(Vector3i.ONE.mul(-radius))), types);
        }

        Sponge.getServer().getCauseStackManager().pushCause(context).addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLUGIN);
        for (Entity entity : remove)
        {
            if (lightning)
            {
                world.spawnEntity(world.createEntity(EntityTypes.LIGHTNING_BOLT, entity.getLocation().getPosition()));
            }
            entity.remove();
        }

        if (remove.size() == 0)
        {
            i18n.send(context, NEUTRAL, "Nothing to butcher!");
        }
        else
        {
            i18n.send(context, POSITIVE, "You just slaughtered {amount} living entities!", remove.size());
        }

    }
}
