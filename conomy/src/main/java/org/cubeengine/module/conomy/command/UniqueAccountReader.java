package org.cubeengine.module.conomy.command;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.service.command.TranslatedReaderException;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class UniqueAccountReader implements ArgumentReader<BaseAccount.Unique>, DefaultValue<BaseAccount.Unique>, Completer
{
    private Conomy module;
    private final ConomyService service;
    private final I18n i18n;

    public UniqueAccountReader(Conomy module, ConomyService service, I18n i18n)
    {
        this.module = module;
        this.service = service;
        this.i18n = i18n;
    }

    @Override
    public BaseAccount.Unique read(Class type, CommandInvocation invocation) throws ReaderException
    {
        String arg = invocation.consume(1);
        User user = (User)invocation.getManager().read(User.class, User.class, invocation);
        Optional<BaseAccount.Unique> target = getAccount(user).filter(a -> {
                Object cmdSource = invocation.getCommandSource();
                return !(cmdSource instanceof Subject && a.isHidden()
                        && !((Subject) cmdSource).hasPermission(module.perms().ACCESS_SEE.getId()));
            });
        if (!target.isPresent())
        {
            throw new TranslatedReaderException(i18n.translate(invocation.getContext(Locale.class), NEGATIVE,
                    "No account found for {user}!", arg));
        }
        return target.get();
    }

    @Override
    public BaseAccount.Unique getDefault(CommandInvocation invocation)
    {
        if (!(invocation.getCommandSource() instanceof User))
        {
            throw new TranslatedReaderException(i18n.translate(invocation.getContext(Locale.class), NEGATIVE,
                    "You have to specify a user!"));
        }
        User user = (User) invocation.getCommandSource();
        Optional<BaseAccount.Unique> account = getAccount(user);
        if (!account.isPresent())
        {
            throw new TranslatedReaderException(i18n.translate(invocation.getContext(Locale.class), NEGATIVE,
                    "You have no account!"));
        }
        return account.get();
    }

    private Optional<BaseAccount.Unique> getAccount(User user)
    {
        return service.createAccount(user.getUniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast);
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        return invocation.getManager().getCompleter(User.class).getSuggestions(invocation);
    }
}
