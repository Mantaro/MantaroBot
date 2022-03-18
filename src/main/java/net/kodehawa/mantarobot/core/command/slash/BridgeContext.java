package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;
import redis.clients.jedis.JedisPool;

import java.util.Collection;

// This is pain and suffering.
public class BridgeContext implements IContext {
    private final MantaroBot bot = MantaroBot.getInstance();
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private final SlashContext slashContext;
    private final Context prefixContext;

    public BridgeContext(SlashContext slashContext, Context prefixContext) {
        this.slashContext = slashContext;
        this.prefixContext = prefixContext;
    }

    public MantaroBot getBot() {
        return bot;
    }

    public Config getConfig() {
        return config;
    }

    public ManagedDatabase db() {
        return managedDatabase;
    }

    public boolean isSlash() {
        return slashContext != null;
    }

    public Guild getGuild() {
        if (isSlash())
            return slashContext.getGuild();
        else {
            return prefixContext.getGuild();
        }
    }

    public TextChannel getChannel() {
        if (isSlash())
            return slashContext.getChannel();
        else {
            return prefixContext.getChannel();
        }
    }

    public Member getMember() {
        if (isSlash())
            return slashContext.getMember();
        else {
            return prefixContext.getMember();
        }
    }

    public User getUser() {
        return getMember().getUser();
    }

    public User getAuthor() {
        return getMember().getUser();
    }

    public SlashContext getSlashContext() {
        return slashContext;
    }

    public Context getPrefixContext() {
        return prefixContext;
    }

    public I18nContext getLanguageContext() {
        if (isSlash())
            return slashContext.getI18nContext();
        else {
            return prefixContext.getLanguageContext();
        }
    }

    public void sendLocalized(String source, Object... args) {
        if (isSlash()) {
            slashContext.reply(source, args);
        } else {
            prefixContext.sendLocalized(source, args);
        }
    }

    public void send(String source) {
        if (isSlash()) {
            slashContext.reply(source);
        } else {
            prefixContext.send(source);
        }
    }

    @Override
    public void send(MessageEmbed embed) {
        if (isSlash()) {
            slashContext.send(embed);
        } else {
            prefixContext.send(embed);
        }
    }

    @Override
    public void sendFormat(String message, Object... format) {
        if (isSlash()) {
            slashContext.sendFormat(message, format);
        } else {
            prefixContext.sendFormat(message, format);
        }
    }

    @Override
    public void sendFormat(String message, Collection<ActionRow> actionRow, Object... format) {
        if (isSlash()) {
            slashContext.sendFormat(message, actionRow, format);
        } else {
            prefixContext.sendFormat(message, actionRow, format);
        }

    }


    public RatelimitContext ratelimitContext() {
        if (isSlash()) {
            return slashContext.ratelimitContext();
        } else {
            return prefixContext.ratelimitContext();
        }
    }

    public boolean isUserBlacklisted(String id) {
        return getMantaroData().getBlackListedUsers().contains(id);
    }

    public JedisPool getJedisPool() {
        return MantaroData.getDefaultJedisPool();
    }

    public MantaroAudioManager getAudioManager() {
        return getBot().getAudioManager();
    }

    public ShardManager getShardManager() {
        return getBot().getShardManager();
    }

    public DBGuild getDBGuild() {
        return managedDatabase.getGuild(getGuild());
    }

    public DBUser getDBUser() {
        return managedDatabase.getUser(getUser());
    }

    public DBUser getDBUser(User user) {
        return managedDatabase.getUser(user);
    }

    public DBUser getDBUser(Member member) {
        return managedDatabase.getUser(member);
    }

    public DBUser getDBUser(String id) {
        return managedDatabase.getUser(id);
    }

    public Player getPlayer() {
        return managedDatabase.getPlayer(getUser());
    }

    public Player getPlayer(User user) {
        return managedDatabase.getPlayer(user);
    }

    public Player getPlayer(Member member) {
        return managedDatabase.getPlayer(member);
    }

    public Player getPlayer(String id) {
        return managedDatabase.getPlayer(id);
    }

    public PlayerStats getPlayerStats() {
        return managedDatabase.getPlayerStats(getMember());
    }

    public PlayerStats getPlayerStats(String id) {
        return managedDatabase.getPlayerStats(id);
    }

    public PlayerStats getPlayerStats(User user) {
        return managedDatabase.getPlayerStats(user);
    }

    public PlayerStats getPlayerStats(Member member) {
        return managedDatabase.getPlayerStats(member);
    }

    public MantaroObj getMantaroData() {
        return managedDatabase.getMantaroData();
    }
}
