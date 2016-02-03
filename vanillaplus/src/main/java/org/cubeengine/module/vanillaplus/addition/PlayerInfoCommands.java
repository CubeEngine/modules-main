package org.cubeengine.module.vanillaplus.addition;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.basics.BasicsAttachment;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.TimeUtil;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;

import static java.text.DateFormat.SHORT;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;

public class PlayerInfoCommands
{
    private I18n i18n;

    public PlayerInfoCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandSource context, Player player)
    {
        if (player.isOnline())
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Instant lastPlayed = player.get(Keys.FIRST_DATE_PLAYED).get();
        if (System.currentTimeMillis() - lastPlayed.toEpochMilli() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} was last seen {input#date}.", player, TimeUtil.format(
                context.getLocale(), new Date(lastPlayed.toEpochMilli())));
            return;
        }
        Date date = new Date(lastPlayed.toEpochMilli());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
        i18n.sendTranslated(context, NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
    }




    @Command(desc = "Displays informations from a player!")
    public void whois(CommandSource context, User player)
    {
        if (player.isOnline())
        {
            i18n.sendTranslated(context, NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            i18n.sendTranslated(context, NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        if (player.hasPlayedBefore() || player.isOnline())
        {
            i18n.sendTranslated(context, NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.getHealth(), player.getMaxHealth());
            i18n.sendTranslated(context, NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.getFoodLevel(), (int)player.getSaturation(), player.getFoodLevel());
            i18n.sendTranslated(context, NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.getLevel(), (int)(player.getExpPerecent() * 100));
            Location loc = player.getLocation();
            if (loc != null)
            {
                i18n.sendTranslated(context, NEUTRAL, "Position: {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
            }
            if (player.getAddress() != null)
            {
                i18n.sendTranslated(context, NEUTRAL, "IP: {input#ip}", player.getAddress().getAddress().getHostAddress());
            }
            if (player.getGameMode() != null)
            {
                i18n.sendTranslated(context, NEUTRAL, "Gamemode: {input#gamemode}", player.getGameMode().toString());
            }
            if (player.getAllowFlight())
            {
                i18n.sendTranslated(context, NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", player.isFlying() ? "flying" : "not flying");
            }
            else
            {
                i18n.sendTranslated(context, NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            if (player.isOp())
            {
                i18n.sendTranslated(context, NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }
            Timestamp muted = module.getBasicsUser(player.asPlayer()).getEntity().getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && muted.getTime() > System.currentTimeMillis())
            {
                i18n.sendTranslated(context, NEUTRAL, "Muted until {input#time}", DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale()).format(muted));
            }
            if (player.getGameMode() != CREATIVE)
            {
                i18n.sendTranslated(context, NEUTRAL, "GodMode: {input#godmode}", player.isInvulnerable() ? ChatFormat.BRIGHT_GREEN + "true" : ChatFormat.RED + "false");
            }
            if (player.get(BasicsAttachment.class).isAfk())
            {
                i18n.sendTranslated(context, NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SHORT, SHORT, Locale.ENGLISH);
            i18n.sendTranslated(context, NEUTRAL, "First played: {input#date}", dateFormat.format(player.getFirstPlayed()));
        }
        if (banManager.isUserBanned(player.getUniqueId()))
        {
            UserBan ban = banManager.getUserBan(player.getUniqueId());
            String expires;
            DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
            if (ban.getExpires() != null)
            {
                expires = format.format(ban.getExpires());
            }
            else
            {
                expires = context.getTranslation(NONE, "for ever").toString();
            }
            i18n.sendTranslated(context, NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})", ban.getSource(), format.format(ban.getCreated()), ban.getReason(), expires);
        }
    }
}
