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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.cubeengine.module.conomy.bank.BankConomyService;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.module.sql.database.Database;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import static java.util.stream.Collectors.toMap;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;
import static org.spongepowered.api.service.economy.transaction.ResultType.*;
import static org.spongepowered.api.service.economy.transaction.TransactionTypes.*;

public abstract class BaseAccount implements Account
{
    // TODO parse currency // TODO filter currency Names / Symbols http://git.cubeisland.de/cubeengine/cubeengine/issues/250
    // rename bank / player(on name change)

    private ConomyService service;
    protected AccountModel account;
    private Database db;
    private Map<Currency, Map<Context, BalanceModel>> balance = new HashMap<>();

    private final Component display;
    private final String id;

    public BaseAccount(ConomyService service, AccountModel account, Database db)
    {
        this.service = service;
        this.account = account;
        this.db = db;
        this.display = Component.text(account.getName());
        this.id = account.getID();
    }

    @Override
    public boolean hasBalance(Currency currency, Cause cause)
    {
        return this.hasBalance(currency, Collections.emptySet());
    }

    @Override
    public BigDecimal balance(Currency currency, Cause cause)
    {
        return this.balance(currency, Collections.emptySet());
    }

    @Override
    public Map<Currency, BigDecimal> balances(Cause cause)
    {
        return this.balances(Collections.emptySet());
    }

    @Override
    public TransactionResult setBalance(Currency currency, BigDecimal amount, Cause cause)
    {
        return this.setBalance(currency, amount, Collections.emptySet());
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(Cause cause)
    {
        return this.resetBalances(Collections.emptySet());
    }

    @Override
    public TransactionResult resetBalance(Currency currency, Cause cause)
    {
        return this.resetBalance(currency, Collections.emptySet());
    }

    @Override
    public TransactionResult deposit(Currency currency, BigDecimal amount, Cause cause)
    {
        return this.deposit(currency, amount, Collections.emptySet());
    }

    @Override
    public TransactionResult withdraw(Currency currency, BigDecimal amount, Cause cause)
    {
        return this.withdraw(currency, amount, Collections.emptySet());
    }

    @Override
    public TransferResult transfer(Account to, Currency currency, BigDecimal amount, Cause cause)
    {
        return this.transfer(to, currency, amount, Collections.emptySet());
    }

    private Optional<BalanceModel> getModel(ConfigCurrency currency, Set<Context> ctxs)
    {
        Map<Context, BalanceModel> balances = this.balance.get(currency);
        if (balances == null)
        {
            balances = new HashMap<>();
            balance.put(currency, balances);
        }
        Optional<Context> relevantCtx = currency.getRelevantContext(ctxs);
        if (!relevantCtx.isPresent())
        {
            return Optional.empty();
        }
        BalanceModel balanceModel = balances.get(relevantCtx.get());
        if (balanceModel == null)
        {
            balanceModel = db.getDSL().selectFrom(TABLE_BALANCE).where(TABLE_BALANCE.ACCOUNT_ID.eq(account.getID()))
                    .and(TABLE_BALANCE.CONTEXT.eq(relevantCtx.get().getKey() + "|" + relevantCtx.get().getValue()))
                    .and(TABLE_BALANCE.CURRENCY.eq(currency.getCurrencyID())).fetchOne();
            if (balanceModel == null)
            {
                balanceModel = db.getDSL().newRecord(TABLE_BALANCE).newBalance(account, currency, relevantCtx.get(), defaultBalance(currency));
                balanceModel.store();
                balances.put(relevantCtx.get(), balanceModel);
            }
        }

        return Optional.of(balanceModel);
    }

    private ConfigCurrency cast(Currency currency)
    {
        if (currency instanceof ConfigCurrency)
        {
            return (ConfigCurrency) currency;
        }
        throw new IllegalArgumentException("Unknown Currency: " + currency.displayName() + " " + currency.getClass().getName());
    }

    @Override
    public BigDecimal defaultBalance(Currency currency)
    {
        return ((ConfigCurrency) currency).getDefaultBalance(this);
    }

    @Override
    public boolean hasBalance(Currency currency, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        return getModel(cur, contexts).isPresent();
    }

    @Override
    public BigDecimal balance(Currency currency, Set<Context> contexts)
    {
        // TODO javadocs are unclear what to do if currency is not found in context
        // If no Balance available then ZERO
        ConfigCurrency cur = cast(currency);
        return getModel(cur, contexts)
                .map(BalanceModel::getBalance)
                .map(cur::fromLong)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<Currency, BigDecimal> balances(Set<Context> ctxs)
    {
        return service.getCurrencies().stream()
                .collect(toMap(c -> c, c -> balance(c, ctxs)));
    }

    @Override
    public TransactionResult setBalance(Currency currency, BigDecimal amount, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            model.get().setBalance(cur.toLong(amount));
            return new Result(this, currency, amount, contexts, SUCCESS, TRANSFER); // TODO TransactionType?
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, TRANSFER);  // TODO TransactionType?
    }

    @Override
    public Map<Currency, TransactionResult> resetBalances(Set<Context> contexts)
    {
        return service.getCurrencies().stream().collect(toMap(c -> c, c -> resetBalance(c, contexts)));
    }

    @Override
    public TransactionResult resetBalance(Currency currency, Set<Context> contexts)
    {
        return setBalance(currency, defaultBalance(currency), contexts);
    }

    @Override
    public TransactionResult deposit(Currency currency, BigDecimal amount, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            model.get().setBalance(model.get().getBalance() + cur.toLong(amount));
            // TODO check max
            return new Result(this, currency, amount, contexts, SUCCESS, DEPOSIT);
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, DEPOSIT);
    }

