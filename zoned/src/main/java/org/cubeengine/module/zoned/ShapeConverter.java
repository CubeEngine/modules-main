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
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.converter.ClassedConverter;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;

public class ShapeConverter implements ClassedConverter<Shape>
{

    @Override
    public Node toNode(Shape shape, ConverterManager manager) throws ConversionException
    {
        if (shape instanceof Cuboid)
        {
            MapNode node = MapNode.emptyMap();
            Cuboid cuboid = (Cuboid) shape;
            node.set("type", StringNode.of(shape.getClass().getName()));
            Vector3d min = cuboid.getMinimumPoint();
            Vector3d max = cuboid.getMaximumPoint();
            node.set("min", manager.convertToNode(min));
            node.set("max", manager.convertToNode(max));
            return node;
        }
        throw new UnsupportedOperationException("Unsupported Shape " + shape.getClass().getSimpleName());
    }

    @Override
    public Shape fromNode(Node node, Class<? extends Shape> type, ConverterManager manager) throws ConversionException
    {
        if (node instanceof MapNode) {
            MapNode map = (MapNode) node;
            switch (map.get("type").asText())
            {
                case "org.cubeengine.libcube.util.math.shape.Cuboid":
                    Vector3d min = manager.convertFromNode(map.get("min"), Vector3d.class);
                    Vector3d max = manager.convertFromNode(map.get("max"), Vector3d.class);
                    return new Cuboid(min, max.sub(min));
                // TODO more shapes
                default:
                    throw new UnsupportedOperationException("Unsupported Shape");
            }

        }
        throw new UnsupportedOperationException("Invalid Shape Data " + node.asString());
    }
}
