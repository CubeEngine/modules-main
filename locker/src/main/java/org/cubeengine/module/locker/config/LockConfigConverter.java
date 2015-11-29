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
package org.cubeengine.module.locker.config;

import java.util.ArrayList;
import java.util.Map.Entry;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.converter.node.BooleanNode;
import de.cubeisland.engine.converter.node.ListNode;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.NullNode;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.module.locker.storage.LockType;
import org.cubeengine.module.locker.storage.ProtectionFlag;

public abstract class LockConfigConverter<C extends LockConfig<C, ?>> extends SimpleConverter<C>
{
    private Log logger;

    public LockConfigConverter(Log logger)
    {
        this.logger = logger;
    }

    @Override
    public Node toNode(C object) throws ConversionException
    {
        MapNode root = MapNode.emptyMap();
        MapNode config = MapNode.emptyMap();
        if (!object.enable)
        {
            config.set("enable", BooleanNode.falseNode());
        }
        if (object.autoProtect)
        {
            config.set("auto-protect", StringNode.of(object.autoProtectType.name()));
        }
        if (object.defaultFlags != null && !object.defaultFlags.isEmpty())
        {
            ListNode flags = ListNode.emptyList();
            for (ProtectionFlag defaultFlag : object.defaultFlags)
            {
                flags.addNode(StringNode.of(defaultFlag.name()));
            }
            config.set("default-flags", flags);
        }
        if (config.isEmpty())
        {
            return StringNode.of(object.getTitle());
        }
        root.set(object.getTitle(), config);
        return root;
    }

    @Override
    public C fromNode(Node node) throws ConversionException
    {
        if (node instanceof NullNode) return null;
        C configuration;
        if (node instanceof StringNode)
        {
            configuration = fromString(node.asText());
        }
        else
        {
            MapNode root = (MapNode)node;
            if (root.isEmpty()) return null;
            String next = root.getOriginalKey(root.getValue().keySet().iterator().next());
            MapNode config = (MapNode)root.get(next);
            configuration = fromString(next);
            for (Entry<String, Node> entry : config.getValue().entrySet())
            {
                if (entry.getKey().equals("enable"))
                {
                    configuration.enable = ((BooleanNode)entry.getValue()).getValue();
                }
                if (entry.getKey().equals("auto-protect"))
                {
                    configuration.autoProtect = true;
                    configuration.autoProtectType = LockType.valueOf(entry.getValue().asText());
                }
                if (entry.getKey().equals("default-flags"))
                {
                    ListNode list = (ListNode)entry.getValue();
                    configuration.defaultFlags = new ArrayList<>();
                    for (Node listedNode : list.getValue())
                    {
                        ProtectionFlag flag = ProtectionFlag.valueOf(listedNode.asText());
                        configuration.defaultFlags.add(flag);
                        if (configuration.protectedType.supportedFlags.contains(flag))
                        {
                        }
                        else
                        {
                            logger.warn("[Locker] Unsupported flag for protectedType! {}: {}", configuration.protectedType.name(), flag.name());
                        }
                    }
                }
            }
        }
        return configuration;
    }

    protected abstract C fromString(String s) throws ConversionException;
}
