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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.service.context.Context;

@SuppressWarnings("all")
public class CurrencyConfiguration extends ReflectedYaml
{
    @Name("currency.symbol")
    public String symbol = "€";
    @Name("currency.name")
    public String name = "Euro";
    @Name("currency.name-plural")
    public String namePlural = "Euros";

    @Name("default.user.balance")
    public double defaultBalance = 1000;
    @Name("default.user.minimum-balance")
    public double minimumBalance = 0;

    @Name("default.bank.balance")
    public double defaultBankBalance = 0;
    @Name("default.bank.minimum-balance")
    public double minimumBankBalance = 0;

    @Comment("The Number of fractional-digits.\n" +
                 "e.g.: 1.00€ -> 2")
    @Name("currency.fractional-digits")
    public int fractionalDigits = 2;

    @Comment("How to format an amount of a currency\n" +
            "Supported Substitutions:\n" +
            " - {AMOUNT}" +
            " - {SYMBOL}" +
            " - {NAME}")
    public String format = "{AMOUNT} {SYMBOL}";

    @Comment("The locale to use when formatting decimals")
    public Locale formatLocale = Locale.ENGLISH;

    public int fractionalDigitsFactor()
    {
        return (int)Math.pow(10, this.fractionalDigits);
    }

    @Comment("The contexts in which an accounts balance is shared\n" +
            "Leave empty to always use global context")
    public Map<Context, List<Context>> contextMirrored = new HashMap<>();
}
