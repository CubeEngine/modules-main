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
package org.cubeengine.module.vanillaplus.fix;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.First;

public class ColoredSigns extends PermissionContainer
{
    public ColoredSigns(PermissionManager pm)
    {
        super(pm, VanillaPlus.class);
    }

    public final Permission SIGN_COLORED = register("sign.colored", "", null);
    public final Permission SIGN_STYLED = register("sign.styled", "", null);

    @Listener
    public void onSignChange(ChangeSignEvent event, @First ServerPlayer cause)
    {
        final PlainComponentSerializer plain = PlainComponentSerializer.plain();
        final List<Component> lines = event.getText().get();
        if (!cause.hasPermission(SIGN_STYLED.getId()))
        {
            for (int i = 0; i < lines.size(); i++)
            {
                lines.set(i, Component.text(plain.serialize(lines.get(i)).replaceAll("&[klmno]", "")));
            }
        }
        if (!cause.hasPermission(SIGN_COLORED.getId()))
        {
            for (int i = 0; i < lines.size(); i++)
            {
                lines.set(i, Component.text(plain.serialize(lines.get(i)).replaceAll("&[0123456789abcdef]", "")));
            }
        }

        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, LegacyComponentSerializer.legacyAmpersand().deserialize(plain.serialize(lines.get(i))));
        }
    }
}
