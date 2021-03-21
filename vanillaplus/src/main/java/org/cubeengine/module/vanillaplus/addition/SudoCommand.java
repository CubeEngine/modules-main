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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class SudoCommand
{
    private I18n i18n;

    @Inject
    public SudoCommand(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Makes a player send a message (including commands)")
    public void sudo(CommandCause context, ServerPlayer player, @Greedy String message) throws CommandException
    {
        if (!message.startsWith("/"))
        {
            player.simulateChat(Component.text(message), Sponge.server().causeStackManager().currentCause());
            i18n.send(context, POSITIVE, "Forced {user} to chat: {input#message}", player, message);
            return;
        }
        if (Sponge.server().commandManager().process(player, player, message.substring(1)).isSuccess())
        {
            i18n.send(context, POSITIVE, "Command {input#command} executed as {user}", message, player);
            return;
        }
        i18n.send(context, NEGATIVE, "Command was not executed successfully!");
    }
}
