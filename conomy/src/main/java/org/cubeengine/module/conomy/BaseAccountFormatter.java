package org.cubeengine.module.conomy;

import org.cubeengine.dirigent.Component;
import org.cubeengine.dirigent.formatter.Context;
import org.cubeengine.dirigent.formatter.reflected.Format;
import org.cubeengine.dirigent.formatter.reflected.Names;
import org.cubeengine.dirigent.formatter.reflected.ReflectedFormatter;
import org.cubeengine.service.i18n.formatter.component.StyledComponent;
import org.cubeengine.service.i18n.formatter.component.TextComponent;

import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;

@Names("account")
public class BaseAccountFormatter extends ReflectedFormatter
{
    @Format
    public Component format(BaseAccount.Unique userAcc, Context context)
    {
        return new StyledComponent(DARK_GREEN, new TextComponent(userAcc.getDisplayName()));
    }

    @Format
    public Component format(BaseAccount.Virtual bankAcc, Context context)
    {
        return new StyledComponent(GOLD, new TextComponent(bankAcc.getDisplayName()));
    }

}
