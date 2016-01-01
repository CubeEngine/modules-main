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

import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.filesystem.FileExtensionFilter;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;

public class ConomyService implements EconomyService
{
    // TODO missing mass operations
    // - get all Accounts of Type
    // - set all Accounts to val
    // - add/remove to/from all Accounts

    private Currency defaultCurrency;
    private List<ContextCalculator<Account>> contextCalculators = new ArrayList<>();

    private Map<UUID, UserAccount> userAccounts = new HashMap<>();
    private Map<String, BankAccount> bankAccounts = new HashMap<>();
    private Map<String, Currency> currencies = new HashMap<>();
    private Database db;

    public ConomyService(ConomyConfiguration config, Path path, Database db, Reflector reflector)
    {
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
}
