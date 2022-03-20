package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;

import java.util.Collection;

public interface IContext {
    Guild getGuild();
    TextChannel getChannel();
    Member getMember();
    User getAuthor();
    RatelimitContext ratelimitContext();
    UtilsContext getUtilsContext();
    I18nContext getLanguageContext();
    void send(String s);
    void send(MessageEmbed e);
    Message sendResult(String s);
    Message sendResult(MessageEmbed e);
    void sendLocalized(String s, Object... args);
    void sendFormat(String message, Object... format);
    void sendFormat(String message, Collection<ActionRow> actionRow, Object... format);
    ManagedDatabase db();
    Player getPlayer();
    DBUser getDBUser();
    Player getPlayer(User user);
    DBUser getDBUser(User user);
    MantaroObj getMantaroData();
    Config getConfig();
}

