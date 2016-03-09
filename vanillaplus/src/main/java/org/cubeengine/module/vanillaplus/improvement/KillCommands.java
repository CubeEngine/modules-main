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
package org.cubeengine.module.vanillaplus.improvement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.readers.UserListInSight;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionContainer;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.service.permission.PermissionDescription;

import static org.cubeengine.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.entity.EntityTypes.LIGHTNING;
import static org.spongepowered.api.event.cause.NamedCause.source;
import static org.spongepowered.api.event.cause.entity.damage.DamageTypes.CUSTOM;

/**
 * {@link #kill}
 * {@link #suicide}
 */
public class KillCommands extends PermissionContainer<VanillaPlus>
{
    private I18n i18n;

    public KillCommands(VanillaPlus module, I18n i18n)
    {
        super(module);
        this.i18n = i18n;
    }

    private final PermissionDescription COMMAND_KILL = register("command.kill", "", null);
    public final PermissionDescription COMMAND_KILL_PREVENT = register("prevent", "Prevents from being killed by the kill command unless forced", COMMAND_KILL);
    public final PermissionDescription COMMAND_KILL_FORCE = register("force", "Kills a player even if the player has the prevent PermissionDescription", COMMAND_KILL);
    public final PermissionDescription COMMAND_KILL_ALL = register("all", "Allows killing all players currently online", COMMAND_KILL);
    public final PermissionDescription COMMAND_KILL_LIGHTNING = register("lightning", "Allows killing a player with a lightning strike", COMMAND_KILL);
    public final PermissionDescription COMMAND_KILL_QUIET = register("quiet", "Prevents the other player being notified who killed him", COMMAND_KILL);
    public final PermissionDescription COMMAND_KILL_NOTIFY = register("notify", "Shows who killed you", COMMAND_KILL);

    @Command(alias = "slay", desc = "Kills a player")
    public void kill(CommandSource context, @Default(UserListInSight.class) UserList players,
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
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to kill everyone!");
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
            i18n.sendTranslated(context, NEUTRAL, "No one was killed!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "You killed {user#list}!", StringUtils.implode(",", killed));
    }


    private boolean kill(Player player, boolean lightning, CommandSource context, boolean showMessage, boolean force, boolean quiet)
    {
        if (!force)
        {
            if (player.hasPermission(COMMAND_KILL_PREVENT.getId()) || player.get(Keys.INVULNERABILITY_TICKS).isPresent())
            {
                i18n.sendTranslated(context, NEGATIVE, "You cannot kill {user}!", player);
                return false;
            }
        }
        if (lightning)
        {
            player.getWorld().spawnEntity(player.getWorld().createEntity(LIGHTNING, player.getLocation().getPosition()).get(), Cause.of(source(context)));
        }

        player.damage(player.getHealthData().maxHealth().get(), DamageSource.builder().absolute().type(CUSTOM).build(), Cause.of(source(context)));
        if (showMessage)
        {
            i18n.sendTranslated(context, POSITIVE, "You killed {user}!", player);
        }
        if (!quiet && player.hasPermission(COMMAND_KILL_NOTIFY.getId()))
        {
            i18n.sendTranslated(player, NEUTRAL, "You were killed by {user}", context);
        }
        return true;
    }


    @Command(desc = "Kills yourself")
    @Restricted(value = Player.class, msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}")
    public void suicide(Player context)
    {
        context.damage(context.getHealthData().maxHealth().get(), DamageSource.builder().absolute().type(CUSTOM).build(), Cause.of(source(context)));
        i18n.sendTranslated(context, NEGATIVE, "You ended your life. Why? {text:\\:(:color=DARK_RED}");
    }
}
