package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;

public interface IContext {
    Guild getGuild();
    TextChannel getChannel();
    Member getMember();
    User getAuthor();
    RatelimitContext ratelimitContext();
    void send(String s);
    void sendLocalized(String s, Object... args);
    I18nContext getLanguageContext();
}

