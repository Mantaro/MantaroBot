package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
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
    void send(Message message);
    void sendStripped(String s);
    void send(MessageEmbed e);
    void send(MessageEmbed e, ActionRow... actionRows);
    Message sendResult(String s);
    Message sendResult(MessageEmbed e);
    void sendLocalized(String s, Object... args);
    void sendLocalizedStripped(String s, Object... args);
    void sendFormat(String message, Object... format);
    void sendFormatStripped(String message, Object... format);
    void sendFormat(String message, Collection<ActionRow> actionRow, Object... format);
    ManagedDatabase db();
    Player getPlayer();
    DBUser getDBUser();
    DBGuild getDBGuild();
    Player getPlayer(User user);
    DBUser getDBUser(User user);
    ShardManager getShardManager();
    MantaroObj getMantaroData();
    Config getConfig();

    default EmbedBuilder baseEmbed(IContext ctx, String name, String image) {
        return new EmbedBuilder()
                .setAuthor(name, null, image)
                .setColor(ctx.getMember().getColor())
                .setFooter("Requested by: %s".formatted(ctx.getMember().getEffectiveName()),
                        ctx.getGuild().getIconUrl()
                );
    }

}