    @Override
    public TransactionResult withdraw(Currency currency, BigDecimal amount, Set<Context> contexts)
    {
        ConfigCurrency cur = cast(currency);
        Optional<BalanceModel> model = getModel(cur, contexts);
        if (model.isPresent())
        {
            long newBalance = model.get().getBalance() - cur.toLong(amount);
            long min = cur.toLong(cur.getMin(this));

            if (newBalance < min)
            {
                return new Result(this, currency, amount, contexts, ACCOUNT_NO_FUNDS, WITHDRAW);
            }
            model.get().setBalance(newBalance);
            return new Result(this, currency, amount, contexts, SUCCESS, WITHDRAW);
        }
        return new Result(this, currency, amount, contexts, CONTEXT_MISMATCH, WITHDRAW);
    }

    @Override
    public TransferResult transfer(Account to, Currency currency, BigDecimal amount, Set<Context> contexts)
    {
        if (this == to)
        {
            return new Result.Transfer(this, to, currency, amount, contexts, ResultType.SUCCESS, TRANSFER);
        }
        // TODO disallow user -> bank if not at least member of bank
        // TODO disallow bank -> user if cause is not admin?
        // TODO check for visibility of account for causer
        TransactionResult result = this.withdraw(currency, amount, contexts);
        if (result.result() == SUCCESS)
        {
            result = to.deposit(currency, amount, contexts);
            if (result.result() == SUCCESS)
            {
                return new Result.Transfer(this, to, currency, amount, contexts, SUCCESS, TRANSFER);
            }
            else
            {
                this.deposit(currency, amount, contexts); // rollback withdraw action
                return new Result.Transfer(this, to, currency, amount, contexts, result.result(), TRANSFER);
            }
        }
        else
        {
            return new Result.Transfer(this, to, currency, amount, contexts, result.result(), TRANSFER);
        }
    }

    public boolean isHidden()
    {
        return account.isHidden();
    }

    public void setHidden(boolean b)
    {
        account.setHidden(b);
    }

    @Override
    public Component displayName()
    {
        return display;
    }

    @Override
    public String identifier()
    {
        return id;
    }

    public static class Unique extends BaseAccount implements UniqueAccount
    {
        private final UUID uuid;

        public Unique(ConomyService service, AccountModel account, Database db)
        {
            super(service, account, db);
            this.uuid = account.getUUID().get();
        }

        @Override
        public UUID uniqueId()
        {
            return uuid;
        }
    }

    public static class Virtual extends BaseAccount implements Account
    {
        public Virtual(BankConomyService service, AccountModel model, Database db)
        {
            super(service, model, db);
        }


        public boolean isInvite()
        {
            return account.isInvite();
        }

        public void setInvite(boolean invite)
        {
            if (!invite)
            {
                account.setHidden(false);
            }
            account.setInvite(invite);
        }

        public boolean rename(String newName)
        {
            // TODO check for name
            return account.setName(newName);
        }
    }

}
