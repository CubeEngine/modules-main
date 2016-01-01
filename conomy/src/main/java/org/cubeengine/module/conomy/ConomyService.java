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
package org.cubeengine.module.conomy;

import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.filesystem.FileExtensionFilter;
import org.jooq.Condition;
import org.jooq.Record4;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.account.VirtualAccount;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.option.OptionSubjectData;
import org.spongepowered.api.service.user.UserStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;

public class ConomyService implements EconomyService
{
    private ConfigCurrency defaultCurrency;
    private List<ContextCalculator<Account>> contextCalculators = new ArrayList<>();

    private Map<UUID, UserAccount> userAccounts = new HashMap<>();
    private Map<String, BankAccount> bankAccounts = new HashMap<>();
    private Map<String, ConfigCurrency> currencies = new HashMap<>();
    private Conomy module;
    private Database db;

    public ConomyService(Conomy module, ConomyConfiguration config, Path path, Database db, Reflector reflector)
    {
        this.module = module;
        this.db = db;
        try
        {
            for (Path file : Files.newDirectoryStream(path, FileExtensionFilter.YAML))
            {
                String name = file.getFileName().toString();
                currencies.put(name, new ConfigCurrency(reflector.load(CurrencyConfiguration.class, file.toFile())));
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        defaultCurrency = currencies.get(config.defaultCurrency);
        if (defaultCurrency == null)
        {
            CurrencyConfiguration defConfig = reflector.load(CurrencyConfiguration.class, path.resolve(config.defaultCurrency + ".yml").toFile());
            defaultCurrency = new ConfigCurrency(defConfig);
            currencies.put(config.defaultCurrency, defaultCurrency);
        }

        /* TODO LOGGER
                this.logger = lf.getLog(Conomy.class, "Conomy-Transactions");
        this.logger.addTarget(new AsyncFileTarget(LoggingUtil.getLogFile(fm, "Conomy-Transactions"),
                                                  LoggingUtil.getFileFormat(true, false), true, LoggingUtil.getCycler(),
                                                  tf));
                                                  if (!this.module.getConfig().enableLogging)
        {
            logger.setLevel(LogLevel.NONE);
        }
         */
    }

    @Override
    public Currency getDefaultCurrency()
    {
        return defaultCurrency;
    }

    @Override
    public Set<Currency> getCurrencies()
    {
        return new HashSet<>(currencies.values());
    }

    @Override
    public Optional<UniqueAccount> getAccount(UUID uuid)
    {
        Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid);
        if (user.isPresent())
        {
            UserAccount userAccount = userAccounts.get(uuid);
            if (userAccount == null)
            {
                userAccount = loadAccount(uuid, user.get(), false);
            }
            return Optional.ofNullable(userAccount);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UniqueAccount> createAccount(UUID uuid)
    {
        Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid);
        if (user.isPresent())
        {
            Optional<UniqueAccount> account = getAccount(uuid);
            return account.isPresent() ? account : Optional.ofNullable(loadAccount(uuid, user.get(), true));
        }
        return Optional.empty();
    }

    private UserAccount loadAccount(UUID uuid, User user, boolean create)
    {
        AccountModel model = db.getDSL().selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.ID.eq(uuid.toString())).fetchOne();
        if (model == null)
        {
            if (!create)
            {
                return null;
            }
            model = db.getDSL().newRecord(TABLE_ACCOUNT).newAccount(uuid, user.getName(), false, false);
            model.storeAsync();
        }
        UserAccount acc = new UserAccount(user, this, model, db);
        this.userAccounts.put(uuid, acc);
        return acc;
    }

    @Override
    public Optional<Account> getAccount(String s)
    {
        BankAccount bankAccount = bankAccounts.get(s);
        if (bankAccount != null)
        {
            return Optional.of(bankAccount);
        }
        return Optional.ofNullable(loadAccount(s, false));
    }

    @Override
    public Optional<VirtualAccount> createVirtualAccount(String s)
    {
        Optional<Account> account = getAccount(s);
        return account.isPresent() ? Optional.of(((VirtualAccount) account.get()))
                                   : Optional.ofNullable(((VirtualAccount) loadAccount(s, true)));
    }

    private BaseAccount loadAccount(String name, boolean create)
    {
        AccountModel model = db.getDSL().selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.ID.eq(name)).fetchOne();
        if (model == null)
        {
            if (!create)
            {
                return null;
            }
            model = db.getDSL().newRecord(TABLE_ACCOUNT).newAccount(name, false, false);
            model.storeAsync();
        }
        if (model.isUUID())
        {
            Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(model.getUUID().get());
            if (user.isPresent())
            {
                UserAccount acc = new UserAccount(user.get(), this, model, db);
                this.userAccounts.put(model.getUUID().get(), acc);
                return acc;
            }
        }
        BankAccount acc = new BankAccount(name, this, model, db);
        this.bankAccounts.put(name, acc);
        return acc;
    }


