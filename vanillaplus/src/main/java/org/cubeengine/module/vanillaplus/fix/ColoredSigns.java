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

import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.event.block.tileentity.SignChangeEvent;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

public class ColoredSigns
{
    private final Basics module;

    public ColoredSigns(Basics module)
    {
        this.module = module;
    }

    @Subscribe
    public void onSignChange(SignChangeEvent event)
    {
        if (!(event.getCause().orNull() instanceof Subject))
        {
            return;
        }
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
