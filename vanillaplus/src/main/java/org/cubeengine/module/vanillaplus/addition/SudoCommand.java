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
package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class SudoCommand
{
    private I18n i18n;
    private CommandManager cm;

    public SudoCommand(I18n i18n, CommandManager cm)
    {
        this.i18n = i18n;
        this.cm = cm;
    }

    @Command(desc = "Makes a player send a message (including commands)")
    public void sudo(CommandSource context, Player player, @Greed(INFINITE) String message)
    {
        if (!message.startsWith("/"))
        {
            player.getMessageChannel().send(player, Text.of(message));
            i18n.send(context, POSITIVE, "Forced {user} to chat: {input#message}", player, message);
            return;
        }
        if (cm.runCommand(player, message.substring(1)))
        {
            i18n.send(context, POSITIVE, "Command {input#command} executed as {user}", message, player);
            return;
        }
        i18n.send(context, NEGATIVE, "Command was not executed successfully!");
    }
}
