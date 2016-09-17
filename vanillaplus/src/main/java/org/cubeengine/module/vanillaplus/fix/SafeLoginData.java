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
package org.cubeengine.module.vanillaplus.fix;

import java.util.Optional;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractBooleanData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

public class SafeLoginData extends AbstractBooleanData<SafeLoginData, ImmutableSafeLoginData>
{

    public final static Key<Value<Boolean>> FLYMODE = KeyFactory.makeSingleKey(
            new TypeToken<Boolean>() {},
            new TypeToken<Value<Boolean>>() {},
            DataQuery.of("flymode"),
            "cubeengine:vanillaplus:flymode", "Flymode on Login");

    public SafeLoginData(Boolean value)
    {
        super(value, FLYMODE, false);
    }

    @Override
    public ImmutableSafeLoginData asImmutable()
    {
        return new ImmutableSafeLoginData(getValue());
    }

    @Override
    public Optional<SafeLoginData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<Boolean> flymode = dataHolder.get(FLYMODE);
        if (flymode.isPresent())
        {
            SafeLoginData data = new SafeLoginData(flymode.get());
            overlap.merge(this, data);
            if (data != this)
            {
                this.setValue(flymode.get());
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SafeLoginData> from(DataContainer container)
    {
        Optional<Boolean> flymode = container.getBoolean(FLYMODE.getQuery());
        if (flymode.isPresent())
        {
            this.setValue(flymode.get());
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public SafeLoginData copy()
    {
        return new SafeLoginData(getValue());
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
