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

import static java.text.DateFormat.SHORT;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.InvulnerabilityData;
import org.spongepowered.api.data.manipulator.mutable.entity.JoinData;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban.Profile;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Optional;

public class PlayerInfoCommands
{
    private I18n i18n;

    public PlayerInfoCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandSource context, User player)
    {
        if (player.isOnline())
        {
            i18n.send(context, NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Instant lastPlayed = player.get(Keys.FIRST_DATE_PLAYED).orElse(null);
        if (lastPlayed == null)
        {
            i18n.send(context, NEGATIVE, "User has not played here yet.");
            return;
        }
        if (System.currentTimeMillis() - lastPlayed.toEpochMilli() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            i18n.send(context, NEUTRAL, "{user} was last seen {input#date}.", player, TimeUtil.format(
                context.getLocale(), new Date(lastPlayed.toEpochMilli())));
            return;
        }
        Date date = new Date(lastPlayed.toEpochMilli());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
        i18n.send(context, NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
    }

    @Command(desc = "Displays informations from a player!")
    public void whois(CommandSource context, User player)
    {
        if (player.isOnline())
        {
            i18n.send(context, NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            i18n.send(context, NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        if (player.get(JoinData.class).isPresent() || player.isOnline())
        {
            i18n.send(context, NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.get(Keys.HEALTH).get(), player.get(Keys.MAX_HEALTH).get());
            i18n.send(context, NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.get(Keys.FOOD_LEVEL).get(), player.get(Keys.SATURATION).get().intValue(), player.get(Keys.FOOD_LEVEL).get());
            i18n.send(context, NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.get(Keys.EXPERIENCE_LEVEL).get(), player.get(Keys.EXPERIENCE_SINCE_LEVEL).get() * 100 / player.get(Keys.EXPERIENCE_FROM_START_OF_LEVEL).get());

            if (player.isOnline())
            {
                Location<World> loc = player.getPlayer().get().getLocation();
                i18n.send(context, NEUTRAL, "Position: {vector} in {world}", new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
            }
            if (player.isOnline())
            {
                i18n.send(context, NEUTRAL, "IP: {input#ip}", player.getPlayer().get().getConnection().getAddress().getAddress().getHostAddress());
            }
            Optional<GameMode> gameMode = player.get(Keys.GAME_MODE);
            if (gameMode.isPresent())
            {
                i18n.send(context, NEUTRAL, "Gamemode: {input#gamemode}", gameMode.get().getTranslation());
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

            if (!gameMode.isPresent() || !gameMode.get().equals(GameModes.CREATIVE))
            {
                Optional<InvulnerabilityData> data = player.get(InvulnerabilityData.class);
                if (data.isPresent())
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
            String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(context.getLocale())
                                             .withZone(ZoneId.systemDefault())
                                             .format(player.get(Keys.FIRST_DATE_PLAYED).get());
            i18n.send(context, NEUTRAL, "First played: {input#date}", format);
        }
        BanService banService = Sponge.getServiceManager().provideUnchecked(BanService.class);
        if (banService.isBanned(player.getProfile()))
        {
            Profile ban = banService.getBanFor(player.getProfile()).get();
            Text expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());

            if (!ban.getExpirationDate().isPresent())
            {
                expires = i18n.translate(context, NONE, "for ever");
            }
            else
            {
                expires = Text.of(format.format(ban.getExpirationDate().get()));
            }
            i18n.send(context, NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})",
                                ban.getBanSource().map(Text::toPlain).orElse("?"), format.format(ban.getCreationDate()),
                                ban.getReason(), expires);
        }
    }
}
