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

import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.util.Profiler;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class SaveCommands
{
    private I18n i18n;

    public SaveCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    // TODO integrate /saveoff and /saveon and provide aliases
    @Alias(value = "save-all")
    @Command(desc = "Saves all or a specific world to disk.")
    public void saveall(CommandSource context, @Optional World world)
    {
        i18n.sendTranslated(context, NEUTRAL, "Saving...");
        Server server = Sponge.getServer();
        if (world != null)
        {
            server.saveWorldProperties(world.getProperties()); // TODO is this saving the world?
            // TODO world.getEntities().stream().filter(entity -> entity instanceof Player).forEach(player -> player.saveData());
            i18n.sendTranslated(context, POSITIVE, "World {world} has been saved to disk!", world);
            return;
        }
        Profiler.startProfiling("save-worlds");
        for (World aWorld : server.getWorlds())
        {
            server.saveWorldProperties(aWorld.getProperties()); // TODO is this saving the world?
        }
        // TODO this.core.getServer().savePlayers();
        i18n.sendTranslated(context, POSITIVE, "All worlds have been saved to disk!");
        i18n.sendTranslated(context, POSITIVE, "The saving took {integer#time} milliseconds.", Profiler.endProfiling("save-worlds", MILLISECONDS));
    }
}
