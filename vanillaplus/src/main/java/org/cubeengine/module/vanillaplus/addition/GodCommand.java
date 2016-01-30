package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.spongepowered.api.data.key.Keys;

/**
 * Provides Gamemodelike Protection
 */
public class GodCommand
{

    @Command(desc = "Toggles the god-mode!")
    public void god(CommandSender context, @Default User player)
    {
        boolean other = false;
        if (!context.equals(player))
        {
            if (!module.perms().COMMAND_GOD_OTHER.isAuthorized(context))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to god others!");
                return;
            }
            other = true;
        }

        Integer invTime = player.asPlayer().get(Keys.INVULNERABILITY).or(0);
        if (invTime > 0)
        {
            player.asPlayer().remove(Keys.INVULNERABILITY);
            if (!other)
            {
                i18n.sendTranslated(context, NEUTRAL, "You are no longer invincible!");
                return;
            }
            player.sendTranslated(NEUTRAL, "You are no longer invincible!");
            i18n.sendTranslated(context, NEUTRAL, "{user} is no longer invincible!", player);
            return;
        }
        player.asPlayer().offer(Keys.INVULNERABILITY, Integer.MAX_VALUE);
        if (!other)
        {
            i18n.sendTranslated(context, POSITIVE, "You are now invincible!");
            return;
        }
        player.sendTranslated(POSITIVE, "You are now invincible!");
        i18n.sendTranslated(context, POSITIVE, "{user} is now invincible!", player);
    }
}
