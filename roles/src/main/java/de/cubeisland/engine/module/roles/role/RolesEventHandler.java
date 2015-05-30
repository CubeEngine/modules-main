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
package de.cubeisland.engine.module.roles.role;

import de.cubeisland.engine.module.service.permission.NotifyPermissionRegistrationCompletedEvent;
import de.cubeisland.engine.module.service.user.UserAuthorizedEvent;
import de.cubeisland.engine.module.roles.Roles;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChangeWorldEvent;

public class RolesEventHandler
{
    private final Roles module;

    public RolesEventHandler(Roles module)
    {
        this.module = module;
    }

    @Subscribe
    public void onPlayerChangedWorld(PlayerChangeWorldEvent event)
    {
        /*
        // TODO this wasn't completly right (we can only not recalculate if all data is shared)
        WorldRoleProvider fromProvider = this.rolesManager.getProvider(event.getFrom());
        WorldRoleProvider toProvider = this.rolesManager.getProvider(event.getPlayer().getWorld());
        if (fromProvider.equals(toProvider))
        {
            if (toProvider.getWorldMirrors().get(event.getPlayer().getWorld()).getSecond()
            && fromProvider.getWorldMirrors().get(event.getFrom()).getSecond())
            {
                return;
            }
        }
        */

        RolesAttachment attachment = this.rolesManager.getRolesAttachment(event.getPlayer());
        attachment.flushData(event.getFrom());
        attachment.getCurrentDataHolder().apply();
    }

    @Subscribe(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event)
    {
        if (event.getResult().equals(Result.ALLOWED)) // only if allowed to join
        {
            this.rolesManager.getRolesAttachment(event.getPlayer()).getCurrentDataHolder(); // Pre-calculate
        }
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event)
    {
        this.rolesManager.getRolesAttachment(event.getPlayer()).getCurrentDataHolder().apply(); // Pre-calculate + apply
    }

    @Subscribe
    public void onAuthorized(UserAuthorizedEvent event)
    {
        RolesAttachment rolesAttachment = this.rolesManager.getRolesAttachment(event.getUser());
        rolesAttachment.flushData();
        rolesAttachment.getCurrentDataHolder().apply(); // Pre-Calculate + apply
    }

    @Subscribe
    public void onPermissionRegister(NotifyPermissionRegistrationCompletedEvent event)
    {
        this.module.getLog().debug("{} registered new Permissions. Reloading Roles...", event.getModule().getName());
        this.rolesManager.initRoleProviders();
        this.rolesManager.recalculateAllRoles();
    }
}
