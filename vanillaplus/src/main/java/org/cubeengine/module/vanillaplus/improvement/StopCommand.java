package org.cubeengine.module.vanillaplus.improvement;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.core.util.ChatFormat;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;

public class StopCommand
{

    @Command(alias = {"shutdown", "killserver", "quit"}, desc = "Shuts down the server")
    public void stop(CommandSender context, @Optional @Greed(INFINITE) String message)
    {
        if (message == null || message.isEmpty())
        {
            message = "";
            // TODO message = this.core.getGame().getServer().getShutdownMessage();
        }
        message = ChatFormat.parseFormats(message);

        game.getServer().shutdown(ChatFormat.fromLegacy(message, '&'));
    }
}