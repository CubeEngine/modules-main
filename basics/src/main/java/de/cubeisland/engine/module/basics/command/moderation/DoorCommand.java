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
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.core.command.CommandSender;
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

import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;

public class DoorCommand
{
    private final Basics basics;

    public DoorCommand(Basics basics)
    {
        this.basics = basics;
    }

    public enum DoorState
    {
        OPEN, CLOSE
    }

    @Command(desc = "Opens or closes doors around the player.")
    public void doors(CommandSender context, DoorState state, Integer radius,
                      @Optional World world, @Optional Integer x, @Optional Integer y, @Optional Integer z,
                      @Flag boolean all, @Flag boolean woodenDoor, @Flag boolean ironDoor, @Flag boolean trapDoor, @Flag boolean fenceGate)
    {
        radius = radius == null ? 0 : radius;
        Vector3 vector;
        Set<Material> openMaterials = EnumSet.noneOf(Material.class);

        if(radius > this.basics.getConfiguration().commands.maxDoorRadius)
        {
            context.sendTranslated(NEGATIVE, "You can't use this with a radius over {amount}", this.basics.getConfiguration().commands.maxDoorRadius);
            return;
        }

        if(z == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "You has to specify a location!");
                return;
            }
            Location location = ((User) context).getLocation();
            world = location.getWorld();
            vector = new Vector3(location.getX(), location.getY(), location.getZ());
        }
        else
        {
            vector = new Vector3(x, y, z);
        }
        
        if(fenceGate)
        {
            openMaterials.add(Material.FENCE_GATE);
        }
        if(trapDoor)
        {
            openMaterials.add(Material.TRAP_DOOR);
        }
        if(ironDoor)
        {
            openMaterials.add(Material.IRON_DOOR_BLOCK);
        }
        if(woodenDoor || (openMaterials.isEmpty() && !all))
        {
            openMaterials.add(Material.WOODEN_DOOR);
        }

        Sphere sphere = new Sphere(vector, radius);
        for(Vector3 point : sphere)
        {
            Block block = world.getBlockAt((int) point.x, (int) point.y, (int) point.z);
            if(all || openMaterials.contains(block.getType()))
            {
                this.setOpen(block, state == DoorState.OPEN);
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