    @Override
    public void registerContextCalculator(ContextCalculator<Account> contextCalculator)
    {
        this.contextCalculators.add(contextCalculator);
    }

    public Set<Context> getActiveContexts(BaseAccount baseAccount)
    {
        Set<Context> contexts = new HashSet<>();
        for (ContextCalculator<Account> calculator : contextCalculators)
        {
            calculator.accumulateContexts(baseAccount, contexts);
        }
        return contexts;
    }

    public Collection<BalanceModel> getTopBalance(boolean user, boolean bank, int fromRank, int toRank, boolean showHidden)
    {
        SelectJoinStep<Record4<String, String, String, Long>> from = db.getDSL()
                .select(TABLE_BALANCE.ACCOUNT_ID, TABLE_BALANCE.CURRENCY, TABLE_BALANCE.CONTEXT, TABLE_BALANCE.BALANCE)
                .from(TABLE_BALANCE.join(TABLE_ACCOUNT).on(TABLE_BALANCE.ACCOUNT_ID.eq(TABLE_ACCOUNT.ID)));
        Condition userCond = TABLE_ACCOUNT.MASK.bitAnd(((byte) 4)).eq(((byte) 4));
        Condition bankCond = TABLE_ACCOUNT.MASK.bitAnd(((byte) 4)).eq(((byte) 0));
        SelectConditionStep<Record4<String, String, String, Long>> where;
        if (!user && !bank)
        {
            throw new IllegalArgumentException();
        }
        if (user)
        {
            where = from.where(userCond);
        }
        else if (bank)
        {
            where = from.where(bankCond);
        }
        else
        {
            where = from.where(DSL.condition(true));
        }

        if (!showHidden)
        {
            where = where.and(TABLE_ACCOUNT.MASK.bitAnd((byte) 1).eq((byte) 0));
        }
        return where.orderBy(TABLE_BALANCE.BALANCE.desc()).limit(fromRank - 1, toRank - fromRank + 1).fetchInto(BalanceModel.class);
    }

    public ConfigCurrency getCurrency(String currency)
    {
        return this.currencies.get(currency);
    }

    public List<BankAccount> getBanks(Subject user, int level)
    {
        SubjectData data = user.getSubjectData();
        if (!(data instanceof OptionSubjectData))
        {
            return Collections.emptyList();
        }
        Map<String, String> options = ((OptionSubjectData) data).getOptions(user.getActiveContexts());

        List<String> manage = new ArrayList<>();
        List<String> withdraw = new ArrayList<>();
        List<String> deposit = new ArrayList<>();
        List<String> see = new ArrayList<>();
        options.entrySet().stream()
                .filter(e -> e.getKey().startsWith("conomy.bank.access-level"))
                .forEach(e ->
                {
                    String key = e.getKey().substring("conomy.bank.access-level".length());
                    switch (e.getValue())
                    {
                        case "0":
                            break;
                        case "1":
                            see.add(key);
                            break;
                        case "2":
                            deposit.add(key);
                            break;
                        case "3":
                            withdraw.add(key);
                            break;
                        case "4":
                            manage.add(key);
                            break;
                        default:
                            module.getProvided(Log.class).warn("Invalid value for option: {} -> {}", e.getKey(), e.getValue());
                            break;
                    }
                });
        if (level <= AccessLevel.WITHDRAW)
        {
            manage.addAll(withdraw);
        }
        if (level <= AccessLevel.DEPOSIT)
        {
            manage.addAll(deposit);
        }
        if (level <= AccessLevel.SEE)
        {
            manage.addAll(see);
        }

        List<BankAccount> collect = manage.stream().map(this::getAccount)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(a -> a instanceof BankAccount)
                .map(BankAccount.class::cast)
                .collect(Collectors.toList());

        if (level == AccessLevel.SEE && user.hasPermission(module.perms().ACCESS_SEE.getId()))
        {
            collect.addAll(getBankAccounts());
        }
        return collect;
    }

    private Collection<BankAccount> getBankAccounts()
    {
        // TODO load all banks
        return this.bankAccounts.values();
    }
}
