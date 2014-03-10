/**
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
package de.cubeisland.engine.rulebook.bookManagement;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.parameterized.Flag;
import de.cubeisland.engine.core.command.parameterized.Param;
import de.cubeisland.engine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.engine.core.command.reflected.Alias;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.permission.Permission;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.core.util.formatter.MessageType;
import de.cubeisland.engine.i18n.language.Language;
import de.cubeisland.engine.rulebook.Rulebook;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import static de.cubeisland.engine.core.permission.PermDefault.TRUE;

public class RulebookCommands extends ContainerCommand
{
    private final RulebookManager rulebookManager;
    private final Rulebook module;

    private final Permission getPermission;
    
    public RulebookCommands(Rulebook module)
    {
        super(module, "rulebook", "shows all commands of the rulebook module");
        this.rulebookManager = module.getRuleBookManager();
        this.module = module;
        this.getPermission = module.getBasePermission().childWildcard("command").childWildcard("get").child("other");
        this.module.getCore().getPermissionManager().registerPermission(module, getPermission);
    }

    @Alias( names = {"getrules", "rules"})
    @Command(
        desc = "gets the player the rulebook in the inventory",
        usage = "[language] [Player <name>]",
        params = @Param(names = {"player", "p"}, type = User.class),
        permDefault = TRUE,
        max = 1
    )
    public void getRuleBook(ParameterizedContext context)
    {
        if(!(context.getSender() instanceof User) && !context.hasParam("player"))
        {
            context.sendTranslated(MessageType.NEGATIVE, "The post office will give you your book!");
            return;
        }
        
        Locale locale;
        User user;
        if(context.hasParam("player"))
        {
            if(!getPermission.isAuthorized(context.getSender()))
            {
                context.sendTranslated(MessageType.NEGATIVE, "You do not have the permissions to add the rulebook to the inventory of an other player");
                return;
            }
            user = context.getParam("player");
            if(user == null)
            {
                context.sendTranslated(MessageType.NEGATIVE, "The given user was not found!");
                return;
            }
        }
        else
        {
            user = (User) context.getSender();
        }

        if(this.rulebookManager.getLocales().isEmpty())
        {
            context.sendTranslated(MessageType.NEUTRAL, "It does not exist a rulebook yet");
            return;
        }

        if(context.hasArg(0))
        {
            Language language = this.rulebookManager.getLanguage(context.getString(0));

            if(language == null)
            {
                context.sendTranslated(MessageType.NEGATIVE, "Can't match the language");
                return;
            }
            
            locale = language.getLocale();
            
            if(!this.rulebookManager.contains(locale))
            {
                context.sendTranslated(MessageType.NEUTRAL, "The language {name} is not supported yet.", language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
                return;
            }
        }
        else
        {
            locale = user.getLocale();
            if(!this.rulebookManager.contains(locale))
            {
                locale = this.module.getCore().getI18n().getDefaultLanguage().getLocale();
                if(!this.rulebookManager.contains(locale))
                {
                    locale = this.rulebookManager.getLocales().iterator().next();
                }
            }
        }

        TIntSet books = this.inventoryRulebookSearching(user.getInventory(), locale);

        TIntIterator iter = books.iterator();
        while(iter.hasNext())
        {
            user.getInventory().clear(iter.next());
        }

        user.getInventory().addItem(this.rulebookManager.getBook(locale));
        user.sendTranslated(MessageType.POSITIVE, "Lot's of fun with your rulebook.");
        if(!books.isEmpty())
        {
            user.sendTranslated(MessageType.POSITIVE, "Your old was removed");
        }
    }

    @Alias(names = "listrules")
    @Command(
        desc = "list all available languages of the rulebooks.",
        flags = {@Flag(longName = "supported", name = "s")},
        permDefault = TRUE,
        usage = "[-supported]",
        max = 0
    )
    public void list(ParameterizedContext context)
    {
        if(!context.hasFlag("s"))
        {
            if(this.rulebookManager.getLocales().isEmpty())
            {
                context.sendTranslated(MessageType.NEUTRAL, "No rulebook available at the moment");
            }
            else
            {
                context.sendTranslated(MessageType.NEUTRAL, "available languages:");
                for(Locale locale : this.rulebookManager.getLocales())
                {
                    context.sendMessage(ChatFormat.YELLOW + "* " + locale.getDisplayLanguage(context.getSender().getLocale()));
                }
            }
        }
        else
        {
            context.sendTranslated(MessageType.NEUTRAL, "supported languages:");
            for(Language language : this.getModule().getCore().getI18n().getLanguages())
            {
                context.sendMessage(ChatFormat.YELLOW +  "* " + language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
            }
        }
    }

    @Alias(names = "removerules")
    @Command(
        desc = "removes the declared language and languagefiles!",
        min = 1,
        max = 1,
        usage = "<language>"
    )
    public void remove(CommandContext context)
    {
        Language language = this.rulebookManager.getLanguage(context.getString(0));

        if(language == null)
        {
            context.sendTranslated(MessageType.NEGATIVE, "More than one or no language is matched with {input}", context.getString(0));
            return;
        }
        if(!this.rulebookManager.contains(language.getLocale()))
        {
            context.sendTranslated(MessageType.POSITIVE, "The languagefile of {input} doesn't exist at the moment", language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
            return;
        }
        
        try
        {
            this.rulebookManager.removeBook(language.getLocale());
            context.sendTranslated(MessageType.POSITIVE, "The languagefiles of {input} was deleted", language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
        }
        catch(IOException ex)
        {
            context.sendTranslated(MessageType.NEGATIVE, "The language file of {input} couldn't be deleted", language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
            this.getModule().getLog().error(ex, "Error when deleting the files!");
        }

    }

    @Alias(names = "modifyrules")
    @Command(
        desc = "modified the rulebook of the declared language with the book in hand",
        usage = "<language>",
        min = 1,
        max = 1
    )
    public void modify(CommandContext context)
    {
        if(!(context.getSender() instanceof User))
        {
            context.sendTranslated(MessageType.NEUTRAL, "You are not able to write, aren't you?");
        }
        User user = (User) context.getSender();

        ItemStack item = user.getItemInHand();

        if(!item.getType().equals(Material.WRITTEN_BOOK) && !item.getType().equals(Material.BOOK_AND_QUILL))
        {
            context.sendTranslated(MessageType.NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language language = this.rulebookManager.getLanguage(context.getString(0));
        if(language == null)
        {
            context.sendTranslated(MessageType.NEGATIVE, "More than one or no language is matched with {input}", context.getString(0));
            return;
        }
        Locale locale = language.getLocale();

        if(this.rulebookManager.contains(locale))
        {
            try
            {
                this.rulebookManager.removeBook(locale);
                this.rulebookManager.addBook(item, locale);
                context.sendTranslated(MessageType.POSITIVE, "The rulebook {name} was succesful modified.", locale
                    .getDisplayLanguage(context.getSender().getLocale()));
            }
            catch(IOException ex)
            {
                context.sendTranslated(MessageType.NEUTRAL, "An error ocurred by deleting the old rulebook");
                this.getModule().getLog().error(ex, "Error when deleting the files!");
            }
        }
        else
        {
            context.sendTranslated(MessageType.NEGATIVE, "You can't modify a book which does not exist.");
        }
    }

    @Alias(names = "addrules")
    @Command(
        desc = "adds the book in hand as rulebook of the declared language",
        min = 1,
        max = 1,
        usage = "<language>"
    )
    public void add(CommandContext context)
    {
        if(!(context.getSender() instanceof User))
        {
            context.sendTranslated(MessageType.NEUTRAL, "I thought you are an analphabet?");
        }
        User user = (User) context.getSender();

        ItemStack item = user.getItemInHand();

        if(!item.getType().equals(Material.WRITTEN_BOOK) && !item.getType().equals(Material.BOOK_AND_QUILL))
        {
            context.sendTranslated(MessageType.NEGATIVE, "I would try it with a book as item in hand");
            return;
        }

        Language language = this.rulebookManager.getLanguage(context.getString(0));
        if(language == null)
        {
            context.sendTranslated(MessageType.NEGATIVE, "More than one or no language is matched with {input}", context.getString(0));
            return;
        }
        Locale locale = language.getLocale();

        if(!this.rulebookManager.contains(locale))
        {
            this.rulebookManager.addBook(item, locale);
            context.sendTranslated(MessageType.POSITIVE, "Rulebook for the language {input} was added succesfully", language.getLocale().getDisplayLanguage(context.getSender().getLocale()));
        }
        else
        {
            context.sendTranslated(MessageType.NEUTRAL, "There is already a book with that language.");
        }
    }

    private TIntSet inventoryRulebookSearching(PlayerInventory inventory, Locale locale)
    {
        TIntSet books = new TIntHashSet();

        for(int i = 0; i < inventory.getSize(); i++)
        {
            ItemStack item = inventory.getItem(i);

            if(item != null && item.getType().equals(Material.WRITTEN_BOOK))
            {
                List<String> lore = item.getItemMeta().getLore();
                if(lore != null)
                {
                    if(lore.size() > 0 && locale.getLanguage().equalsIgnoreCase(lore.get(0)))
                    {
                        books.add(i);
                    }
                }
            }
        }
        return books;
    }
}
