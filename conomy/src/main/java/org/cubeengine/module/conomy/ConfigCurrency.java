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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import static org.cubeengine.libcube.util.ContextUtil.GLOBAL;

public class ConfigCurrency implements Currency
{
    private CurrencyConfiguration config;
    private boolean def = true;

    private Map<Context, Context> mirrors = new HashMap<>();

    public Optional<Context> getRelevantContext(Set<Context> contexts)
    {
        if (mirrors.isEmpty())
        {
            return Optional.of(GLOBAL);
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


    public String getId()
    {
        return this.getCurrencyID();
    }

    public String getName()
    {
        return config.name;
    }

    @Override
    public Component displayName()
    {
        return Component.text(config.name);
    }

    @Override
    public Component pluralDisplayName()
    {
        return Component.text(config.namePlural);
    }

    @Override
    public Component symbol()
    {
        return Component.text(config.symbol);
    }

    @Override
    public Component format(BigDecimal amount, int numFractionDigits)
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
        return Component.text(result);
    }

    @Override
    public int defaultFractionDigits()
    {
        return config.fractionalDigits;
    }

    @Override
    public boolean isDefault()
    {
        return def;
    }

    public String getCurrencyID()
    {
        return StringUtils.stripFileExtension(config.getFile().getName());
    }

    public long toLong(BigDecimal balance)
    {
        return (long)(balance.doubleValue() * config.fractionalDigitsFactor());
    }

    public BigDecimal fromLong(long longBalance)
    {
        return BigDecimal.valueOf(((double)longBalance / config.fractionalDigitsFactor()));
    }

    public BigDecimal getDefaultBalance(BaseAccount baseAccount)
    {
        if (baseAccount instanceof UniqueAccount)
        {
            return BigDecimal.valueOf((this.config.defaultBalance));
        }
        return BigDecimal.valueOf((this.config.defaultBankBalance));
    }

    public BigDecimal getMin(BaseAccount baseAccount)
    {
        if (baseAccount instanceof UniqueAccount)
        {
            return BigDecimal.valueOf((config.minimumBalance));
        }
        return BigDecimal.valueOf((config.minimumBankBalance));
    }
}
