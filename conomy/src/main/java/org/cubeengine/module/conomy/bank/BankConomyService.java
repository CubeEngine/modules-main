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
import org.cubeengine.logscribe.Log;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.butler.Dispatcher;
import org.cubeengine.module.conomy.AccessLevel;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyConfiguration;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.bank.command.BankCommand;
import org.cubeengine.module.conomy.bank.command.BankManageCommand;
import org.cubeengine.module.conomy.bank.command.EcoBankCommand;
import org.cubeengine.module.conomy.bank.command.VirtualAccountParser;
import org.cubeengine.module.conomy.storage.AccountModel;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

import static org.cubeengine.module.conomy.storage.TableAccount.TABLE_ACCOUNT;

public class BankConomyService extends ConomyService
{
    public BankConomyService(Conomy module, ConomyConfiguration config, Path path, Database db, Reflector reflector)
    {
        super(module, config, path, db, reflector);
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
        this.accounts.put(account.getIdentifier(), account);
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
        SubjectData data = user.getSubjectData();
        Map<String, String> options = data.getOptions(user.getActiveContexts());

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
                            module.getLog().warn("Invalid value for option: {} -> {}", e.getKey(), e.getValue());
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

        List<BaseAccount.Virtual> collect = manage.stream().map(this::getOrCreateAccount)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(a -> a instanceof BaseAccount.Virtual)
                .map(BaseAccount.Virtual.class::cast)
                .collect(Collectors.toList());

        if (level.value == AccessLevel.SEE.value && user.hasPermission(module.bankPerms().ACCESS_SEE.getId()))
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
    public void registerCommands(CommandManager cm, I18n i18n)
    {
        super.registerCommands(cm, i18n);
        ((Dispatcher) cm.getCommand("eco")).addCommand(new EcoBankCommand(cm, this, i18n));
        cm.getProviders().register(module, new VirtualAccountParser(this, i18n), BaseAccount.Virtual.class);
        BankCommand bankCommand = new BankCommand(cm, this, i18n);
        cm.addCommand(bankCommand);
        bankCommand.addCommand(new BankManageCommand(cm, this, i18n));
    }

    public boolean hasAccess(BaseAccount.Virtual bank, AccessLevel level, Subject context)
    {
        switch (level)
        {
            case NONE:
                return true;
            case SEE:
                if (context.hasPermission(module.bankPerms().ACCESS_SEE.getId()))
                {
                    return true;
                }
                break;
            case DEPOSIT:
                if (context.hasPermission(module.bankPerms().ACCESS_DEPOSIT.getId()))
                {
                    return true;
                }
                break;
            case WITHDRAW:
                if (context.hasPermission(module.bankPerms().ACCESS_WITHDRAW.getId()))
                {
                    return true;
                }
                break;
            case MANAGE:
                if (context.hasPermission(module.bankPerms().ACCESS_MANAGE.getId()))
                {
                    return true;
                }
                break;
        }
        Map<String, String> options = context.getSubjectData().getOptions(context.getActiveContexts());
        return Integer.valueOf(options.get("conomy.bank.access-level." + bank.getIdentifier())) >= level.value;
    }
}
