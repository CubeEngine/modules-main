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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;
import static org.spongepowered.api.entity.EntityTypes.LIGHTNING_BOLT;
import static org.spongepowered.api.event.cause.entity.damage.DamageTypes.CUSTOM;

/**
 * {@link #kill0}
 * {@link #suicide}
 */
@Singleton
public class KillCommands extends PermissionContainer
{
    private I18n i18n;

    @Inject
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
    public void kill(CommandCause context, Collection<ServerPlayer> players,
                     @Flag boolean force, @Flag boolean quiet, @Flag boolean lightning)
    {
        lightning = lightning && context.hasPermission(COMMAND_KILL_LIGHTNING.getId());
        force = force && context.hasPermission(COMMAND_KILL_FORCE.getId());
        quiet = quiet && context.hasPermission(COMMAND_KILL_QUIET.getId());
        List<String> killed = new ArrayList<>();
        final boolean all = Sponge.server().onlinePlayers().containsAll(players);
        if (all)
        {
            if (!context.hasPermission(COMMAND_KILL_ALL.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to kill everyone!");
                return;
            }
            if (context.subject() instanceof ServerPlayer)
            {
                players.remove(context.subject());
            }
        }
        for (ServerPlayer user : players)
        {
            if (this.kill0(user, lightning, context, false, force, quiet))
            {
                killed.add(user.name());
            }
        }
        if (killed.isEmpty())
        {
            i18n.send(context, NEUTRAL, "No one was killed!");
            return;
        }
        i18n.send(context, POSITIVE, "You killed {user#list}!", StringUtils.implode(",", killed));
    }


    private boolean kill0(ServerPlayer player, boolean lightning, CommandCause context, boolean showMessage, boolean force, boolean quiet)
    {
        if (!force)
        {

            if (COMMAND_KILL_PREVENT.check(player) || player.get(Keys.INVULNERABILITY_TICKS).isPresent())
            {
                i18n.send(context, NEGATIVE, "You cannot kill {user}!", player);
                return false;
            }
        }
        Sponge.server().causeStackManager().pushCause(context);
        if (lightning)
        {
            player.world().spawnEntity(player.world().createEntity(LIGHTNING_BOLT, player.location().position()));
        }


        player.damage(player.get(Keys.MAX_HEALTH).get(), DamageSource.builder().absolute().type(CUSTOM).build());

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
    @Restricted(msg = "You want to kill yourself? {text:The command for that is stop!:color=BRIGHT_GREEN}")
    public void suicide(ServerPlayer context)
    {
        Sponge.server().causeStackManager().pushCause(context);
        context.offer(Keys.HEALTH, 0d);
        i18n.send(context, NEGATIVE, "You ended your life. Why? {text::(:color=DARK_RED}");
    }
}
