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

import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class ColoredSigns extends PermissionContainer<VanillaPlus>
{
    public ColoredSigns(VanillaPlus module)
    {
        super(module);
    }

    public final PermissionDescription SIGN_COLORED = register("sign.colored", "", null);
    public final PermissionDescription SIGN_STYLED = register("sign.styled", "", null);

    @Listener
    public void onSignChange(ChangeSignEvent event, @First Subject cause)
    {
        ListValue<Text> lines = event.getText().lines();
        if (!cause.hasPermission(SIGN_STYLED.getId()))
        {
            for (int i = 0; i < lines.size(); i++)
            {
                lines.set(i, Text.of(lines.get(i).toPlain().replaceAll("&[klmno]", "")));
            }
        }
        if (cause.hasPermission(SIGN_COLORED.getId()))
        {
            for (int i = 0; i < lines.size(); i++)
            {
                lines.set(i, Text.of(lines.get(i).toPlain().replaceAll("&[0123456789abcdef]", "")));
            }
        }

        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, TextSerializers.FORMATTING_CODE.deserialize(lines.get(i).toPlain()));
        }
    }
}
