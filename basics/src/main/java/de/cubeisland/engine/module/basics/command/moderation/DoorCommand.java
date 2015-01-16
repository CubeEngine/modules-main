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
package de.cubeisland.engine.module.basics.command.moderation;

import java.util.EnumSet;
import java.util.Set;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.math.Vector3;
import de.cubeisland.engine.core.util.math.shape.Sphere;
import de.cubeisland.engine.module.basics.Basics;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Openable;

import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;

public class DoorCommand
{
    private final Basics basics;

    public DoorCommand(Basics basics)
    {
        this.basics = basics;
    }

    @Command(desc = "Opens or closes doors around the player.")
    @Params(positional = {@Param(names = {"open","close"}),
                          @Param(label = "radius", type = Integer.class),
                          @Param(req = OPTIONAL, label = "world", type = World.class),
                          @Param(req = OPTIONAL, label = "x", type = Integer.class),
                          @Param(req = OPTIONAL, label = "y", type = Integer.class),
                          @Param(req = OPTIONAL, label = "z", type = Integer.class)})
    @Flags({@Flag(longName = "all", name = "a"),
            @Flag(longName = "woodenDoor", name = "w"),
            @Flag(longName = "ironDoor", name = "i"),
            @Flag(longName = "trapDoor", name = "t"),
            @Flag(longName = "fenceGate", name = "f")})
    public void doors(CommandContext context)
    {
        boolean open;
        int radius = context.get(1, 0);
        Vector3 vector;
        World world;
        Set<Material> openMaterials = EnumSet.noneOf(Material.class);

        String task = context.get(0);
        if("open".equalsIgnoreCase(task))
        {
            open = true;
        }
        else if("close".equalsIgnoreCase(task))
        {
            open = false;
        }
        else
        {
            context.sendTranslated(NEGATIVE, "Do not know whether I should close or open the doors");
            return;
        }

        if(radius > this.basics.getConfiguration().commands.maxDoorRadius)
        {
            context.sendTranslated(NEGATIVE, "You can't use this with a radius over {amount}", this.basics.getConfiguration().commands.maxDoorRadius);
            return;
        }

        if(!context.hasPositional(5) && !(context.getSource() instanceof User))
        {
            context.sendTranslated(NEGATIVE, "You has to specify a location!");
            return;
        }
        else if(!context.hasPositional(5))
        {
            Location location = ((User) context.getSource()).getLocation();
            world = location.getWorld();
            vector = new Vector3(location.getX(), location.getY(), location.getZ());
        }
        else
        {
            world = context.get(2, null);
            if(world == null)
            {
                context.sendTranslated(NEGATIVE, "World {input#world} not found!", context.get(2));
                return;
            }
            Integer x = context.get(3, null);
            if(x == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid x-value {input}!", context.get(3));
                return;
            }
            Integer y = context.get(4, null);
            if(y == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid y-value {input}!", context.get(4));
                return;
            }
            Integer z = context.get(5, null);
            if(z == null)
            {
                context.sendTranslated(NEGATIVE, "Invalid z-value {input}!", context.get(5));
                return;
            }
            vector = new Vector3(x, y, z);
        }
        
        if(context.hasFlag("f"))
        {
            openMaterials.add(Material.FENCE_GATE);
        }
        if(context.hasFlag("t"))
        {
            openMaterials.add(Material.TRAP_DOOR);
        }
        if(context.hasFlag("i"))
        {
            openMaterials.add(Material.IRON_DOOR_BLOCK);
        }
        if(context.hasFlag("w") || (openMaterials.isEmpty() && !context.hasFlag("a")))
        {
            openMaterials.add(Material.WOODEN_DOOR);
        }

        Sphere sphere = new Sphere(vector, radius);
        for(Vector3 point : sphere)
        {
            Block block = world.getBlockAt((int) point.x, (int) point.y, (int) point.z);
            if(context.hasFlag("a") || openMaterials.contains(block.getType()))
            {
                this.setOpen(block, open);
            }
        }
    }

    /**
     * sets the block either open or closed.
     *
     * @param block - the block
     * @param open  - true to set open, false to set closed
     * @return returns whether the block could set or not
     */
    public boolean setOpen(Block block, boolean open)
    {
        Material type = block.getType();
        BlockState state = block.getState();

        if(!(state.getData() instanceof Openable))
        {
            return false;
        }

        byte rawData = state.getRawData();

        if((type == Material.WOODEN_DOOR || type == Material.IRON_DOOR_BLOCK) && (rawData & 0x8) == 0x8)
        {
            return false;
        }

        if(open)
        {
            state.setRawData((byte) (rawData | 0x4));   // open door
        }
        else
        {
            state.setRawData((byte) (rawData & 0xB));    //close door
        }
        state.update();
        return true;
    }
}
