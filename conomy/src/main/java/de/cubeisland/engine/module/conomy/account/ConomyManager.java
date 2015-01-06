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
package de.cubeisland.engine.module.conomy.account;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import de.cubeisland.engine.core.logging.LoggingUtil;
import de.cubeisland.engine.core.module.service.Economy;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserManager;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.logscribe.LogLevel;
import de.cubeisland.engine.logscribe.target.file.AsyncFileTarget;
import de.cubeisland.engine.module.conomy.Conomy;
import de.cubeisland.engine.module.conomy.ConomyConfiguration;
import de.cubeisland.engine.module.conomy.account.storage.AccountModel;
import de.cubeisland.engine.module.conomy.account.storage.BankAccessModel;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;

import static de.cubeisland.engine.module.conomy.account.storage.TableAccount.TABLE_ACCOUNT;
import static de.cubeisland.engine.module.conomy.account.storage.TableBankAccess.TABLE_BANK_ACCESS;

public class ConomyManager
{
    protected final Conomy module;
    protected final Log logger;
    protected final ConomyConfiguration config;
    protected final DSLContext dsl;
    protected final UserManager um;
    private final ThreadFactory threadFactory;
    private final Map<String, BankAccount> bankaccounts = new HashMap<>();
    private final Map<Long, BankAccount> bankaccountsID = new HashMap<>();
    private final Economy conomyInterface;
    private Thread thread = null;

    public ConomyManager(Conomy module)
    {
        this.module = module;
        this.threadFactory = module.getCore().getTaskManager().getThreadFactory(module);
        this.config = module.getConfig();

        this.dsl = this.module.getCore().getDB().getDSL();

        this.logger = module.getCore().getLogFactory().getLog(Conomy.class, "Conomy-Transactions");
        this.logger.addTarget(new AsyncFileTarget(LoggingUtil.getLogFile(module.getCore(), "Conomy-Transactions"),
                                                  LoggingUtil.getFileFormat(true, false), true, LoggingUtil.getCycler(),
                                                  module.getCore().getTaskManager().getThreadFactory()));
        if (!this.module.getConfig().enableLogging)
        {
            logger.setLevel(LogLevel.NONE);
        }

        this.um = this.module.getCore().getUserManager();
        this.conomyInterface = new ConomyInterface(this);
    }

