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
import java.util.Set;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parameter.TooFewArgumentsException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import de.cubeisland.engine.modularity.core.Module;

import org.cubeengine.module.core.module.ModuleCommands;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.command.CommandSender;
import org.cubeengine.module.core.sponge.CoreModule;
import de.cubeisland.engine.service.permission.Permission;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.Profiler;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.difficulty.Difficulty;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SaveCommands
{
    private final VanillaPlus module;
    private Game game;

    private final Permission COMMAND_VERSION_PLUGINS;

    public SaveCommands(VanillaPlus module, Game game, PermissionManager pm)
    {
        this.module = module;
        this.game = game;

        COMMAND_VERSION_PLUGINS = module.getProvided(Permission.class).childWildcard("command").childWildcard("version").child("plugins");
        pm.registerPermission(module, COMMAND_VERSION_PLUGINS);
    }

    // todo integrate /saveoff and /saveon and provide aliases
    @Alias(value = "save-all")
    @Command(desc = "Saves all or a specific world to disk.")
    public void saveall(CommandSender context, @Optional World world)
    {
        i18n.sendTranslated(context, NEUTRAL, "Saving...");
        if (world != null)
        {
            game.getServer().saveWorldProperties(world.getProperties()); // TODO is this saving the world?
            // TODO world.getEntities().stream().filter(entity -> entity instanceof Player).forEach(player -> player.saveData());
            i18n.sendTranslated(context, POSITIVE, "World {world} has been saved to disk!", world);
            return;
        }
        Profiler.startProfiling("save-worlds");
        for (World aWorld : game.getServer().getWorlds())
        {
            game.getServer().saveWorldProperties(aWorld.getProperties()); // TODO is this saving the world?
        }
        // TODO this.core.getServer().savePlayers();
        i18n.sendTranslated(context, POSITIVE, "All worlds have been saved to disk!");
        i18n.sendTranslated(context, POSITIVE, "The saving took {integer#time} milliseconds.", Profiler.endProfiling("save-worlds", MILLISECONDS));
    }


}
