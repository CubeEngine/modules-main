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
import java.util.function.Supplier;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
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

    public Result(Account account, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, Supplier<TransactionType> type)
    {
        this.account = account;
        this.currency = currency;
        this.amount = amount;
        this.contexts = contexts;
        this.result = result;
        this.type = type.get();

        Sponge.eventManager().post(new ResultEvent( this));
    }

    @Override
    public Account account()
    {
        return this.account;
    }

    @Override
    public Currency currency()
    {
        return this.currency;
    }

    @Override
    public BigDecimal amount()
    {
        return amount;
    }

    @Override
    public Set<Context> contexts()
    {
        return contexts;
    }

    @Override
    public ResultType result()
    {
        return result;
    }

    @Override
    public TransactionType type()
    {
        return type;
    }

    public static class Transfer extends Result implements TransferResult
    {
        private Account accountTo;

        public Transfer(Account account, Account accountTo, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, Supplier<TransactionType> type)
        {
            super(account, currency, amount, contexts, result, type);
            this.accountTo = accountTo;
        }

        @Override
        public Account accountTo()
        {
            return this.accountTo;
        }
    }

    public static class ResultEvent implements EconomyTransactionEvent
    {
        private Cause cause;
        private TransactionResult transaction;

        public ResultEvent(TransactionResult transaction)
        {
            this.cause = Sponge.server().causeStackManager().currentCause();
            this.transaction = transaction;
        }

        @Override
        public TransactionResult transactionResult()
        {
            return transaction;
        }

        @Override
        public Cause cause()
        {
            return cause;
        }
    }
}
