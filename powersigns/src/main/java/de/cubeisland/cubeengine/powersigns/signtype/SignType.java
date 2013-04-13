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
package de.cubeisland.cubeengine.powersigns.signtype;

import java.util.Map;

import org.bukkit.Location;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.powersigns.PowerSign;
import de.cubeisland.cubeengine.powersigns.Powersigns;
import de.cubeisland.cubeengine.powersigns.SignManager;

import gnu.trove.map.hash.THashMap;

public abstract class SignType<T extends SignType, I extends SignTypeInfo>
{
    private Map<String,String> lowerCasedNames = new THashMap<String, String>();
    protected SignManager manager;
    protected Powersigns module;

    public SignType(String... names)
    {
        for (String name : names)
        {
            lowerCasedNames.put(name.toLowerCase(),name);
        }
    }

    public void init(Powersigns module)
    {
        this.module = module;
        this.manager = module.getManager();
    }

    /**
     * Gets the unique PSID (PowerSign-ID)
     *
     * @return
     */
    public abstract String getPSID();

    public abstract boolean onSignLeftClick(User user, PowerSign<T,I> sign);
    public abstract boolean onSignRightClick(User user, PowerSign<T,I> sign);

    public abstract boolean onSignShiftRightClick(User user, PowerSign<T,I> sign);
    public abstract boolean onSignShiftLeftClick(User user, PowerSign<T,I> sign);

    public Map<String, String> getNames()
    {
        return this.lowerCasedNames;
    }

    public abstract I createInfo(User user, Location location, String line1, String id, String line3, String line4);
}
