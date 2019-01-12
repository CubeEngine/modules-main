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
package org.cubeengine.module.zoned;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Zoned extends CubeEngineModule
{
    @Inject private CommandManager cm;
    @Inject private I18n i18n;

    private Path path;
    @Inject private ModuleManager mm;
    @Inject private Reflector reflector;

    @ModuleListener private ZonesListener listener;
    @ModuleCommand private ZonedCommands zonedCommands;
    @ModuleCommand private SelectorCommand selectorCommand;
    @Inject private TaskManager tm;
    @Inject private EventManager em;

    private ZoneManager manager;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.reflector.getDefaultConverterManager().registerConverter(new ShapeConverter(), Shape.class);
        this.reflector.getDefaultConverterManager().registerConverter(new Vector3dConverter(), Vector3d.class);

        this.path = mm.getPathFor(Zoned.class);
        this.manager = new ZoneManager(this, i18n, reflector, mm.getLoggerFor(Zoned.class));
        this.cm.getProviders().register(this, new ZoneParser(this, manager, i18n), ZoneConfig.class);
    }

    public ZoneConfig getActiveZone(Player player)
    {
        return this.listener.getZone(player);
    }

    public void setActiveZone(Player player, ZoneConfig zone)
    {
        this.listener.setZone(player, zone);
    }

    public Path getPath()
    {
        return this.path;
    }

    public ZoneManager getManager()
    {
        return this.manager;
    }
}
