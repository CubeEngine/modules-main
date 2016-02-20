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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;

public class ConfigCurrency implements Currency
{
    private CurrencyConfiguration config;
    private boolean def = true;

    private Map<Context, Context> mirrors = new HashMap<>();

    public Optional<Context> getRelevantContext(Set<Context> contexts)
    {
        if (mirrors.isEmpty())
        {
            return Optional.of(new Context("global", ""));
        }
        contexts.retainAll(mirrors.keySet()); // remove context where not allowed

        return contexts.stream().map(mirrors::get).findFirst(); // get from mirror ; get first context
    }

    public ConfigCurrency(CurrencyConfiguration config)
    {
        this.config = config;
        for (Map.Entry<Context, List<Context>> entry : config.contextMirrored.entrySet())
        {
            mirrors.put(entry.getKey(), entry.getKey());
            for (Context value : entry.getValue())
            {
                mirrors.put(value, entry.getKey());
            }
        }
    }

    @Override
    public Text getDisplayName()
    {
        return Text.of(config.name);
    }

    @Override
    public Text getPluralDisplayName()
    {
        return Text.of(config.namePlural);
    }

    @Override
    public Text getSymbol()
    {
        return Text.of(config.symbol);
    }

    @Override
    public Text format(BigDecimal amount, int numFractionDigits)
    {
        String result = config.format;
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(numFractionDigits);
        decimalFormat.setMinimumFractionDigits(numFractionDigits);
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(config.formatLocale));
        String name = amount.equals(BigDecimal.ONE) ? config.name : config.namePlural;
        result = result.replace("{SYMBOL}", config.symbol)
                       .replace("{AMOUNT}", decimalFormat.format(amount))
                       .replace("{NAME}", name) ;
        return Text.of(result);
    }

    @Override
    public int getDefaultFractionDigits()
    {
        return config.fractionalDigits;
    }

    @Override
    public boolean isDefault()
    {
        return def;
    }

    public String getID()
    {
        return config.getFile().getName();
    }

    public long toLong(BigDecimal balance)
    {
        return (long)(balance.doubleValue() * config.fractionalDigitsFactor());
    }

    public BigDecimal fromLong(long longBalance)
    {
        return new BigDecimal((double)longBalance / config.fractionalDigitsFactor());
    }

    public BigDecimal getDefaultBalance(BaseAccount baseAccount)
    {
        if (baseAccount instanceof UniqueAccount)
        {
            return new BigDecimal(this.config.defaultBalance);
        }
        return new BigDecimal(this.config.defaultBankBalance);
    }

    public BigDecimal getMin(BaseAccount baseAccount)
    {
        if (baseAccount instanceof UniqueAccount)
        {
            return new BigDecimal(config.minimumBalance);
        }
        return new BigDecimal(config.minimumBankBalance);
    }
}
