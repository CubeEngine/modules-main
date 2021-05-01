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
import java.util.stream.Stream;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.filesystem.FileExtensionFilter;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.conomy.command.EcoCommand;
import org.cubeengine.module.conomy.command.MoneyCommand;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.reflect.Reflector;
import org.jooq.Condition;
import org.jooq.Record4;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.AccountDeletionResultType;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.account.VirtualAccount;
import org.spongepowered.plugin.PluginContainer;

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
    protected PluginContainer plugin;

    protected ConomyPermission perms;

    public ConomyService(Conomy module, ConomyConfiguration config, Path path, Database db, Reflector reflector, PermissionManager pm, PluginContainer plugin)
    {
        this.perms = new ConomyPermission(pm);
        this.module = module;
        this.db = db;
        this.plugin = plugin;
        try
        {
            for (Path file : Files.newDirectoryStream(path, FileExtensionFilter.YAML))
            {
                ConfigCurrency cc = new ConfigCurrency(reflector.load(CurrencyConfiguration.class, file.toFile()));
                currencies.put(cc.getCurrencyID(), cc);
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
    }

    public ConomyPermission getPerms()
    {
        return perms;
    }

    @Override
    public Stream<UniqueAccount> streamUniqueAccounts()
    {
        return null;
    }

    @Override
    public Collection<UniqueAccount> uniqueAccounts()
    {
        return null;
    }

    @Override
    public Stream<VirtualAccount> streamVirtualAccounts()
    {
        return null;
    }

    @Override
    public Collection<VirtualAccount> virtualAccounts()
    {
        return null;
    }

    @Override
    public AccountDeletionResultType deleteAccount(UUID uuid)
    {
        return null;
    }

    @Override
    public AccountDeletionResultType deleteAccount(String identifier)
    {
        return null;
    }

    @Override
    public Currency defaultCurrency()
    {
        return defaultCurrency;
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
    public Optional<UniqueAccount> findOrCreateAccount(UUID uuid)
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
    public Optional<Account> findOrCreateAccount(String identifier)
    {
        try
        {
            return findOrCreateAccount(UUID.fromString(identifier)).map(Account.class::cast);
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
        Optional<User> user = Sponge.server().userManager().find(uuid);
        AccountModel model = db.getDSL().newRecord(TABLE_ACCOUNT).newAccount(uuid, user.map(User::name).orElse(uuid.toString()), false, false);
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

    public void registerCommands(RegisterCommandEvent<Parameterized> event, ModuleManager mm, I18n i18n)
    {
        mm.registerCommands(event, plugin, module, MoneyCommand.class);
        mm.registerCommands(event, plugin, module, EcoCommand.class);
    }

    public Collection<ConfigCurrency> getCurrencies()
    {
        return this.currencies.values();
    }
}
