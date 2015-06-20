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

import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.basics.Basics;
import de.cubeisland.engine.module.core.util.math.Vector3;
import de.cubeisland.engine.module.core.util.math.shape.Sphere;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.user.User;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.manipulator.block.OpenData;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;

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
        Set<BlockType> openMaterials = new HashSet<>();

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
            world = (World)location.getExtent();
            vector = new Vector3(location.getX(), location.getY(), location.getZ());
        }
        else
        {
            vector = new Vector3(x, y, z);
        }
        
        if(fenceGate)
        {
            openMaterials.add(BlockTypes.FENCE_GATE);
        }
        if(trapDoor)
        {
            openMaterials.add(BlockTypes.TRAPDOOR);
            openMaterials.add(BlockTypes.IRON_TRAPDOOR);
        }
        if(ironDoor)
        {
            openMaterials.add(BlockTypes.IRON_DOOR);
        }
        if(woodenDoor || (openMaterials.isEmpty() && !all))
        {
            openMaterials.add(BlockTypes.WOODEN_DOOR); // TODO other wood doors
        }

        Sphere sphere = new Sphere(vector, radius);
        for(Vector3 point : sphere)
        {
            Location block = new Location(world, point.x, point.y, point.z);
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
    public boolean setOpen(Location block, boolean open)
    {
        if (block.isCompatible(OpenData.class))
        {
            return false;
        }

        boolean isOpen = block.getData(OpenData.class).isPresent();
        if (!open && isOpen)
        {
            block.remove(OpenData.class);
        }
        else if (open && !isOpen)
        {
            block.offer(block.getOrCreate(OpenData.class).get());
        }
        return true;
    }
}
