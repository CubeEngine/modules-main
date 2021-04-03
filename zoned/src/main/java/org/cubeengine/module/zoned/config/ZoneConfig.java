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
package org.cubeengine.module.zoned.config;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.math.vector.Vector3d;

public class ZoneConfig extends ReflectedYaml
{
    public String name;

    public ConfigWorld world;

    public Shape shape;

    private Map<Integer, Vector3d> vectors = new HashMap<>();
    private Class<? extends Shape> currentShape = Cuboid.class;

    public Component addPoint(I18n i18n, ServerPlayer player, boolean primary, Vector3d loc)
    {
        if (currentShape == Cuboid.class)
        {
            int pos = primary ? 1 : 2;

            vectors.put(pos, loc);

            if (vectors.containsKey(1) && vectors.containsKey(2))
            {
                Vector3d size = vectors.get(2).sub(vectors.get(1));
                shape = new Cuboid(vectors.get(1), size);
            }
            else
            {
                shape = null;
            }

            return i18n.translate(player, "Position {number}", pos);
        }
        else
        {
            // TODO additional shapes
        }
        shape = null;
        return Component.text("UNSUPPORTED SHAPE " + currentShape.getSimpleName());
    }

    public Component getSelected(I18n i18n, ServerPlayer player)
    {
        if (currentShape == Cuboid.class)
        {
            if (vectors.containsKey(1) && vectors.containsKey(2))
            {
                Vector3d pos1 = vectors.get(1);
                Vector3d pos2 = vectors.get(2);
                Vector3d size = pos1.sub(pos2);
                int count = (int)((Math.abs(size.getX()) + 1) * (Math.abs(size.getY()) + 1) * (Math.abs(size.getZ()) + 1));

                return i18n.translate(player, Style.empty().color(NamedTextColor.GRAY), "{amount} blocks selected", count);
            }
            else
            {
                return i18n.translate(player, Style.empty().color(NamedTextColor.GRAY), "incomplete selection");
            }
        }
        else
        {
            // TODO additional shapes
        }
        return Component.text("UNSUPPORTED SHAPE " + currentShape.getSimpleName());
    }

    public ZoneConfig name(String name)
    {
        this.name = name;
        return this;
    }

    public boolean isComplete()
    {
        return this.shape != null;
    }

    public void clear()
    {
        this.vectors.clear();
        this.world = null;
        this.shape = null;
    }

    public ZoneConfig clone(Reflector reflector)
    {
        ZoneConfig clone = reflector.create(ZoneConfig.class);
        clone.name = this.name;
        clone.shape = this.shape;
        clone.vectors = new HashMap<>(this.vectors);
        clone.currentShape = this.currentShape;
        clone.world = this.world;
        return clone;
    }
}
