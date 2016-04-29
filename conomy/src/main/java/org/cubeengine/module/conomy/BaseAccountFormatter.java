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
package org.cubeengine.module.conomy;

import org.cubeengine.dirigent.Component;
import org.cubeengine.dirigent.formatter.Context;
import org.cubeengine.dirigent.formatter.reflected.Format;
import org.cubeengine.dirigent.formatter.reflected.Names;
import org.cubeengine.dirigent.formatter.reflected.ReflectedFormatter;
import org.cubeengine.libcube.service.i18n.formatter.component.StyledComponent;
import org.cubeengine.libcube.service.i18n.formatter.component.TextComponent;

import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;

@Names("account")
public class BaseAccountFormatter extends ReflectedFormatter
{
    @Format
    public Component format(BaseAccount.Unique userAcc, Context context)
    {
        return new StyledComponent(DARK_GREEN, new TextComponent(userAcc.getDisplayName()));
    }

    @Format
    public Component format(BaseAccount.Virtual bankAcc, Context context)
    {
        return new StyledComponent(GOLD, new TextComponent(bankAcc.getDisplayName()));
    }

}
