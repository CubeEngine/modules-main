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
package org.cubeengine.module.travel.home;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.HomeConfig;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.event.Order.EARLY;

@Singleton
public class HomeManager
{
    private Travel module;
    private I18n i18n;
    private HomeConfig config;

    @Inject
    public HomeManager(EventManager em, PluginContainer plugin, Travel module, I18n i18n, Reflector reflector, ModuleManager mm)
    {
        this.config = reflector.load(HomeConfig.class, mm.getPathFor(Travel.class).resolve("homes.yml").toFile());
        this.module = module;
        this.i18n = i18n;
        em.registerListeners(plugin, this);
    }

    public Home create(User owner, String name, ServerWorld world, Transform transform)
    {
        if (this.has(owner.getUniqueId(), name))
        {
            throw new IllegalArgumentException("Tried to create duplicate home!");
        }

        Home home = new Home();
        home.name = name;
        home.owner = owner.getUniqueId();
        home.transform = transform;
        home.world = new ConfigWorld(world);

        config.homes.add(home);
        config.save();
        return home;
    }

    public void delete(Home home)
    {
        config.homes.remove(home);
        config.save();
    }

    public boolean has(UUID user, String name)
    {
        return get(user, name).isPresent();
    }

    public Optional<Home> get(UUID user, String name)
    {
        return config.homes.stream().filter(home -> home.owner.equals(user) && home.name.equals(name)).findFirst();
    }

    public Optional<Home> find(ServerPlayer player, String name, @Nullable User owner)
    {
        owner = owner == null ? player.getUser() : owner; // No owner specified?
        Optional<Home> home = get(owner.getUniqueId(), name); // Get home by name and owner
        if (!home.isPresent() && owner.getUniqueId().equals(player.getUniqueId())) // Not found and not looking for a specific home
        {
            List<UUID> globalInvites = config.globalInvites.getOrDefault(player.getUniqueId(), Collections.emptyList());
            // Go look in invites...
            home = config.homes.stream().filter(h -> (h.invites.contains(player.getUniqueId()) || globalInvites.contains(h.owner)) && h.name.equals(name)).findFirst();
        }
        return home;
    }


    public long getCount(Player player)
    {
        return config.homes.stream().filter(h -> h.owner.equals(player.getUniqueId())).count();
    }

    public void save()
    {
        config.save();
    }

    public boolean rename(Home point, String name)
    {
        if (has(point.getOwner().getUniqueId(), name))
        {
            return false;
        }

        point.name = name;
        save();
        return true;
    }

    public Set<Home> list(User user, boolean owned, boolean invited)
    {
        List<UUID> globalInvites = config.globalInvites.getOrDefault(user.getUniqueId(), Collections.emptyList());
        return config.homes.stream()
           .filter(home -> (owned || !invited) && home.owner.equals(user.getUniqueId())
                         || invited && home.invites.contains(user.getUniqueId())
                         || invited && globalInvites.contains(home.owner))
           .collect(Collectors.toSet());
    }

    public void massDelete(Predicate<Home> predicate)
    {
        config.homes = config.homes.stream().filter(predicate).collect(Collectors.toList());
        save();
    }


    @Listener(order = EARLY)
    public void rightClickBed(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        if (!player.hasPermission("cubeengine.travel.command.home.set.use"))
        {
            return;
        }

        if (!event.getBlock().getState().getType().isAnyOf(BlockTypes.BLACK_BED, BlockTypes.BLUE_BED, BlockTypes.BROWN_BED, BlockTypes.CYAN_BED, BlockTypes.GRAY_BED,
                                                           BlockTypes.GREEN_BED, BlockTypes.LIGHT_BLUE_BED, BlockTypes.LIGHT_GRAY_BED, BlockTypes.LIME_BED, BlockTypes.MAGENTA_BED,
                                                           BlockTypes.ORANGE_BED, BlockTypes.PINK_BED, BlockTypes.PURPLE_BED, BlockTypes.RED_BED, BlockTypes.WHITE_BED, BlockTypes.YELLOW_BED)
            || !player.get(Keys.IS_SNEAKING).orElse(false)
            || !player.getItemInHand(HandTypes.MAIN_HAND).isEmpty())
        {
            return;
        }
        Optional<Home> home = this.get(player.getUniqueId(), "home");
        if (home.isPresent())
        {
            home.get().setTransform(player.getWorld(), player.getTransform());
            config.save();
        }
        else if (getCount(player) >= module.getConfig().homes.max)
        {
            i18n.send(player, CRITICAL, "You have reached your maximum number of homes!");
            i18n.send(player, NEGATIVE, "You have to delete a home to make a new one");
            return;
        }
        else
        {
            this.create(player.getUser(), "home", player.getWorld(), player.getTransform());
            i18n.send(player, POSITIVE, "Your home has been created!");
        }
        // event.setCancelled(true);
    }

    public void purge(ServerWorld world)
    {
        this.config.homes.removeIf(home -> home.world.getWorld().getKey().equals(world.getKey()));
        this.config.save();
    }
}
