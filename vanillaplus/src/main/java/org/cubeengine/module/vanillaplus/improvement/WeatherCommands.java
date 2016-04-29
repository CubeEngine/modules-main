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

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Commands changing time. /time /ptime
 */
public class WeatherCommands
{
    private I18n i18n;

    public WeatherCommands(I18n i18n)
    {
        this.i18n = i18n;
    }



    public enum Weather
    {
        SUN, RAIN, STORM
    }

    @Command(desc = "Changes the weather")
    public void weather(CommandSource context, Weather weather, @Optional Integer duration, @Default @Named({"in", "world"}) World world)
    {
        boolean sunny = true;
        boolean noThunder = true;
        duration = (duration == null ? 10000000 : duration) * 20;
        switch (weather)
        {
            case SUN:
                sunny = true;
                noThunder = true;
                break;
            case RAIN:
                sunny = false;
                noThunder = true;
                break;
            case STORM:
                sunny = false;
                noThunder = false;
                break;
        }

        WorldProperties worldProp = world.getProperties();
        if (worldProp.isThundering() != noThunder && worldProp.isRaining() != sunny) // weather is not changing
        {
            i18n.sendTranslated(context, POSITIVE, "Weather in {world} is already set to {input#weather}!", world, weather.name());
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "Changed weather in {world} to {input#weather}!", world, weather.name());
        }
        worldProp.setRaining(!sunny);
        worldProp.setThundering(!noThunder);
        worldProp.setRainTime(duration);
    }

    public enum PlayerWeather
    {
        CLEAR, DOWNFALL, RESET
    }

    /* TODO wait for https://github.com/SpongePowered/SpongeAPI/issues/1059
    @Command(alias = "playerweather", desc = "Changes your weather")
    public void pweather(CommandContext context, PlayerWeather weather, @Default @Named("player") Player player)
    {
        if (!player.isOnline())
        {
            i18n.sendTranslated(context, NEGATIVE, "{user} is not online!", player);
            return;
        }
        switch (weather)
        {
            case CLEAR:
                player.setPlayerWeather(Weathers.CLEAR);
                if (context.getSource().equals(player))
                {
                    i18n.sendTranslated(context, POSITIVE, "Your weather is now clear!");
                }
                else
                {
                    player.sendTranslated(POSITIVE, "Your weather is now clear!");
                    i18n.sendTranslated(context, POSITIVE, "{user}s weather is now clear!", player);
                }
                return;
            case DOWNFALL:
                player.setPlayerWeather(Weathers.RAIN);
                if (context.getSource().equals(player))
                {
                    i18n.sendTranslated(context, POSITIVE, "Your weather is now not clear!");
                }
                else
                {
                    player.sendTranslated(POSITIVE, "Your weather is now not clear!");
                    i18n.sendTranslated(context, POSITIVE, "{user}s weather is now not clear!", player);
                }
                return;
            case RESET:
                player.resetPlayerWeather();
                if (context.getSource().equals(player))
                {
                    i18n.sendTranslated(context, POSITIVE, "Your weather is now reset to server weather!");
                }
                else
                {
                    player.sendTranslated(POSITIVE, "Your weather is now reset to server weather!");
                    i18n.sendTranslated(context, POSITIVE, "{user}s weather is now reset to server weather!", player);
                }
                return;
        }
        throw new IncorrectUsageException("You did something wrong!");
    }
    */
}
