package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;

import java.util.Collection;

public interface IContext {
    Guild getGuild();
    TextChannel getChannel();
    Member getMember();
    User getAuthor();
    RatelimitContext ratelimitContext();
    void send(String s);
    Message sendResult(String s);
    void send(MessageEmbed e);
    Message sendResult(MessageEmbed e);
    void sendFormat(String message, Object... format);
    void sendFormat(String message, Collection<ActionRow> actionRow, Object... format);
    void sendLocalized(String s, Object... args);
    ManagedDatabase db();
    I18nContext getLanguageContext();
}

