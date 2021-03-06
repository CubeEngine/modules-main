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
package org.cubeengine.module.portals;

public class PortalsAttachment
{
    private long lastIn = 0;
    private boolean isInPortal = false;
    private Portal portal;
    private boolean debug = false;

    public Portal getPortal()
    {
        return portal;
    }

    public void setPortal(Portal portal)
    {
        this.portal = portal;
    }

    public boolean isInPortal()
    {
        return isInPortal;
    }

    public void setInPortal(boolean isInPortal)
    {
        if (isInPortal)
        {
            lastIn = System.currentTimeMillis();
        }
        else
        {
            if (lastIn + 1000 > System.currentTimeMillis())
            {
                return;
            }
        }
        this.isInPortal = isInPortal;
    }

    public void toggleDebug()
    {
        this.debug = !this.debug;
    }

    public boolean isDebug()
    {
        return debug;
    }
}
