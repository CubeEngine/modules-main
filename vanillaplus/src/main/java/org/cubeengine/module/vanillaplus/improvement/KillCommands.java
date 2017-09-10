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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.CauseUtil;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.command.parser.UserListInSight;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.command.parser.PlayerList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.entity.EntityTypes.LIGHTNING;
import static org.spongepowered.api.event.cause.NamedCause.source;
import static org.spongepowered.api.event.cause.entity.damage.DamageTypes.CUSTOM;

/**
 * {@link #kill}
 * {@link #suicide}
 */
public class KillCommands extends PermissionContainer
{
    private I18n i18n;

    public KillCommands(PermissionManager pm, I18n i18n)
    {
        super(pm, VanillaPlus.class);
        this.i18n = i18n;
    }

    public final Permission COMMAND_KILL_PREVENT = register("command.kill.prevent", "Prevents from being killed by the kill command unless forced", null);
    public final Permission COMMAND_KILL_FORCE = register("command.kill.force", "Kills a player even if the player has the prevent PermissionDescription", null);
    public final Permission COMMAND_KILL_ALL = register("command.kill.all", "Allows killing all players currently online", null);
    public final Permission COMMAND_KILL_LIGHTNING = register("command.kill.lightning", "Allows killing a player with a lightning strike", null);
    public final Permission COMMAND_KILL_QUIET = register("command.kill.quiet", "Prevents the other player being notified who killed him", null);
    public final Permission COMMAND_KILL_NOTIFY = register("command.kill.notify", "Shows who killed you", null);

    @Command(alias = "slay", desc = "Kills a player")
    public void kill(CommandSource context, @Default(UserListInSight.class) PlayerList players,
                     @Flag boolean force, @Flag boolean quiet, @Flag boolean lightning)
    {
        lightning = lightning && context.hasPermission(COMMAND_KILL_LIGHTNING.getId());
        force = force && context.hasPermission(COMMAND_KILL_FORCE.getId());
        quiet = quiet && context.hasPermission(COMMAND_KILL_QUIET.getId());
        List<String> killed = new ArrayList<>();
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (!context.hasPermission(COMMAND_KILL_ALL.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to kill everyone!");
                return;
            }
            if (context instanceof Player)
            {
                userList.remove(context);
            }
        }
        for (Player user : userList)
        {
            if (this.kill(user, lightning, context, false, force, quiet))
            {
                killed.add(user.getName());
            }
        }
        if (killed.isEmpty())
        {
            i18n.send(context, NEUTRAL, "No one was killed!");
            return;
        }
        i18n.send(context, POSITIVE, "You killed {user#list}!", StringUtils.implode(",", killed));
    }


    private boolean kill(Player player, boolean lightning, CommandSource context, boolean showMessage, boolean force, boolean quiet)
    {
        if (!force)
        {
            if (player.hasPermission(COMMAND_KILL_PREVENT.getId()) || player.get(Keys.INVULNERABILITY_TICKS).isPresent())
            {
                i18n.send(context, NEGATIVE, "You cannot kill {user}!", player);
                return false;
            }
        }
        Sponge.getCauseStackManager().pushCause(context);
        if (lightning)
        {
            player.getWorld().spawnEntity(player.getWorld().createEntity(LIGHTNING, player.getLocation().getPosition()));
        }

        player.damage(player.getHealthData().maxHealth().get(), DamageSource.builder().absolute().type(CUSTOM).build());

        if (force)
        {
            player.offer(Keys.HEALTH, 0d);
        }
        if (showMessage)
        {
            i18n.send(context, POSITIVE, "You killed {user}!", player);
        }
        if (!quiet && player.hasPermission(COMMAND_KILL_NOTIFY.getId()))
        {
            i18n.send(player, NEUTRAL, "You were killed by {user}", context);
        }
        return true;
    }


    @Command(desc = "Kills yourself")
    @Restricted(value = Player.class, msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}")
    public void suicide(Player context)
    {
        Sponge.getCauseStackManager().pushCause(context);
        context.damage(context.getHealthData().maxHealth().get(), DamageSource.builder().absolute().type(CUSTOM).build());
        context.offer(Keys.HEALTH, 0d);
        i18n.send(context, NEGATIVE, "You ended your life. Why? {text::(:color=DARK_RED}");
    }
}