    public BankAccount getBankAccount(String name, boolean create)
    {
        BankAccount bankAccount = this.bankaccounts.get(name);
        if (bankAccount == null)
        {
            AccountModel model = this.dsl.selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.NAME.eq(name)).fetchOneInto(
                TABLE_ACCOUNT);
            if (model == null)
            {
                if (!create)
                {
                    return null;
                }
                model = this.dsl.newRecord(TABLE_ACCOUNT).
                    newAccount(null, name, (int)(this.config.defaultBankBalance * this.config.fractionalDigitsFactor()),
                               false, this.config.bankNeedInvite);
                model.insert();
                bankAccount = new BankAccount(this, model);
                this.logger.info("NEW Bank:{} :: {}", name, bankAccount.balance());
            }
            else
            {
                bankAccount = new BankAccount(this, model);
                this.logger.info("LOAD Bank:{} :: {}", name, bankAccount.balance());
            }
            this.bankaccounts.put(name, bankAccount);
            this.bankaccountsID.put(bankAccount.model.getValue(TABLE_ACCOUNT.KEY).longValue(), bankAccount);
        }
        return bankAccount;
    }

    public UserAccount getUserAccount(User user, boolean create)
    {
        AccountAttachment attachment = user.attachOrGet(AccountAttachment.class, module);
        if (attachment.getAccount() == null)
        {
            if (!create)
            {
                return null;
            }
            attachment.createAccount();
        }
        return attachment.getAccount();
    }

    public boolean bankAccountExists(String name)
    {
        if (this.bankaccounts.containsKey(name))
        {
            return true;
        }
        Account acc = this.getBankAccount(name, false);
        return acc != null;
    }

    public void setAllOnline(final double value)
    {
        for (User user : this.module.getCore().getUserManager().getOnlineUsers())
        {
            UserAccount userAccount = ConomyManager.this.getUserAccount(user, true);
            userAccount.set(value);
        }
    }

    public void transactionAllOnline(final double value)
    {
        for (User user : this.um.getOnlineUsers())
        {
            UserAccount userAccount = ConomyManager.this.getUserAccount(user, true);
            userAccount.deposit(value);
        }
    }

    public void scaleAllOnline(final float factor)
    {
        for (User user : this.um.getOnlineUsers())
        {
            UserAccount userAccount = ConomyManager.this.getUserAccount(user, true);
            userAccount.scale(factor);
        }
    }

    public void setAll(boolean userAcc, boolean bankAcc, double value)
    {
        final long longValue = (long)(value * this.config.fractionalDigitsFactor());
        this.dsl.update(TABLE_ACCOUNT).set(TABLE_ACCOUNT.VALUE, longValue).
            where(DSL.condition(TABLE_ACCOUNT.NAME.getName() + " IS NULL = ?", userAcc)).or(DSL.condition(
            TABLE_ACCOUNT.USER_ID.getName() + " IS NULL = ?", bankAcc)).execute();
        this.logger.info("SET-ALL {} {}", (userAcc && bankAcc ? "User/Bank" : userAcc ? "User" : "Bank"), value);
        // update all loaded accounts...
        if (userAcc)
        {
            for (User user : this.um.getOnlineUsers())
            {
                UserAccount userAccount = ConomyManager.this.getUserAccount(user, false);
                if (userAccount != null)
                {
                    userAccount.model.setValue(TABLE_ACCOUNT.VALUE, longValue);
                }
            }
        }
        if (bankAcc)
        {
            for (BankAccount bankAccount : this.bankaccounts.values())
            {
                bankAccount.model.setValue(TABLE_ACCOUNT.VALUE, longValue);
            }
        }
    }

    public void scaleAll(boolean userAcc, boolean bankAcc, float factor)
    {
        this.dsl.query("UPDATE " + TABLE_ACCOUNT.getName() + " SET " + TABLE_ACCOUNT.VALUE.getName() + " = ? * "
                           + TABLE_ACCOUNT.VALUE.getName() +
                           " WHERE " + TABLE_ACCOUNT.NAME.getName() + " IS NULL = ? OR "
                           + TABLE_ACCOUNT.USER_ID.getName() + " IS NULL = ?", factor, userAcc, bankAcc).execute();
        this.logger.info("SCALE-ALL {} {}", (userAcc && bankAcc ? "User/Bank" : userAcc ? "User" : "Bank"), factor);
        // update all loaded accounts...
        if (userAcc)
        {
            for (User user : this.um.getOnlineUsers())
            {
                UserAccount userAccount = ConomyManager.this.getUserAccount(user, false);
                if (userAccount != null)
                {
                    userAccount.model.setValue(TABLE_ACCOUNT.VALUE, (long)(userAccount.model.getValue(TABLE_ACCOUNT.VALUE) * factor));
                }
            }
        }
        if (bankAcc)
        {
            for (BankAccount bankAccount : this.bankaccounts.values())
            {
                bankAccount.model.setValue(TABLE_ACCOUNT.VALUE, (long)(bankAccount.model.getValue(TABLE_ACCOUNT.VALUE) * factor));
            }
        }
    }

    public void transactionAll(boolean userAcc, boolean bankAcc, double value)
    {
        final long longValue = (long)(value * this.config.fractionalDigitsFactor());
        this.dsl.query("UPDATE " + TABLE_ACCOUNT.getName() + " SET " + TABLE_ACCOUNT.VALUE.getName() + " = ? + "
                           + TABLE_ACCOUNT.VALUE.getName() +
                           " WHERE " + TABLE_ACCOUNT.NAME.getName() + " IS NULL = ? OR "
                           + TABLE_ACCOUNT.USER_ID.getName() + " IS NULL = ?", value, userAcc, bankAcc).execute();
        this.logger.info("TRANSACTION-ALL {} {}", (userAcc && bankAcc ? "User/Bank" : userAcc ? "User" : "Bank"),
                         value);
        // update all loaded accounts...
        if (userAcc)
        {
            for (User user : this.um.getOnlineUsers())
            {
                UserAccount userAccount = ConomyManager.this.getUserAccount(user, false);
                if (userAccount != null)
                {
                    userAccount.model.setValue(TABLE_ACCOUNT.VALUE, userAccount.model.getValue(TABLE_ACCOUNT.VALUE) + longValue);
                }
            }
        }
        if (bankAcc)
        {
            for (BankAccount bankAccount : this.bankaccounts.values())
            {
                bankAccount.model.setValue(TABLE_ACCOUNT.VALUE, bankAccount.model.getValue(TABLE_ACCOUNT.VALUE) + longValue);
            }
        }
    }

    public boolean transaction(Account from, Account to, double amount, boolean force)
    {
        if (to == null && from == null)
        {
            throw new IllegalStateException("Both accounts are null!");
        }
        if (from != null && !force)
        {
            if (!from.has(amount))
            {
                return false;
            }
        }
        if (to != null && from != null)
        {
            this.logger.info("TRANSACTION {}{} -> {}{}", (from instanceof UserAccount ? "User:" : "Bank:"),
                             from.getName(), (to instanceof UserAccount ? "User:" : "Bank:"), to.getName());
        }
        if (from != null)
        {
            from.withdraw(amount);
        }
        if (to != null)
        {
            to.deposit(amount);
        }
        return true;
    }

    public Collection<AccountModel> getTopAccounts(boolean user, boolean bank, int fromRank, int toRank,
                                                   boolean showHidden)
    {
        return this.dsl.selectFrom(TABLE_ACCOUNT).
            where(DSL.condition("((mask & 1) = 0 OR ((mask & 1) = 1) = " + showHidden + ")")).
                           and(DSL.condition("(name IS NULL) = " + user + " OR " + "(user_id IS NULL) = " + bank)).
                           orderBy(TABLE_ACCOUNT.VALUE.desc()).limit(fromRank - 1, toRank - fromRank + 1).
                           fetch();
    }

    public void hideAll(boolean userAcc, boolean bankAcc)
    {
        this.dsl.query("UPDATE " + TABLE_ACCOUNT.getName() + " SET " + TABLE_ACCOUNT.MASK.getName() + " = 1 | "
                           + TABLE_ACCOUNT.MASK.getName() +
                           " WHERE " + TABLE_ACCOUNT.NAME.getName() + " IS NULL = ? OR "
                           + TABLE_ACCOUNT.USER_ID.getName() + " IS NULL = ?", userAcc, bankAcc).execute();
        if (userAcc)
        {
            for (User user : this.um.getOnlineUsers())
            {
                UserAccount userAccount = ConomyManager.this.getUserAccount(user, false);
                if (userAccount != null)
                {
                    userAccount.model.setHidden(true);
                }
            }
        }
        if (bankAcc)
        {
            for (BankAccount bankAccount : this.bankaccounts.values())
            {
                bankAccount.model.setHidden(true);
            }
        }
    }

    public void unhideAll(boolean userAcc, boolean bankAcc)
    {
        this.dsl.query("UPDATE " + TABLE_ACCOUNT.getName() + " SET " + TABLE_ACCOUNT.MASK.getName() + " = 1 & ~"
                           + TABLE_ACCOUNT.MASK.getName() +
                           " WHERE " + TABLE_ACCOUNT.NAME.getName() + " IS NULL = ? OR "
                           + TABLE_ACCOUNT.USER_ID.getName() + " IS NULL = ?", userAcc, bankAcc).execute();
        if (userAcc)
        {
            for (User user : this.um.getOnlineUsers())
            {
                UserAccount userAccount = ConomyManager.this.getUserAccount(user, false);
                if (userAccount != null)
                {
                    userAccount.model.setHidden(true);
                }
            }
        }
        if (bankAcc)
        {
            for (BankAccount bankAccount : this.bankaccounts.values())
            {
                bankAccount.model.setHidden(true);
            }
        }
    }

    public boolean deleteUserAccount(User user)
    {
        UserAccount account = this.getUserAccount(user, false);
        if (account == null)
        {
            return false;
        }
        account.model.delete();
        user.detach(AccountAttachment.class);
        return true;
    }

    public boolean deleteBankAccount(String name)
    {
        BankAccount bankAccount = this.bankaccounts.get(name);
        if (bankAccount == null)
        {
            return false;
        }
        bankAccount.model.delete();
        this.bankaccounts.remove(name);
        this.bankaccountsID.remove(bankAccount.model.getValue(TABLE_ACCOUNT.KEY).longValue());
        return true;
    }

    public String format(double balance)
    {
        return this.format(Locale.ENGLISH, balance);
    }

    public String format(Locale locale, double balance)
    {
        return String.format(locale, "%." + this.config.fractionalDigits + "f " + this.config.symbol, balance);
    }

    public Double parse(String amountString, Locale locale)
    {

        //private Pattern pattern2 = Pattern.compile("[^a-zA-Z]+");
        //private Pattern pattern1;
        //this.pattern1 = Pattern.compile("^-*[\\d,]+$");
        NumberFormat format = NumberFormat.getInstance(locale);
        try
        {
            Number parsed = format.parse(amountString);
            return parsed.doubleValue();
        }
        catch (NumberFormatException | ParseException ex)
        {
        }
        // TODO filter currency Names / Symbols http://git.cubeisland.de/cubeengine/cubeengine/issues/250
        return null;
    }

    public long fractionalDigitsFactor()
    {
        return this.config.fractionalDigitsFactor();
    }

    public double getDefaultBalance()
    {
        return this.config.defaultBalance;
    }

    public double getDefaultBankBalance()
    {
        return this.config.defaultBankBalance;
    }

    public double getMinimumBankBalance()
    {
        return this.config.minimumBankBalance;
    }

    public double getMinimumBalance()
    {
        return this.config.minimumBalance;
    }

    public Set<BankAccount> getBankAccounts(User user)
    {
        Result<AccountModel> accountModels = this.dsl.selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.KEY.eq(
            this.dsl.select(TABLE_BANK_ACCESS.ACCOUNTID).from(TABLE_BANK_ACCESS).where(TABLE_BANK_ACCESS.USERID.eq(
                user.getEntity().getKey())))).fetch();
        Set<BankAccount> accounts = new HashSet<>();
        for (AccountModel accountModel : accountModels)
        {
            BankAccount account = this.bankaccountsID.get(accountModel.getValue(TABLE_ACCOUNT.KEY).longValue());
            if (account == null)
            {
                account = new BankAccount(this, accountModel);
                this.bankaccountsID.put(accountModel.getValue(TABLE_ACCOUNT.KEY).longValue(), account);
                this.bankaccounts.put(account.getName(), account);
            }
            accounts.add(account);
        }
        return accounts;
    }

    public boolean getAutoCreateUserAccount()
    {
        return this.config.autocreateUserAcc;
    }

    public boolean renameBank(BankAccount bankAccount, String newName)
    {
        BankAccount acc = this.getBankAccount(newName, false);
        if (acc != null)
        {
            return false; // Account name exists!
        }
        this.bankaccounts.remove(bankAccount.getName());
        bankAccount.model.setValue(TABLE_ACCOUNT.NAME, newName);
        bankAccount.model.update();
        this.bankaccounts.put(newName, bankAccount);
        return true;
    }

    public Economy getInterface()
    {
        return this.conomyInterface;
    }

    /**
     * Returns the names of all banks
     *
     * @param hidden if true return hidden banks too
     */
    public Set<String> getBankNames(boolean hidden)
    {
        Set<String> banks = new HashSet<>();
        Result<Record1<String>> fetch = this.dsl.select(TABLE_ACCOUNT.NAME).from(TABLE_ACCOUNT).
            where(TABLE_ACCOUNT.NAME.isNotNull()).
            and(DSL.condition("(mask & 1 = 0) OR (mask & 1 = 1) = " + hidden)).fetch();
        for (Record1<String> names : fetch)
        {
            banks.add(names.value1());
        }
        return banks;
    }

    protected AccountModel loadUserAccount(User holder)
    {
        return this.dsl.selectFrom(TABLE_ACCOUNT).where(TABLE_ACCOUNT.USER_ID.eq(
            holder.getEntity().getKey())).fetchOneInto(TABLE_ACCOUNT);
    }

    public List<BankAccessModel> getBankAccess(AccountModel model)
    {
        return this.dsl.selectFrom(TABLE_BANK_ACCESS).where(TABLE_BANK_ACCESS.ACCOUNTID.eq(model.getValue(TABLE_ACCOUNT.KEY))).fetch();
    }
}
