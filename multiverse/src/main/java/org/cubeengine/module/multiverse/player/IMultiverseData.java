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
package org.cubeengine.module.multiverse.player;

import java.util.Map;
import org.spongepowered.api.data.DataContainer;

public interface IMultiverseData
{
    Map<String, DataContainer> getPlayerData();
    String getCurrentUniverse();

    static <E extends IMultiverseData> int compareTo(E o1, E o2)
    {
        int compare = o1.getCurrentUniverse().compareTo(o2.getCurrentUniverse());
        if (compare != 0)
        {
            return compare;
        }

        compare = Integer.compare(o1.getPlayerData().size(), o2.getPlayerData().size());
        if (compare != 0)
        {
            return compare;
        }

        compare = Boolean.compare(o1.getPlayerData().entrySet().containsAll(o2.getPlayerData().entrySet()),
                                  o2.getPlayerData().entrySet().containsAll(o1.getPlayerData().entrySet()));
        if (compare != 0)
        {
            return compare;
        }

        return 0;
    }
}
