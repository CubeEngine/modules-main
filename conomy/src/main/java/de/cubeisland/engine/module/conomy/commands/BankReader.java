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
package de.cubeisland.engine.module.conomy.commands;

import de.cubeisland.engine.butler.CommandInvocation;
import de.cubeisland.engine.butler.parameter.reader.ArgumentReader;
import de.cubeisland.engine.butler.parameter.reader.ReaderException;
import de.cubeisland.engine.module.conomy.account.BankAccount;
import de.cubeisland.engine.module.conomy.account.ConomyManager;
import de.cubeisland.engine.service.i18n.I18n;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;

public class BankReader implements ArgumentReader<BankAccount>
{
    private final ConomyManager manager;
    private final I18n i18n;

    public BankReader(ConomyManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public BankAccount read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String arg = invocation.consume(1);
        BankAccount target = this.manager.getBankAccount(arg, false);
        if (target == null)
        {
            throw new ReaderException(i18n.translate(invocation.getLocale(), NEGATIVE, "There is no bank account named {input#name}!", arg));
        }
        return target;
    }
}
