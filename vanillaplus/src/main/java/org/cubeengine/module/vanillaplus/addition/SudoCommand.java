package org.cubeengine.module.vanillaplus.addition;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

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
            player.getMessageChannel().send(Text.of(message));
            i18n.sendTranslated(context, POSITIVE, "Forced {user} to chat: {input#message}", player, message);
            return;
        }
        if (cm.runCommand(player, message.substring(1)))
        {
            i18n.sendTranslated(context, POSITIVE, "Command {input#command} executed as {user}", message, player);
            return;
        }
        i18n.sendTranslated(context, NEGATIVE, "Command was not executed successfully!");
    }
}
