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
package de.cubeisland.engine.module.worlds.converter;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.converter.converter.SingleClassConverter;
import de.cubeisland.engine.converter.node.ByteNode;
import de.cubeisland.engine.converter.node.IntNode;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.NullNode;
import de.cubeisland.engine.converter.node.StringNode;

public class PotionEffectConverter extends SingleClassConverter<PotionEffect>
{
    @Override
    public Node toNode(PotionEffect object, ConverterManager manager) throws ConversionException
    {
        Map<String, Object> map = new HashMap<>();
        map.put("amplifier", object.getAmplifier());
        map.put("duration", object.getDuration());
        map.put("type", object.getType().getName());
        map.put("ambient", object.isAmbient());
        return manager.convertToNode(map);
    }

    @Override
    public PotionEffect fromNode(Node node, ConverterManager manager) throws ConversionException
    {
        if (node instanceof NullNode)
        {
            return null;
        }
        if (node instanceof MapNode)
        {
            Node amplifier = ((MapNode)node).get("amplifier");
            Node duration = ((MapNode)node).get("duration");
            Node type = ((MapNode)node).get("type");
            Node ambient = ((MapNode)node).get("ambient");
            if (amplifier instanceof IntNode && duration instanceof IntNode && type instanceof StringNode && ambient instanceof ByteNode)
            {
                PotionEffectType byName = PotionEffectType.getByName(type.asText());
                if (byName != null)
                {
                    return new PotionEffect(byName, ((IntNode)duration).getValue(), ((IntNode)amplifier).getValue(), ((ByteNode)ambient).getValue() == 1);
                }
                else
                {
                    throw ConversionException.of(this, node, "Unknown PotionEffectType " + type.asText());
                }
            }
        }
        throw ConversionException.of(this, node, "Invalid NodeTypes!");
    }
}
