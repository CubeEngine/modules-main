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
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.converter.node.IntNode;
import de.cubeisland.engine.converter.node.ListNode;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.ShortNode;
import de.cubeisland.engine.converter.node.StringNode;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.Game;
import org.spongepowered.api.item.inventory.Inventory;

public class InventoryConverter extends SimpleConverter<Inventory>
{
    private final Game game;

    public InventoryConverter(Game game)
    {
        this.game = game;
    }

    @Override
    public Node toNode(Inventory object) throws ConversionException
    {
        MapNode node = MapNode.emptyMap();
        ItemStack[] contents = object.getContents();
        ListNode list = ListNode.emptyList();
        node.set("Size", new IntNode(object.getSize() + 9));
        node.set("Contents", list);
        for (int i = 0; i < contents.length; i++)
        {
            ItemStack itemStack = contents[i];
            if (itemStack != null)
            {
                this.addItem(list, itemStack, i);
            }
        }
        if (object instanceof PlayerInventory)
        {
            ItemStack[] armorContents = ((PlayerInventory)object).getArmorContents();
            for (int i = 0; i < armorContents.length; i++)
            {
                ItemStack itemStack = armorContents[i];
                if (itemStack != null && itemStack.getType() != Material.AIR)
                {
                    this.addItem(list, itemStack, i + object.getSize());
                }
            }
        }
        return node;
    }

    private void addItem(ListNode list, ItemStack itemStack, int index)
    {
        MapNode item = MapNode.emptyMap();
        item.set("Slot", new IntNode(index));
        item.set("Count", new IntNode(itemStack.getAmount()));
        item.set("Damage", new ShortNode(itemStack.getDurability()));
        item.set("Item", StringNode.of(itemStack.getType().name()));
        NBTTagCompound tag = CraftItemStack.asNMSCopy(itemStack).getTag();
        item.set("tag", tag == null ? MapNode.emptyMap() : NBTUtils.convertNBTToNode(tag));
        list.addNode(item);
    }

    @Override
    public Inventory fromNode(Node node) throws ConversionException
    {
        if (node instanceof MapNode)
        {
            Node size = ((MapNode)node).get("Size");
            if (size instanceof IntNode)
            {
                Inventory inventory = game.createInventory(null, ((IntNode)size).getValue());
                Node contents = ((MapNode)node).get("Contents");
                if (contents instanceof ListNode)
                {
                    for (Node listedNode : ((ListNode)contents).getValue())
                    {
                        if (listedNode instanceof MapNode)
                        {
                            Node slot = ((MapNode)listedNode).get("Slot");
                            Node count = ((MapNode)listedNode).get("Count");
                            Node damage = ((MapNode)listedNode).get("Damage");
                            Node item = ((MapNode)listedNode).get("Item");
                            Node tag = ((MapNode)listedNode).get("tag");
                            if (slot instanceof IntNode && count instanceof IntNode && damage instanceof ShortNode &&
                                item instanceof StringNode && (tag instanceof MapNode))
                            {
                                try
                                {
                                    ItemStack itemStack = new ItemStack(Material.valueOf(item.asText()));
                                    itemStack.setDurability(((ShortNode)damage).getValue());
                                    itemStack.setAmount(((IntNode)count).getValue());
                                    net.minecraft.server.v1_8_R2.ItemStack nms = CraftItemStack.asNMSCopy(itemStack);
                                    nms.setTag(((MapNode)tag).isEmpty() ? null : (NBTTagCompound)NBTUtils.convertNodeToNBT(tag));
                                    inventory.setItem(((IntNode)slot).getValue(), CraftItemStack.asBukkitCopy(nms));
                                }
                                catch (IllegalArgumentException e)
                                {
                                    throw ConversionException.of(this, item, "Unknown Material!");
                                }
                            }
                            else
                            {
                                throw ConversionException.of(this, listedNode, "Invalid SubNodes!");
                            }
                        }
                        else
                        {
                            throw ConversionException.of(this, listedNode, "Node is not a MapNode!");
                        }
                    }
                    return inventory;
                }
                else
                {
                    throw ConversionException.of(this, contents, "Node is not a ListNode!");
                }
            }
            else
            {
                throw ConversionException.of(this, size, "Node is not a IntNode!");
            }
        }
        throw ConversionException.of(this, node, "Node is not a MapNode!");

    }
}
