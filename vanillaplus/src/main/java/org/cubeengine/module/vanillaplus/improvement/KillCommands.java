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
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

/**
 * {@link #kill}
 * {@link #suicide}
 */
public class KillCommands
{
    private VanillaPlus module;
    private I18n i18n;

    public KillCommands(VanillaPlus module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }


    @Command(alias = "slay", desc = "Kills a player")
    public void kill(CommandSource context, UserList players, // TODO default line of sight player
                     @Flag boolean force, @Flag boolean quiet, @Flag boolean lightning)
    {
        lightning = lightning && context.hasPermission(module.perms().COMMAND_KILL_LIGHTNING.getId());
        force = force && context.hasPermission(module.perms().COMMAND_KILL_FORCE.getId());
        quiet = quiet && context.hasPermission(module.perms().COMMAND_KILL_QUIET.getId());
        List<Text> killed = new ArrayList<>();
        Collection<Player> userList = players.list();
        if (players.isAll())
        {
            if (!context.hasPermission(module.perms().COMMAND_KILL_ALL.getId()))
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
            if (player.hasPermission(module.perms().COMMAND_KILL_PREVENT.getId())) // TODO also check "creative/godmode"
            {

                i18n.sendTranslated(context, NEGATIVE, "You cannot kill {user}!", player);
                return false;
            }
        }
        if (lightning)
        {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }
        player.setHealth(0);
        if (showMessage)
        {
            i18n.sendTranslated(context, POSITIVE, "You killed {user}!", player);
        }
        if (!quiet && player.hasPermission(module.perms().COMMAND_KILL_NOTIFY.getId()))
        {
            i18n.sendTranslated(player, NEUTRAL, "You were killed by {user}", context);
        }
        return true;
    }


    @Command(desc = "Kills yourself")
    @Restricted(value = Player.class, msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}") // TODO replace User.class /w interface that has life stuff?
    public void suicide(Player context)
    {
        context.setHealth(0);

        // TODO context.setLastDamageCause(new EntityDamageEvent(context, CUSTOM, context.getMaxHealth()));
        // maybe DamageableData
        i18n.sendTranslated(context, NEGATIVE, "You ended your life. Why? {text:\\:(:color=DARK_RED}");
    }


}
