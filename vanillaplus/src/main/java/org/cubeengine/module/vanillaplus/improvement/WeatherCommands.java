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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.WeatherType;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Commands changing time. /time /ptime
 */
@Singleton
public class WeatherCommands
{
    private I18n i18n;

    @Inject
    public WeatherCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Changes the weather")
    public void weather(CommandCause context, WeatherType weather, @Option Integer duration, @Default @Named("in") ServerWorld world)
    {
        duration = (duration == null ? 10000000 : duration) * 20;

        final Weather currentWeather = world.weather();
        final String weatherKey = Sponge.getGame().registries().registry(RegistryTypes.WEATHER_TYPE).valueKey(weather).getValue();
        if (currentWeather.type() == weather) // weather is not changing
        {
            i18n.send(context, POSITIVE, "Weather in {world} is already set to {input#weather}!", world, weatherKey);
        }
        else
        {
            i18n.send(context, POSITIVE, "Changed weather in {world} to {input#weather}!", world, weatherKey);
        }

        world.setWeather(weather, Ticks.of(duration));
    }

    /* TODO wait for https://github.com/SpongePowered/SpongeAPI/issues/393
    @Command(alias = "playerweather", desc = "Changes your weather")
    public void pweather(CommandSource context, PlayerWeather weather, @Default @Named("player") Player player)
    {
        if (!player.isOnline())
        {
            i18n.sendTranslated(context, NEGATIVE, "{user} is not online!", player);
            return;
        }
        switch (weather)
        {
            case SUN:
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
            case RAIN:
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
    //*/
}
