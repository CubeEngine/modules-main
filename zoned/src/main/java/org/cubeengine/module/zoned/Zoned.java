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

import java.io.IOException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.zoned.command.SelectorCommand;
import org.cubeengine.module.zoned.command.ZonedCommands;
import org.cubeengine.module.zoned.config.ShapeConverter;
import org.cubeengine.module.zoned.config.Vector3dConverter;
import org.cubeengine.module.zoned.config.ZoneConfig;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.math.vector.Vector3d;

@Singleton
@Module
public class Zoned
{
    @Inject private Reflector reflector;
    @ModuleListener private ZonedListener listener;
    @ModuleCommand private ZonedCommands zonedCommands;
    @ModuleCommand private SelectorCommand selectorCommand;
    @Inject private ZoneManager manager;

    public Zoned()
    {
        System.out.println("Created Zoned instance");
    }

    @Listener
    public void onEnable(StartingEngineEvent<Server> event)
    {
        this.reflector.getDefaultConverterManager().registerConverter(new ShapeConverter(), Shape.class);
        this.reflector.getDefaultConverterManager().registerConverter(new Vector3dConverter(), Vector3d.class);

        try
        {
            this.manager.loadZones();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        ZonedData.register(event);
    }

    public ZoneConfig getActiveZone(ServerPlayer player)
    {
        return this.listener.getZone(player);
    }

    public void setActiveZone(ServerPlayer player, ZoneConfig zone)
    {
        this.listener.setZone(player, zone);
    }

    public ZoneManager getManager()
    {
        return this.manager;
    }
}
