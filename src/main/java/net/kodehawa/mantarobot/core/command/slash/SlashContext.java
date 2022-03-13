package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;
import redis.clients.jedis.JedisPool;

import java.util.EnumSet;
import java.util.List;

public class SlashContext implements IContext {
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private final SlashCommandEvent slash;
    private final I18nContext i18n;

    public SlashContext(SlashCommandEvent event, I18nContext i18n) {
        this.slash = event;
        this.i18n = i18n;
    }

    public I18nContext getI18nContext() {
        return i18n;
    }

    public String getName() {
        return slash.getName();
    }

    public String getSubCommand() {
        return slash.getSubcommandName();
    }

    public OptionMapping getOption(String name) {
        return slash.getOption(name);
    }

    public void defer() {
        slash.deferReply().queue();
    }

    // This is a little cursed, but I guess we can make do.
    public List<OptionMapping> getOptions() {
        return slash.getOptions();
    }

    public TextChannel getChannel() throws IllegalStateException {
        return slash.getTextChannel();
    }

    public Member getMember() {
        return slash.getMember();
    }

    public User getAuthor() {
        return slash.getUser();
    }

    public Guild getGuild() {
        return getChannel().getGuild();
    }

    public RatelimitContext ratelimitContext() {
        return new RatelimitContext(getGuild(), null, getChannel(), null, slash);
    }

    public JDA getJDA() {
        return slash.getJDA();
    }

    public void reply(String source, Object... args) {
        slash.reply(i18n.get(source).formatted(args))
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public ReplyAction replyAction(String source, Object... args) {
        return slash.reply(i18n.get(source).formatted(args)).allowedMentions(EnumSet.noneOf(Message.MentionType.class));
    }

    public void reply(String text) {
        slash.reply(text)
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public ReplyAction replyAction(String text) {
        return slash.reply(text).allowedMentions(EnumSet.noneOf(Message.MentionType.class));
    }

    @Override
    public void send(String s) {
        reply(s);
    }

    @Override
    public void sendLocalized(String s, Object... args) {
        reply(s, args);
    }

    @Override
    public I18nContext getLanguageContext() {
        return getI18nContext();
    }

    public ManagedDatabase getDatabase() {
        return managedDatabase;
    }

    public Config getConfig() {
        return config;
    }

    public boolean isUserBlacklisted(String id) {
        return getMantaroData().getBlackListedUsers().contains(id);
    }

    public JedisPool getJedisPool() {
        return MantaroData.getDefaultJedisPool();
    }

    public MantaroBot getBot() {
        return MantaroBot.getInstance();
    }

    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), slash);
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
        return managedDatabase.getUser(getAuthor());
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
        return managedDatabase.getPlayer(getAuthor());
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

    // Cursed wrapper to get around null checks on getAsX
    public User getOptionAsUser(String name) {
        var option = getOption(name);
        if (option == null) {
            return null;
        }
        return option.getAsUser();
    }

    public String getOptionAsString(String name) {
        var option = getOption(name);
        if (option == null) {
            return null;
        }
        return option.getAsString();
    }

    public String getOptionAsString(String name, String def) {
        var option = getOption(name);
        if (option == null) {
            return def;
        }
        return option.getAsString();
    }

    public long getOptionAsLong(String name) {
        var option = getOption(name);
        if (option == null) {
            return 0;
        }
        return option.getAsLong();
    }

    public boolean getOptionAsBoolean(String name) {
        var option = getOption(name);
        if (option == null) {
            return false;
        }
        return option.getAsBoolean();
    }

}
