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
package org.cubeengine.module.travel.home;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.cubeengine.module.travel.Travel;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.HomeConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.config.WorldTransform;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.mutable.entity.SneakingData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.event.Order.EARLY;

public class HomeManager
{
    private Travel module;
    private I18n i18n;
    private HomeConfig config;

    public HomeManager(Travel module, I18n i18n, HomeConfig config)
    {
        this.module = module;
        this.i18n = i18n;
        this.config = config;
    }

    public Home create(Player owner, String name, Transform<World> transform)
    {
        if (this.has(owner, name))
        {
            throw new IllegalArgumentException("Tried to create duplicate home!");
        }

        Home home = new Home();
        home.name = name;
        home.owner = owner.getUniqueId();
        home.transform = new WorldTransform(transform.getLocation(), transform.getRotation());
        home.world = new ConfigWorld(transform.getExtent());

        config.homes.add(home);
        config.save();
        return home;
    }

    public void delete(Home home)
    {
        config.homes.remove(home);
        config.save();
    }

    public boolean has(User user, String name)
    {
        return get(user, name).isPresent();
    }

    public Optional<Home> get(User user, String name)
    {
        return config.homes.stream().filter(home -> home.owner.equals(user.getUniqueId()) && home.name.equals(name)).findFirst();
    }

    public Optional<Home> find(Player player, String name, @Nullable User owner)
    {
        owner = owner == null ? player : owner; // No owner specified?
        Optional<Home> home = get(owner, name); // Get home by name and owner
        if (!home.isPresent() && owner == player) // Not found and not looking for a specific home
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
        if (has(point.getOwner(), name))
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
    public void rightClickBed(InteractBlockEvent.Secondary event, @First Player player)
    {
        if (event.getTargetBlock().getState().getType() != BlockTypes.BED
            || !player.get(SneakingData.class).isPresent()
            || player.getItemInHand(HandTypes.MAIN_HAND).isPresent())
        {
            return;
        }
        Optional<Home> home = this.get(player, "home");
        if (home.isPresent())
        {
            home.get().setTransform(player.getTransform());
            config.save();
        }
        else if (getCount(player) >= module.getConfig().homes.max)
        {
            i18n.sendTranslated(player, CRITICAL, "You have reached your maximum number of homes!");
            i18n.sendTranslated(player, NEGATIVE, "You have to delete a home to make a new one");
            return;
        }
        else
        {
            this.create(player, "home", player.getTransform());
            i18n.sendTranslated(player, POSITIVE, "Your home has been created!");
        }
        event.setCancelled(true);
    }

}
