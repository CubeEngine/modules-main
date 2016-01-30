package org.cubeengine.module.vanillaplus.addition;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.basics.BasicsAttachment;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.TimeUtil;
import org.cubeengine.module.core.util.math.BlockVector3;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.world.Location;

import static java.text.DateFormat.SHORT;

public class PlayerInfoCommands
{
    private static final long SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000;

    @Command(desc = "Shows when given player was online the last time")
    public void seen(CommandSender context, Player player)
    {
        if (player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "{user} is currently online!", player);
            return;
        }

        Date lastPlayed = player.get(Keys.FIRST_DATE_PLAYED).get();
        if (System.currentTimeMillis() - lastPlayed.getTime() <= SEVEN_DAYS) // If less than 7 days show timeframe instead of date
        {
            context.sendTranslated(NEUTRAL, "{user} was last seen {input#date}.", player, TimeUtil.format(
                context.getLocale(), new Date(lastPlayed.getTime())));
            return;
        }
        Date date = new Date(lastPlayed.getTime());
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale());
        context.sendTranslated(NEUTRAL, "{user} is offline since {input#time}", player, format.format(date));
    }




    @Command(desc = "Displays informations from a player!")
    public void whois(CommandSender context, User player)
    {
        if (player.asPlayer().isOnline())
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user}", player);
        }
        else
        {
            context.sendTranslated(NEUTRAL, "Nickname: {user} ({text:offline})", player);
        }
        if (player.hasPlayedBefore() || player.isOnline())
        {
            context.sendTranslated(NEUTRAL, "Life: {decimal:0}/{decimal#max:0}", player.getHealth(), player.getMaxHealth());
            context.sendTranslated(NEUTRAL, "Hunger: {integer#foodlvl:0}/{text:20} ({integer#saturation}/{integer#foodlvl:0})", player.getFoodLevel(), (int)player.getSaturation(), player.getFoodLevel());
            context.sendTranslated(NEUTRAL, "Level: {integer#level} + {integer#percent}%", player.getLevel(), (int)(player.getExpPerecent() * 100));
            Location loc = player.getLocation();
            if (loc != null)
            {
                context.sendTranslated(NEUTRAL, "Position: {vector} in {world}", new BlockVector3(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getExtent());
            }
            if (player.getAddress() != null)
            {
                context.sendTranslated(NEUTRAL, "IP: {input#ip}", player.getAddress().getAddress().getHostAddress());
            }
            if (player.getGameMode() != null)
            {
                context.sendTranslated(NEUTRAL, "Gamemode: {input#gamemode}", player.getGameMode().toString());
            }
            if (player.getAllowFlight())
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:true:color=BRIGHT_GREEN} {input#flying}", player.isFlying() ? "flying" : "not flying");
            }
            else
            {
                context.sendTranslated(NEUTRAL, "Flymode: {text:false:color=RED}");
            }
            if (player.isOp())
            {
                context.sendTranslated(NEUTRAL, "OP: {text:true:color=BRIGHT_GREEN}");
            }
            Timestamp muted = module.getBasicsUser(player.asPlayer()).getEntity().getValue(TABLE_BASIC_USER.MUTED);
            if (muted != null && muted.getTime() > System.currentTimeMillis())
            {
                context.sendTranslated(NEUTRAL, "Muted until {input#time}", DateFormat.getDateTimeInstance(SHORT, SHORT, context.getLocale()).format(muted));
            }
            if (player.getGameMode() != CREATIVE)
            {
                context.sendTranslated(NEUTRAL, "GodMode: {input#godmode}", player.isInvulnerable() ? ChatFormat.BRIGHT_GREEN + "true" : ChatFormat.RED + "false");
            }
            if (player.get(BasicsAttachment.class).isAfk())
            {
                context.sendTranslated(NEUTRAL, "AFK: {text:true:color=BRIGHT_GREEN}");
            }
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SHORT, SHORT, Locale.ENGLISH);
            context.sendTranslated(NEUTRAL, "First played: {input#date}", dateFormat.format(player.getFirstPlayed()));
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
            context.sendTranslated(NEUTRAL, "Banned by {user} on {input#date}: {input#reason} ({input#expire})", ban.getSource(), format.format(ban.getCreated()), ban.getReason(), expires);
        }
    }
}
