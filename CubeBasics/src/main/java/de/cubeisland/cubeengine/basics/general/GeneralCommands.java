package de.cubeisland.cubeengine.basics.general;

import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.annotation.Command;
import static de.cubeisland.cubeengine.core.command.exception.InvalidUsageException.invalidUsage;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.user.UserManager;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 *
 * @author Anselm Brehme
 */
public class GeneralCommands
{
    
    private UserManager cuManager;

    public GeneralCommands(Basics module)
    {
        this.cuManager = module.getUserManager();
    }
  
    @Command(
    desc = "Allows you to emote",
    min = 1,
    usage = "/me <message>")
    public void me(CommandContext context)
    {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (context.hasIndexed(i))
        {
            sb.append(context.getString(i++));
        }
        context.getSender().getServer().broadcastMessage('*' + context.getSender().getName() + " " + sb.toString());
    }
    
    @Command(
    desc = "Sends a private message to someone",
    names={"msg","tell","pn","m","t","whisper"},
    min = 1,
    usage = "/msg <player> <message>")
    public void msg(CommandContext context)
    {
        User sender = cuManager.getUser(context.getSender());
        User user = context.getUser(0);
        if (user == null)
        {
            invalidUsage(context.getSender(), "core", "&cThe User %s does not exist!", context.getString(0));
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (context.hasIndexed(i))
        {
            sb.append(context.getString(i++));
        }
        sender.sendMessage("basics","You -> %s &s", user.getName(), sb.toString());
        user.sendMessage("basics","%s -> You %s", sender.getName(), sb.toString());
        //TODO save last whispered to so i can implement /reply
    }
    
    @Command(
    desc = "Shows when given player was online the last time",
    min = 1,
    max= 1,
    usage = "/seen <player>")
    public void seen(CommandContext context)
    {
        User sender = cuManager.getUser(context.getSender());
        User user = context.getUser(0);
        if (user == null)
        {
            invalidUsage(context.getSender(), "core", "&cThe User %s does not exist!", context.getString(0));
        }
        long lastPlayed = user.getLastPlayed();
        //TODO ausgabe;       
    }
    
        
    @Command(
    desc = "Kills yourself",
    max= 0,
    usage = "/suicide")
    public void suicide(CommandContext context)
    {
        User sender = cuManager.getUser(context.getSender());
        if (sender == null)
        {
            invalidUsage(context.getSender(), "basics", "&cYou want to kill yourself? &aThe command for that is stop!");
        }
        sender.setHealth(0);
        sender.setLastDamageCause(new EntityDamageEvent(sender, EntityDamageEvent.DamageCause.CUSTOM, 20));
        //TODO msg;
    }
}
