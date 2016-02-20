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
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableBooleanData;
import org.spongepowered.api.data.value.BaseValue;

import static org.cubeengine.module.vanillaplus.fix.SafeLoginData.FLYMODE;

public class ImmutableSafeLoginData extends AbstractImmutableBooleanData<ImmutableSafeLoginData, SafeLoginData>
{
    public ImmutableSafeLoginData(boolean value)
    {
        super(value, FLYMODE, false);
    }

    @Override
    public SafeLoginData asMutable()
    {
        return new SafeLoginData(getValue());
    }

    @Override
    public <E> Optional<ImmutableSafeLoginData> with(Key<? extends BaseValue<E>> key, E value)
    {
        if (supports(key))
        {
            Optional.of(new ImmutableSafeLoginData(((Boolean)value)));
        }
        return Optional.empty();
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

    public DataContainer toContainer()
    {
        return super.toContainer().set(FLYMODE, getValue());
    }
}
