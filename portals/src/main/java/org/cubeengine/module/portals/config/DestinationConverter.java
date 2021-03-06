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
package org.cubeengine.module.portals.config;

import java.util.Map;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.converter.SingleClassConverter;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.module.portals.config.Destination.Type;
import org.cubeengine.libcube.service.config.ConfigWorld;
import org.spongepowered.math.vector.Vector3d;

public class DestinationConverter extends SingleClassConverter<Destination>
{
    @Override
    public Node toNode(Destination destination, ConverterManager converterManager) throws ConversionException
    {
        final MapNode result = MapNode.emptyMap();
        result.set("type", StringNode.of(destination.type.name()));
        switch (destination.type)
        {
            case PORTAL:
                result.set("portal", StringNode.of(destination.portal));
                break;
            case WORLD:
            case RANDOM:
                result.set("world", StringNode.of(destination.world.getName()));
                break;
            case LOCATION:
                result.set("world", StringNode.of(destination.world.getName()));
                result.set("position", converterManager.convertToNode(destination.position));
                result.set("rotation", converterManager.convertToNode(destination.rotation));
                break;
        }
        return result;
    }

    @Override
    public Destination fromNode(Node node, ConverterManager converterManager) throws ConversionException
    {
        if (node instanceof MapNode)
        {
            final Map<String, Node> mappedNodes = ((MapNode)node).getValue();
            try
            {
                final Type type = Type.valueOf(mappedNodes.get("type").asText());
                Destination destination;
                if (type == Type.RANDOM)
                {
                    destination = new RandomDestination();
                }
                else
                {
                    destination = new Destination();
                }
                destination.type = type;
                switch (type)
                {
                    case PORTAL:
                        destination.portal = mappedNodes.get("portal").asText();
                        break;
                    case RANDOM:
                    case WORLD:
                        destination.world = new ConfigWorld(mappedNodes.get("world").asText());
                        break;
                    case LOCATION:
                        destination.world = new ConfigWorld(mappedNodes.get("world").asText());
                        destination.position = converterManager.convertFromNode(mappedNodes.get("position"), Vector3d.class);
                        destination.rotation = converterManager.convertFromNode(mappedNodes.get("rotation"), Vector3d.class);
                        break;
                }
                return destination;
            }
            catch (IllegalArgumentException e)
            {
                throw ConversionException.of(this, mappedNodes.get("type"), "Could not read Type!");
            }
        }
        throw ConversionException.of(this, node, "Node is not a mapnode!");
    }
}
