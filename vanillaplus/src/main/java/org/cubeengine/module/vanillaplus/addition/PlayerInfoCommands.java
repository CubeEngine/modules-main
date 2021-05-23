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
package org.cubeengine.module.vanillaplus.addition;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import static java.text.DateFormat.SHORT;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;

@Singleton
public class PlayerInfoCommands
{
    private I18n i18n;

    @Inject
    public PlayerInfoCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandCause context, User player)
    {
        if (player.isOnline())
        {
            i18n.send(context, NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Instant lastPlayed = player.get(Keys.LAST_DATE_PLAYED).orElse(null);
        if (lastPlayed == null)
        {
            i18n.send(context, NEGATIVE, "User has not played here yet.");
            return;
        }
        final Locale locale = getLocale(context);
        if (System.currentTimeMillis() - lastPlayed.toEpochMilli() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            i18n.send(context, NEUTRAL, "{user} was last seen {input#date}.", player,
                      TimeUtil.format(locale, new Date(lastPlayed.toEpochMilli())));
            return;
        }
        Date date = new Date(lastPlayed.toEpochMilli());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, locale);
        i18n.send(context, NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
    }

    private Locale getLocale(CommandCause context)
    {
        if (context.subject() instanceof ServerPlayer)
        {
            return ((ServerPlayer)context.subject()).locale();
        }
        return Locale.getDefault();
    }

    @Command(desc = "Displays informations from a player!")
    public void whois(CommandCause context, User player)
    {
        if (player.isOnline())
        {
            i18n.send(context, NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            i18n.send(context, NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        final Locale locale = getLocale(context);
        if (player.get(Keys.FIRST_DATE_JOINED).isPresent() || player.isOnline())
        {
            i18n.send(context, NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.get(Keys.HEALTH).get(), player.get(Keys.MAX_HEALTH).get());
            i18n.send(context, NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.get(Keys.FOOD_LEVEL).get(), player.get(Keys.SATURATION).get().intValue(), player.get(Keys.FOOD_LEVEL).get());
            i18n.send(context, NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.get(Keys.EXPERIENCE_LEVEL).get(), player.get(Keys.EXPERIENCE_SINCE_LEVEL).get() * 100 / player.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).get());

            if (player.isOnline())
            {
                ServerLocation loc = player.player().get().serverLocation();
                i18n.send(context, NEUTRAL, "Position: {vector} in {world}", new Vector3i(loc.blockX(), loc.blockY(), loc.blockZ()), loc.world());
            }
            if (player.isOnline())
            {
                i18n.send(context, NEUTRAL, "IP: {input#ip}", player.player().get().connection().address().getAddress().getHostAddress());
            }
            Optional<GameMode> gameMode = player.get(Keys.GAME_MODE);
            if (gameMode.isPresent())
            {
                i18n.send(context, NEUTRAL, "Gamemode: {name#gamemode}", gameMode.get().asComponent());
            }
            if (player.get(Keys.CAN_FLY).orElse(false))
            {
                i18n.send(context, NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", player.get(Keys.IS_FLYING).orElse(false) ? "flying" : "not flying");
            }
            else
            {
                i18n.send(context, NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            /* TODO
            if (player.isOp())
            {
                i18n.sendTranslated(context, NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }*/

            if (!gameMode.isPresent() || !gameMode.get().equals(GameModes.CREATIVE.get()))
            {
                final Optional<Boolean> invulnerable = player.get(Keys.INVULNERABLE);
                final Optional<Ticks> ticksInvulnerable = player.get(Keys.INVULNERABILITY_TICKS);
                if (invulnerable.isPresent() || ticksInvulnerable.isPresent())
                {
                    i18n.send(context, NEUTRAL, "is invulnerable");
                }
            }
            /*
            if (player.get(BasicsAttachment.class).isAfk())
            {
                i18n.sendTranslated(context, NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            */
            String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
                                             .withZone(ZoneId.systemDefault())
                                             .format(player.get(Keys.FIRST_DATE_JOINED).get());
            i18n.send(context, NEUTRAL, "First played: {input#date}", format);
        }
        final BanService banService = Sponge.server().serviceProvider().banService();
        if (banService.banFor(player.profile()).join().isPresent())
        {
            final Ban.Profile ban = banService.banFor(player.profile()).join().get();
            Component expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, locale);

            if (!ban.expirationDate().isPresent())
            {
                expires = i18n.translate(context, "for ever");
            }
            else
            {
                expires = Component.text(format.format(ban.expirationDate().get()));
            }
            i18n.send(context, NEUTRAL, "Banned by {text#source} on {input#date}: {input#reason} ({input#expire})",
                                ban.banSource().orElse(Component.text("?")), format.format(ban.creationDate()),
                                ban.reason(), expires);
        }
    }
}
