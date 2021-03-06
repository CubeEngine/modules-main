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
package org.cubeengine.module.conomy.bank;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyConfiguration;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.bank.command.BankCommand;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.plugin.PluginContainer;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;

public class BankConomyService extends ConomyService
{

    private final BankPermission bankPerms;

    public BankConomyService(Conomy module, ConomyConfiguration config, Path path, Database db, Reflector reflector, PermissionManager pm, PluginContainer plugin)
    {
        super(module, config, path, db, reflector, pm, plugin);
        bankPerms = new BankPermission(pm);
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
            Account account = accounts.get(identifier);
            if (account == null)
            {
                return loadAccount(identifier, true);
            }
            return Optional.of(account);
        }
    }

    @Override
    public boolean hasAccount(String identifier)
    {
        return super.hasAccount(identifier) || accounts.get(identifier) != null || loadAccount(identifier, false).isPresent();
    }

    protected Optional<Account> loadAccount(String name, boolean create)
    {
        AccountModel model = loadModel(name);
        if (model == null)
        {
            if (!create)
            {
                return Optional.empty();
            }
            model = createModel(name);
        }
        Account account;
        if (model.isUUID())
        {
            account = new BaseAccount.Unique(this, model, db);
        }
        else
        {
            account = new BaseAccount.Virtual(this, model, db);
        }
        this.accounts.put(account.identifier(), account);
        return Optional.of(account);
    }

    private AccountModel createModel(String name)
    {
        AccountModel model = db.getDSL().newRecord(TABLE_ACCOUNT).newAccount(name, false, false);
        model.store();
        return model;
    }

    public List<BaseAccount.Virtual> getBanks(Subject user, AccessLevel level)
    {
        List<String> manage = new ArrayList<>();
        List<String> withdraw = new ArrayList<>();
        List<String> deposit = new ArrayList<>();
        List<String> see = new ArrayList<>();
        this.getBankAccounts().stream().map(BaseAccount::identifier).forEach(bank -> {
            switch (user.option("conomy.bank.access-level" + bank).orElse("-1"))
            {
                case "0":
                    break;
                case "1":
                    see.add(bank);
                    break;
                case "2":
                    deposit.add(bank);
                    break;
                case "3":
                    withdraw.add(bank);
                    break;
                case "4":
                    manage.add(bank);
                    break;
                default:
                    break;
            }
        });

        if (level.value <= AccessLevel.WITHDRAW.value)
        {
            manage.addAll(withdraw);
        }
        if (level.value <= AccessLevel.DEPOSIT.value)
        {
            manage.addAll(deposit);
        }
        if (level.value <= AccessLevel.SEE.value)
        {
            manage.addAll(see);
        }

        List<BaseAccount.Virtual> collect = manage.stream().map(this::findOrCreateAccount)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(a -> a instanceof BaseAccount.Virtual)
                .map(BaseAccount.Virtual.class::cast)
                .collect(Collectors.toList());

        if (level.value == AccessLevel.SEE.value && bankPerms.ACCESS_SEE.check(user))
        {
            collect.addAll(getBankAccounts());
        }
        return collect;
    }

    private Collection<BaseAccount.Virtual> getBankAccounts()
    {
        // TODO load all banks
        return this.accounts.values().stream()
                .filter(a -> a instanceof BaseAccount.Virtual)
                .map(BaseAccount.Virtual.class::cast)
                .collect(Collectors.toList());
    }

    @Override
    public void registerCommands(RegisterCommandEvent<Parameterized> event, ModuleManager mm, I18n i18n)
    {
        super.registerCommands(event, mm, i18n);
        mm.registerCommands(event, plugin, module, BankCommand.class);
    }

    public boolean hasAccess(BaseAccount.Virtual bank, AccessLevel level, Subject context)
    {
        switch (level)
        {
            case NONE:
                return true;
            case SEE:
                if (bankPerms.ACCESS_SEE.check(context))
                {
                    return true;
                }
                break;
            case DEPOSIT:
                if (bankPerms.ACCESS_DEPOSIT.check(context))
                {
                    return true;
                }
                break;
            case WITHDRAW:
                if (bankPerms.ACCESS_WITHDRAW.check(context))
                {
                    return true;
                }
                break;
            case MANAGE:
                if (bankPerms.ACCESS_MANAGE.check(context))
                {
                    return true;
                }
                break;
        }
        return context.option("conomy.bank.access-level." + bank.identifier()).map(Integer::valueOf).orElse(-1) >= level.value;
    }
}
