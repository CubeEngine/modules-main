package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.service.command.CommandContext;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

public class SudoCommand
{

    @Command(desc = "Makes a player send a message (including commands)")
    public void sudo(CommandContext context, User player, @Greed(INFINITE) String message)
    {
        if (!message.startsWith("/"))
        {
            player.chat(message);
            context.sendTranslated(POSITIVE, "Forced {user} to chat: {input#message}", player, message);
            return;
        }
        if (cm.runCommand(player, message.substring(1)))
        {
            context.sendTranslated(POSITIVE, "Command {input#command} executed as {user}", message, player);
            return;
        }
        context.sendTranslated(NEGATIVE, "Command was not executed successfully!");
    }
}
