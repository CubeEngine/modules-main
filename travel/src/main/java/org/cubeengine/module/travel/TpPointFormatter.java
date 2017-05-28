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
package org.cubeengine.module.travel;

import org.cubeengine.dirigent.context.Context;
import org.cubeengine.dirigent.formatter.AbstractFormatter;
import org.cubeengine.dirigent.formatter.argument.Arguments;
import org.cubeengine.dirigent.parser.component.Component;
import org.cubeengine.module.travel.config.Home;
import org.cubeengine.module.travel.config.TeleportPoint;
import org.cubeengine.libcube.service.i18n.I18n;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.libcube.service.i18n.formatter.component.ClickComponent.runCommand;
import static org.cubeengine.libcube.service.i18n.formatter.component.HoverComponent.hoverText;
import static org.cubeengine.libcube.service.i18n.formatter.component.StyledComponent.styled;
import static org.spongepowered.api.text.format.TextStyles.UNDERLINE;

public class TpPointFormatter extends AbstractFormatter<TeleportPoint>
{
    private I18n i18n;

    public TpPointFormatter(I18n i18n)
    {
        super("tppoint");
        this.i18n = i18n;
    }

    @Override
    public Component format(TeleportPoint object, Context context, Arguments args)
    {
        String cmd = "/" + (object instanceof Home ? "home" : "warp") + " tp " + object.name + " " + object.getOwner().getName();
        return styled(UNDERLINE, runCommand(cmd, hoverText(i18n.translate(context, NONE, "Click to teleport to {}", object.name), object.getOwner().getName())));
    }
}
