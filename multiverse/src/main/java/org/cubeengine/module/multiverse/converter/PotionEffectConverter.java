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
package org.cubeengine.module.multiverse.converter;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.converter.SingleClassConverter;
import de.cubeisland.engine.converter.node.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
                Optional<PotionEffectType> byName = Sponge.getRegistry().getType(PotionEffectType.class, type.asText());

                if (byName.isPresent())
                {
                    return PotionEffect.builder().potionType(byName.get())
                                .duration(((IntNode)duration).getValue())
                                .amplifier(((IntNode)amplifier).getValue())
                                .ambience(((ByteNode)ambient).getValue() == 1).build();
                }
                else
                {
                    throw ConversionException.of(this, node, "Unknown PotionEffectType " + byName.get().getName());
                }
            }
        }
        throw ConversionException.of(this, node, "Invalid NodeTypes!");
    }
}
