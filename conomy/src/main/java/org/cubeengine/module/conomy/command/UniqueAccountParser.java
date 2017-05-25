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
package org.cubeengine.module.conomy.command;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.libcube.service.command.TranslatedParserException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class UniqueAccountParser implements ArgumentParser<BaseAccount.Unique>, DefaultValue<BaseAccount.Unique>, Completer
{
    private Conomy module;
    private final ConomyService service;
    private final I18n i18n;

    public UniqueAccountParser(Conomy module, ConomyService service, I18n i18n)
    {
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public BaseAccount.Unique parse(Class type, CommandInvocation invocation) throws ParserException
    {
        String arg = invocation.currentToken();
        User user = (User)invocation.getManager().read(User.class, User.class, invocation);
        Optional<BaseAccount.Unique> target = getAccount(user).filter(a -> {
                Object cmdSource = invocation.getCommandSource();
                return !(cmdSource instanceof Subject && a.isHidden()
                        && !((Subject) cmdSource).hasPermission(module.perms().ACCESS_SEE.getId()));
            });
        if (!target.isPresent())
        {
            throw new TranslatedParserException(i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE,
                    "No account found for {user}!", arg));
        }
        return target.get();
    }

    @Override
    public BaseAccount.Unique getDefault(CommandInvocation invocation)
    {
        if (!(invocation.getCommandSource() instanceof User))
        {
            throw new TranslatedParserException(i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE,
                    "You have to specify a user!"));
        }
        User user = (User) invocation.getCommandSource();
        Optional<BaseAccount.Unique> account = getAccount(user);
        if (!account.isPresent())
        {
            throw new TranslatedParserException(i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE,
                    "You have no account!"));
        }
        return account.get();
    }

    private Optional<BaseAccount.Unique> getAccount(User user)
    {
        return service.getOrCreateAccount(user.getUniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast);
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        return invocation.getManager().getCompleter(User.class).suggest(type, invocation);
    }
}
