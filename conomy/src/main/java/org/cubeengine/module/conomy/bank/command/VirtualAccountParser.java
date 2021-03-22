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
package org.cubeengine.module.conomy.bank.command;

import java.util.List;
import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.BaseAccount.Virtual;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

@Singleton
@ParserFor(Virtual.class)
public class VirtualAccountParser implements ValueParser<Virtual>, DefaultParameterProvider<Virtual>
{
    private final BankConomyService service;
    private final I18n i18n;

    @Inject
    public VirtualAccountParser(BankConomyService service, I18n i18n)
    {
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public Virtual apply(CommandCause commandCause)
    {
        if (!(commandCause.audience() instanceof ServerPlayer))
        {
            i18n.send(commandCause, NEGATIVE, "You have to specify a bank!");
            return null;
        }

        ServerPlayer user = (ServerPlayer) commandCause.audience();
        final List<Virtual> banks = service.getBanks(user, AccessLevel.SEE);
        if (banks.isEmpty())
        {
            i18n.send(commandCause, NEGATIVE, "You have no banks available!");
            return null;
        }
        return banks.get(0);
    }

    @Override
    public Optional<? extends Virtual> parseValue(Key<? super Virtual> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final String arg = reader.parseString();
        Optional<BaseAccount.Virtual> target = Optional.empty();
        if (service.hasAccount(arg))
        {
            target = service.accountOrCreate(arg).filter(a -> a instanceof BaseAccount.Virtual).map(BaseAccount.Virtual.class::cast);
        }
        if (!target.isPresent())
        {
            throw reader.createException(i18n.translate(context.cause(), NEGATIVE, "There is no bank account named {input#name}!", arg));
        }
        return target;
    }


}
