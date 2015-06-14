package de.cubeisland.engine.module.vanillaplus;

import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

public class InventoryCommands
{
    private VanillaPlus module;

    public InventoryCommands(VanillaPlus module)
    {
        this.module = module;
    }

    @Command(alias = {"ci", "clear"}, desc = "Clears the inventory")
    @SuppressWarnings("deprecation")
    public void clearinventory(CommandContext context, @Default User player,
                               @Flag(longName = "removeArmor", name = "ra") boolean removeArmor,
                               @Flag boolean quiet,
                               @Flag boolean force)
    {
        //sender.sendTranslated(NEGATIVE, "That awkward moment when you realize you do not have an inventory!");
        CommandSender sender = context.getSource();
        if (!sender.equals(player))
        {
            context.ensurePermission(module.perms().COMMAND_CLEARINVENTORY_OTHER);
            if (module.perms().COMMAND_CLEARINVENTORY_PREVENT.isAuthorized(player)
                && !(force && module.perms().COMMAND_CLEARINVENTORY_FORCE.isAuthorized(sender)))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to clear the inventory of {user}", player);
                return;
            }
        }
        player.getInventory().clear();
        org.spongepowered.api.entity.player.User user = player.getOfflinePlayer();
        if (removeArmor)
        {
            user.setBoots(null);
            user.setLeggings(null);
            user.setChestplate(null);
            user.setHelmet(null);
        }
        if (sender.equals(player))
        {
            sender.sendTranslated(POSITIVE, "Your inventory has been cleared!");
            return;
        }
        if (module.perms().COMMAND_CLEARINVENTORY_NOTIFY.isAuthorized(player)) // notify
        {
            if (!(module.perms().COMMAND_CLEARINVENTORY_QUIET.isAuthorized(sender) && quiet)) // quiet
            {
                player.sendTranslated(NEUTRAL, "Your inventory has been cleared by {sender}!", sender);
            }
        }
        sender.sendTranslated(POSITIVE, "The inventory of {user} has been cleared!", player);
    }
}
