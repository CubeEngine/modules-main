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
package org.cubeengine.module.protector.listener;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.protector.Protector;
import org.cubeengine.module.protector.RegionManager;
import org.cubeengine.module.protector.region.Region;
import org.cubeengine.module.protector.region.RegionConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.util.Tristate;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.CRITICAL;
import static org.spongepowered.api.util.Tristate.*;

@Singleton
public class SettingsListener extends PermissionContainer
{

    private RegionManager manager;
    private I18n i18n;

    public final Permission command;

    @Inject
    public SettingsListener(RegionManager manager, PermissionManager pm, I18n i18n)
    {
        super(pm, Protector.class);
        this.manager = manager;
        this.i18n = i18n;
        this.command = this.register("bypass.command", "Region bypass for using all commands");
    }


    public static Tristate checkSetting(Cancellable event, ServerPlayer player, List<Region> regionsAt, Supplier<Permission> perm, Function<RegionConfig.Settings, Tristate> func, Tristate defaultTo)
    {
        Permission permission = perm.get();
        if (player != null && permission != null && permission.check(player))
        {
            event.setCancelled(false);
            return Tristate.TRUE;
        }
        Tristate allow = UNDEFINED;
        for (Region region : regionsAt)
        {
            allow = allow.and(func.apply(region.getSettings()));
            if (allow != UNDEFINED)
            {
                if (allow == FALSE)
                {
                    event.setCancelled(true);
                    return FALSE;
                }
                break;
            }
        }
        if (allow == TRUE)
        {
            event.setCancelled(false);
        }
        if (allow == UNDEFINED)
        {
            return defaultTo;
        }
        return allow;
    }


    @Listener(order = Order.EARLY)
    public void onCommand(ExecuteCommandEvent.Pre event, @Root ServerPlayer player)
    {
        CommandMapping mapping = Sponge.getGame().getCommandManager().getCommandMapping(event.getCommand()).orElse(null);
        if (mapping == null)
        {
            return;
        }
        List<Region> regionsAt = manager.getRegionsAt(player.getServerLocation());
        if (checkSetting(event, player, regionsAt, () -> command, s -> {

            Tristate value = UNDEFINED;
            // TODO subcommands?
            // TODO register commands as aliascommand
            for (String alias : mapping.getAllAliases())
            {
                value = value.and(s.blockedCommands.getOrDefault(alias.toLowerCase(), UNDEFINED));
                if (value != UNDEFINED)
                {
                    break;
                }
            }
            return value;
        }, UNDEFINED) == FALSE)
        {
            i18n.send(ChatType.ACTION_BAR, player, CRITICAL, "You are not allowed to execute this command here!");
        }
    }

}