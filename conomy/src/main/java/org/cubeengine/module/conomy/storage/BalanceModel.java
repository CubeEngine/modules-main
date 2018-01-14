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
package org.cubeengine.module.conomy.storage;

import java.math.BigDecimal;
import org.cubeengine.module.conomy.ConfigCurrency;
import org.cubeengine.module.sql.database.AsyncRecord;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;

public class BalanceModel extends AsyncRecord<BalanceModel>
{
    public BalanceModel()
    {
        super(TABLE_BALANCE);
    }

    public BalanceModel newBalance(AccountModel account, Currency currency, Context context, BigDecimal balance)
    {
        this.setValue(TABLE_BALANCE.ACCOUNT_ID, account.getValue(TABLE_ACCOUNT.ID));
        this.setValue(TABLE_BALANCE.CURRENCY, ((ConfigCurrency) currency).getCurrencyID());
        this.setValue(TABLE_BALANCE.CONTEXT, context.getType() + "|" + context.getName());
        this.setValue(TABLE_BALANCE.BALANCE, ((ConfigCurrency) currency).toLong(balance));
        return this;
    }

    public Long getBalance()
    {
        return getValue(TABLE_BALANCE.BALANCE);
    }

    public void setBalance(Long amount)
    {
        setValue(TABLE_BALANCE.BALANCE, amount);
        this.store();
    }

    public String getAccountID()
    {
        return getValue(TABLE_BALANCE.ACCOUNT_ID);
    }

    public String getCurrency()
    {
        return getValue(TABLE_BALANCE.CURRENCY);
    }

    public String getContext()
    {
        return getValue(TABLE_BALANCE.CONTEXT);
    }
}
