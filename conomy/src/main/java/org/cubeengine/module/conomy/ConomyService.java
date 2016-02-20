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
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.conomy.command.EcoCommand;
import org.cubeengine.module.conomy.command.MoneyCommand;
import org.cubeengine.module.conomy.command.UniqueAccountReader;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.filesystem.FileExtensionFilter;
import org.cubeengine.service.i18n.I18n;
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
import org.spongepowered.api.service.user.UserStorageService;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;
import static org.cubeengine.module.conomy.storage.TableBalance.TABLE_BALANCE;

public class ConomyService implements EconomyService
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
            currencies.put(defaultCurrency.getID(), defaultCurrency);
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
        Account account = accounts.get(uuid.toString());
        if (account instanceof UniqueAccount)
        {
            return Optional.of(((UniqueAccount) account));
        }
        if (account == null)
        {
            return loadAccount(uuid, false);
        }
        // else not UniqueAccount under uuid
        return Optional.empty();
    }

    @Override
    public Optional<UniqueAccount> createAccount(UUID uuid)
    {
        Optional<UniqueAccount> account = getAccount(uuid);
        return account.isPresent() ? account : loadAccount(uuid, true);
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
    public Optional<Account> getAccount(String identifier)
    {
        try
        {
            return getAccount(UUID.fromString(identifier)).map(Account.class::cast);
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    @Override
    public Optional<VirtualAccount> createVirtualAccount(String s)
    {
        return Optional.empty();
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

    public void registerCommands(CommandManager cm, I18n i18n)
    {
        cm.getProviderManager().register(module, new UniqueAccountReader(module, this, i18n), BaseAccount.Unique.class);
        cm.addCommand(new MoneyCommand(module, this, i18n));
        cm.addCommand(new EcoCommand(module, this, i18n));
    }
}
