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

import java.util.List;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.converter.SimpleConverter;
import org.cubeengine.converter.node.DoubleNode;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.Node;
import org.spongepowered.math.vector.Vector3d;

public class Vector3dConverter extends SimpleConverter<Vector3d>
{

    @Override
    public Node toNode(Vector3d vector) throws ConversionException
    {
        ListNode node = ListNode.emptyList();
        node.addNode(new DoubleNode(vector.x()));
        node.addNode(new DoubleNode(vector.y()));
        node.addNode(new DoubleNode(vector.z()));
        return node;
    }

    @Override
    public Vector3d fromNode(Node node) throws ConversionException
    {
        if (!(node instanceof ListNode))
        {
            throw ConversionException.of(this, node, "Node is not a ListNode");
        }
        List<Node> value = ((ListNode)node).getValue();
        double x = Double.parseDouble(value.get(0).asText());
        double y = Double.parseDouble(value.get(1).asText());
        double z = Double.parseDouble(value.get(2).asText());
        return new Vector3d(x, y, z);
    }
}
