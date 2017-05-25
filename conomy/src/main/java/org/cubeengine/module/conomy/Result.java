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
package org.cubeengine.module.conomy;

import java.math.BigDecimal;
import java.util.Set;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.economy.EconomyTransactionEvent;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransferResult;

public class Result implements TransactionResult
{
    private Account account;
    private Currency currency;
    private BigDecimal amount;
    private Set<Context> contexts;
    private ResultType result;
    private TransactionType type;

    public Result(Account account, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, TransactionType type, Cause cause)
    {
        this.account = account;
        this.currency = currency;
        this.amount = amount;
        this.contexts = contexts;
        this.result = result;
        this.type = type;

        Sponge.getEventManager().post(new ResultEvent(cause, this));
    }

    @Override
    public Account getAccount()
    {
        return this.account;
    }

    @Override
    public Currency getCurrency()
    {
        return this.currency;
    }

    @Override
    public BigDecimal getAmount()
    {
        return amount;
    }

    @Override
    public Set<Context> getContexts()
    {
        return contexts;
    }

    @Override
    public ResultType getResult()
    {
        return result;
    }

    @Override
    public TransactionType getType()
    {
        return type;
    }

    public static class Transfer extends Result implements TransferResult
    {
        private Account accountTo;

        public Transfer(Account account, Account accountTo, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, TransactionType type, Cause cause)
        {
            super(account, currency, amount, contexts, result, type, cause);
            this.accountTo = accountTo;
        }

        @Override
        public Account getAccountTo()
        {
            return this.accountTo;
        }
    }

    public static class ResultEvent implements EconomyTransactionEvent
    {
        private Cause cause;
        private TransactionResult transaction;

        public ResultEvent(Cause cause, TransactionResult transaction)
        {
            this.cause = cause;
            this.transaction = transaction;
        }

        @Override
        public TransactionResult getTransactionResult()
        {
            return transaction;
        }

        @Override
        public Cause getCause()
        {
            return cause;
        }
    }
}
