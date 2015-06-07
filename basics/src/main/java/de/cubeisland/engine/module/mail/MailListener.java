package de.cubeisland.engine.module.mail;

import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.basics.BasicsUser;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.event.Order.POST;

public class MailListener
{
    private Module module;
    private UserManager um;

    public MailListener(Module module, UserManager um)
    {
        this.module = module;
        this.um = um;
    }

    @Subscribe(order = POST)
    public void onAfterJoin(PlayerJoinEvent event)
    {
        User user = um.getExactUser(event.getUser().getUniqueId());
        MailAttachment mails = user.attachOrGet(MailAttachment.class, module);
        int amount = mails.countMail();
        if (amount > 0)
        {
            user.sendTranslatedN(POSITIVE, amount, "You have a new mail!", "You have {amount} of mail!", amount);
            user.sendTranslated(NEUTRAL, "Use {text:/mail read} to display them.");
        }
    }
}
