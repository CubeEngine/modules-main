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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.cubeengine.reflect.Reflector;
import org.cubeengine.module.conomy.command.EcoCommand;
import org.cubeengine.module.conomy.command.MoneyCommand;
import org.cubeengine.module.conomy.command.UniqueAccountParser;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.filesystem.FileExtensionFilter;
import org.cubeengine.libcube.service.i18n.I18n;
import org.jooq.Condition;
import org.jooq.Record4;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.user.UserStorageService;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;

public class ConomyService implements EconomyService, CatalogRegistryModule<Currency>
{
    private ConfigCurrency defaultCurrency;
    private List<ContextCalculator<Account>> contextCalculators = new ArrayList<>();

    protected Map<String, Account> accounts = new HashMap<>();
    private Map<String, ConfigCurrency> currencies = new HashMap<>();
    protected Conomy module;
    protected Database db;

    public ConomyService(Conomy module, ConomyConfiguration config, Path path, Database db, Reflector reflector)
    {
        this.module = module;
        this.db = db;
        try
        {
            for (Path file : Files.newDirectoryStream(path, FileExtensionFilter.YAML))
            {
                ConfigCurrency cc = new ConfigCurrency(reflector.load(CurrencyConfiguration.class, file.toFile()));
                currencies.put(cc.getCurrencyID(), cc);

                Sponge.getRegistry().registerModule(Currency.class, this);

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
            currencies.put(defaultCurrency.getCurrencyID(), defaultCurrency);
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
    public Optional<Currency> getById(String id)
    {
        return Optional.ofNullable(currencies.get(id));
    }

    @Override
    public Collection<Currency> getAll()
    {
        return new ArrayList<>(currencies.values());
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
    public boolean hasAccount(UUID uuid)
    {
        Account account = accounts.get(uuid.toString());
        if (account instanceof UniqueAccount)
        {
            return true;
        }
        return loadAccount(uuid, false).isPresent();
    }

    @Override
    public boolean hasAccount(String identifier)
    {
        try
        {
            return hasAccount(UUID.fromString(identifier));
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    @Override
    public Optional<UniqueAccount> getOrCreateAccount(UUID uuid)
    {
        Account account = accounts.get(uuid.toString());
        if (account instanceof UniqueAccount)
        {
            return Optional.of(((UniqueAccount) account));
        }
        if (account == null)
        {
            return loadAccount(uuid, true);
        }
        // else not UniqueAccount under uuid
        return Optional.empty();
    }

    @Override
    public Optional<Account> getOrCreateAccount(String identifier)
    {
        try
        {
            return getOrCreateAccount(UUID.fromString(identifier)).map(Account.class::cast);
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    protected Optional<UniqueAccount> loadAccount(UUID uuid, boolean create)
    {
        AccountModel model = loadModel(uuid.toString());
        if (model == null || !model.isUUID())
        {
            if (model != null)
            {
                model.deleteAsync();
            }
            if (!create)
            {
                return Optional.empty();
            }
            model = createModel(uuid);
        }
        UniqueAccount acc = new BaseAccount.Unique(this, model, db);
        accounts.put(uuid.toString(), acc);
        return Optional.of(acc);
    }

    private AccountModel createModel(UUID uuid)
    {
        Optional<User> user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(uuid);
        AccountModel model = db.getDSL().newRecord(TABLE_ACCOUNT).newAccount(uuid, user.map(User::getName).orElse(uuid.toString()), false, false);
        model.store();
        return model;
    }

    protected AccountModel loadModel(String id)
    {
        return db.getDSL().selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.ID.eq(id)).fetchOne();
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
        Condition userCond = TABLE_ACCOUNT.IS_UUID.eq(true);
        Condition bankCond = TABLE_ACCOUNT.IS_UUID.eq(false);
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
            where = where.and(TABLE_ACCOUNT.HIDDEN.eq(false));
        }
        return where.orderBy(TABLE_BALANCE.BALANCE.desc()).limit(fromRank - 1, toRank - fromRank + 1).fetchInto(BalanceModel.class);
    }

    public ConfigCurrency getCurrency(String currency)
    {
        return this.currencies.get(currency);
    }

    public void registerCommands(CommandManager cm, I18n i18n)
    {
        cm.getProviders().register(module, new UniqueAccountParser(module, this, i18n), BaseAccount.Unique.class);
        cm.addCommand(new MoneyCommand(cm, module, this, i18n));
        cm.addCommand(new EcoCommand(cm, this, i18n));
    }
}
