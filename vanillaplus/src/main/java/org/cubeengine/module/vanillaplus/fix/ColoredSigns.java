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
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.block.tileentity.SignChangeEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

public class ColoredSigns
{
    private final VanillaPlus module;

    public ColoredSigns(VanillaPlus module)
    {
        this.module = module;
    }

    public final PermissionDescription SIGN_COLORED = getBasePerm().childWildcard("sign").child("colored");
    public final PermissionDescription SIGN_STYLED = getBasePerm().childWildcard("sign").child("styled");

    @Listener
    public void onSignChange(ChangeSignEvent event, @First Subject cause)
    {
        SIGN_STYLED
        if (module.perms().SIGN_COLORED.isAuthorized((Subject)event.getCause().get())) // ALL colors
        {
            SignData newData = event.getNewData();
            ListValue<Text> lines = newData.lines();
            for (int i = 0; i < lines.size(); i++)
            {
                final Text text = lines.get(i);
                lines.set(i, Texts.legacy().fromUnchecked(Texts.toPlain(text)));
            }
            event.setNewData(newData);
        }
    }
}
