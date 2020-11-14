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
package org.cubeengine.module.locker.config;

import java.util.Arrays;
import java.util.List;
import org.cubeengine.module.locker.data.ProtectedType;
import org.cubeengine.module.locker.data.ProtectionFlag;

public abstract class LockConfig<This extends LockConfig,T>
{
    protected final ProtectedType protectedType;
    protected boolean autoProtect = false;
    protected List<ProtectionFlag> defaultFlags;
    protected boolean enable = true;
    protected T type;

    public LockConfig(ProtectedType protectedType)
    {
        this.protectedType = protectedType;
    }

    public This autoProtect()
    {
        this.autoProtect = true;
        return (This)this;
    }

    public This defaultFlags(ProtectionFlag... flags)
    {
        this.defaultFlags = Arrays.asList(flags);
        return (This)this;
    }

    public boolean isType(T type)
    {
        return this.type.equals(type);
    }

    public abstract String getTitle();

    public T getType()
    {
        return type;
    }

    public short getFlags()
    {
        short result = 0;
        if (defaultFlags == null) return result;
        for (ProtectionFlag defaultFlag : defaultFlags)
        {
            result |= defaultFlag.flagValue;
        }
        return result;
    }

    public boolean isAutoProtect()
    {
        return autoProtect;
    }
}
