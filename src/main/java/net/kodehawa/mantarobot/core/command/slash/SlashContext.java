package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitContext;
import redis.clients.jedis.JedisPool;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SlashContext implements IContext {
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private final SlashCommandEvent slash;
    private final I18nContext i18n;
    private boolean deferred = false;

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

    public SlashCommandEvent getEvent() {
        return slash;
    }

    public String getSubCommand() {
        return slash.getSubcommandName();
    }

    public OptionMapping getOption(String name) {
        return slash.getOption(name);
    }

    public void defer() {
        slash.deferReply().queue();
        deferred = true;
    }

    public void deferEphemeral() {
        slash.deferReply().queue();
        deferred = true;
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

    public User getSelfUser() {
        return slash.getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return getGuild().getSelfMember();
    }

    public Color getMemberColor(Member member) {
        return member.getColor() == null ? Color.PINK : member.getColor();
    }

    public Color getMemberColor() {
        return getMember().getColor() == null ? Color.PINK : getMember().getColor();
    }

    public RatelimitContext ratelimitContext() {
        return new RatelimitContext(getGuild(), null, getChannel(), null, slash);
    }

    public JDA getJDA() {
        return slash.getJDA();
    }

    public void reply(String source, Object... args) {
        if (deferred) {
            slash.reply(i18n.get(source).formatted(args)).queue();
        } else {
            slash.deferReply()
                    .setContent(i18n.get(source).formatted(args))
                    .queue();
        }
    }

    public void replyEphemeral(String source, Object... args) {
        if (deferred) {
            slash.reply(i18n.get(source).formatted(args)).queue();
        } else {
            slash.deferReply(true)
                    .setContent(i18n.get(source).formatted(args))
                    .queue();
        }
    }

    public ReplyAction replyAction(String source, Object... args) {
        if (deferred) {
            return slash.reply(i18n.get(source).formatted(args));
        } else {
            return slash.deferReply().setContent(i18n.get(source).formatted(args));
        }
    }

    public void reply(String text) {
        if (deferred) {
            slash.reply(text).queue();
        } else {
            slash.deferReply()
                    .setContent(text)
                    .queue();
        }
    }

    public void reply(MessageEmbed embed) {
        if (deferred) {
            slash.replyEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            slash.deferReply().addEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }

    public void replyEphemeral(MessageEmbed embed) {
        if (deferred) {
            slash.replyEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            slash.deferReply(true).addEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }

    public ReplyAction replyAction(String text) {
        if (deferred) {
            return slash.reply(text);
        } else {
            return slash.deferReply().setContent(text);
        }
    }

    public ReplyAction replyAction(MessageEmbed embed) {
        if (deferred) {
            return slash.replyEmbeds(embed);
        } else {
            return slash.deferReply().addEmbeds(embed);
        }
    }

    public WebhookMessageUpdateAction<Message> editAction(MessageEmbed embed) {
        return slash.getHook().editOriginalEmbeds(embed).setContent("");
    }

    public void edit(MessageEmbed embed) {
        slash.getHook().editOriginalEmbeds(embed).setContent("").queue();
    }

    public void edit(String s) {
        slash.getHook().editOriginal(s).setEmbeds(Collections.emptyList()).queue();
    }

    public void edit(String s, Object... args) {
        slash.getHook().editOriginal(getLanguageContext().get(s.formatted(args)))
                .setEmbeds(Collections.emptyList())
                .queue();
    }

    public WebhookMessageUpdateAction<Message> editAction(String s) {
        return slash.getHook().editOriginal(s).setEmbeds(Collections.emptyList());
    }

    public void send(MessageEmbed embed, ActionRow... actionRow) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        if (deferred) {
            slash.replyEmbeds(embed)
                    .addActionRows(actionRow)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            slash.deferReply()
                    .addEmbeds(embed)
                    .addActionRows(actionRow)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }

    @Override
    public void sendFormat(String message, Object... format) {
        reply(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format));
    }

    @Override
    public void sendFormat(String message, Collection<ActionRow> actionRow, Object... format) {
        if (deferred) {
            slash.reply(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format))
                    .addActionRows(actionRow)
                    .queue();
        } else {
            slash.deferReply()
                    .setContent(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format))
                    .addActionRows(actionRow)
                    .queue();
        }
    }

    @Override
    public void send(String s) {
        reply(s);
    }

    @Override
    public Message sendResult(String s) {
        return slash.getHook().sendMessage(s).complete();
    }

    @Override
    public Message sendResult(MessageEmbed e) {
        return slash.getHook().sendMessageEmbeds(e).complete();
    }
    @Override
    public void send(MessageEmbed embed) {
        reply(embed);
    }

    @Override
    public void sendLocalized(String s, Object... args) {
        reply(s, args);
    }

    @Override
    public I18nContext getLanguageContext() {
        return getI18nContext();
    }

    public ManagedDatabase db() {
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
        return new UtilsContext(getGuild(), getMember(), getChannel(), getLanguageContext(), slash);
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

    public Marriage getMarriage(UserData userData) {
        return MantaroData.db().getMarriage(userData.getMarriageId());
    }

    // Cursed wrapper to get around null checks on getAsX
    public Role getOptionAsRole(String name) {
        var option = getOption(name);
        if (option == null) {
            return null;
        }

        return option.getAsRole();
    }

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
