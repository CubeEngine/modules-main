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
import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.BaseAccount.Unique;
import org.cubeengine.module.conomy.ConomyService;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.command.parameter.managed.standard.ResourceKeyedValueParameters;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

@Singleton
@ParserFor(Unique.class)
public class UniqueAccountParser implements ValueParser<Unique>, DefaultParameterProvider<Unique>, ValueCompleter
{
    private final ConomyService service;
    private final I18n i18n;

    @Inject
    public UniqueAccountParser(ConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public Unique apply(CommandCause commandCause)
    {
        if (!(commandCause.audience() instanceof ServerPlayer))
        {
            i18n.send(commandCause, NEGATIVE,  "You have to specify a user!");
            return null;
        }
        ServerPlayer user = (ServerPlayer) commandCause.audience();
        Optional<BaseAccount.Unique> account = getAccount(user.uniqueId());
        if (!account.isPresent())
        {
            i18n.send(commandCause, NEGATIVE, "You have no account!");
            return null;
        }
        return account.get();
    }

    @Override
    public List<CommandCompletion> complete(CommandContext context, String currentInput)
    {
        return ResourceKeyedValueParameters.USER.get().complete(context, currentInput);
    }

    @Override
    public Optional<? extends Unique> parseValue(Key<? super Unique> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final String arg = reader.parseString();
        final Optional<Unique> account = ResourceKeyedValueParameters.USER.get().parseValue(Parameter.key(parameterKey.key(), UUID.class), reader, context).flatMap(
            user -> getAccount(user).filter(a -> {
                CommandCause cmdSource = context.cause();
                return !a.isHidden() && !service.getPerms().ACCESS_SEE.check(cmdSource);
            }));
        if (!account.isPresent())
        {
            throw reader.createException(i18n.translate(context.cause(), NEGATIVE, "No account found for {user}!", arg));
        }
        return account;
    }

    private Optional<BaseAccount.Unique> getAccount(UUID user)
    {
        return service.findOrCreateAccount(user)
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast);
    }

}
