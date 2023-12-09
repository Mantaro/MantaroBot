package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MantaroObject;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;
import redis.clients.jedis.JedisPool;

import java.awt.Color;
import java.util.*;

@SuppressWarnings("unused")
public abstract class BaseInteractionContext<T extends GenericCommandInteractionEvent> implements IContext {
    protected final ManagedDatabase managedDatabase = MantaroData.db();
    protected final Config config = MantaroData.config().get();
    protected final T event;
    protected final I18nContext i18n;
    protected boolean deferred = false;
    private boolean forceEphemeral = false;

    public BaseInteractionContext(T event, I18nContext i18n) {
        this.event = event;
        this.i18n = i18n;
    }

    public I18nContext getI18nContext() {
        return i18n;
    }

    public String getName() {
        return event.getName();
    }

    public T getEvent() {
        return event;
    }

    public void defer() {
        if (forceEphemeral) {
            deferEphemeral();
        } else {
            event.deferReply().complete();
            deferred = true;
        }
    }

    public void deferEphemeral() {
        event.deferReply(true).complete();
        deferred = true;
    }

    @Override
    public GuildMessageChannel getChannel() {
        if (event.getChannel() instanceof GuildMessageChannel c) {
            return c;
        }
        return null;
    }


    public boolean isChannelNSFW() {
        if (getChannel() instanceof IAgeRestrictedChannel txtChannel) {
            return txtChannel.isNSFW();
        }

        return true;
    }

    @Override
    public Member getMember() {
        return event.getMember();
    }

    @Override
    public User getAuthor() {
        return event.getUser();
    }

    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    public User getSelfUser() {
        return event.getJDA().getSelfUser();
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


    @Override
    public RateLimitContext ratelimitContext() {
        return new RateLimitContext(getGuild(), null, getChannel(), null, event);
    }

    public JDA getJDA() {
        return event.getJDA();
    }

    public void replyRaw(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(source.formatted(args)).queue();
        } else {
            event.reply(source.formatted(args)).setEphemeral(forceEphemeral).queue();
        }
    }

    public void reply(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(source).formatted(args)).queue();
        } else {
            event.reply(i18n.get(source).formatted(args)).setEphemeral(forceEphemeral).queue();
        }
    }

    public void replyStripped(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(source).formatted(args))
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .queue();
        } else {
            event.reply(i18n.get(source).formatted(args))
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .setEphemeral(forceEphemeral)
                    .queue();
        }
    }

    public void replyLocalized(String text) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(text)).setEphemeral(forceEphemeral).queue();
        } else {
            event.reply(i18n.get(text)).setEphemeral(forceEphemeral).queue();
        }
    }

    public void reply(String text) {
        if (deferred) {
            event.getHook().sendMessage(text).setEphemeral(forceEphemeral).queue();
        } else {
            event.reply(text).setEphemeral(forceEphemeral).queue();
        }
    }

    public void reply(MessageCreateData message) {
        if (deferred) {
            event.getHook().sendMessage(message).queue();
        } else {
            event.reply(message).setEphemeral(forceEphemeral).queue();
        }
    }

    public void replyStripped(String text) {
        if (deferred) {
            event.getHook().sendMessage(text)
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .queue();
        } else {
            event.reply(text)
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .setEphemeral(forceEphemeral)
                    .queue();
        }
    }

    public void reply(MessageEmbed embed) {
        if (deferred) {
            event.getHook().sendMessageEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            event.replyEmbeds(embed)
                    .setEphemeral(forceEphemeral)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }

    public void replyEphemeralRaw(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(source.formatted(args)).queue();
        } else {
            event.reply(source.formatted(args))
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void replyEphemeral(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(source).formatted(args)).queue();
        } else {
            event.reply(i18n.get(source).formatted(args))
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void replyEphemeralStripped(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(source).formatted(args))
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .queue();
        } else {
            event.reply(i18n.get(source).formatted(args))
                    .setEphemeral(true)
                    .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                    .queue();
        }
    }

    public void replyEphemeral(MessageEmbed embed) {
        if (deferred) {
            event.getHook().sendMessageEmbeds(embed)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            event.replyEmbeds(embed)
                    .setEphemeral(true)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }

    public void replyModal(Modal modal) {
        if (deferred) {
            throw new IllegalStateException("Cannot reply to a deferred interaction with a modal.");
        }

        event.replyModal(modal).queue();
        deferred = true; // This will defer it!
    }

    public WebhookMessageEditAction<Message> editAction(MessageEmbed embed) {
        if (!event.isAcknowledged()) {
            event.deferReply().setEphemeral(forceEphemeral).complete();
        }

        return event.getHook().editOriginalEmbeds(embed).setContent("");
    }

    public void edit(MessageEmbed embed) {
        if (!event.isAcknowledged()) {
            reply(embed);
            return;
        }

        event.getHook().editOriginalEmbeds(embed).setContent("")
                .queue(success -> {}, Throwable::printStackTrace);
    }

    public void edit(String s) {
        if (!event.isAcknowledged()) {
            replyRaw(s);
            return;
        }

        event.getHook().editOriginal(s).setEmbeds(Collections.emptyList()).queue();
    }

    public void editStripped(String s) {
        if (!event.isAcknowledged()) {
            replyStripped(s);
            return;
        }

        event.getHook().editOriginal(s)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .setEmbeds(Collections.emptyList())
                .queue();
    }


    public void edit(String s, Object... args) {
        if (!event.isAcknowledged()) {
            reply(s, args);
            return;
        }

        event.getHook().editOriginal(i18n.get(s).formatted(args))
                .setEmbeds(Collections.emptyList())
                .setComponents()
                .queue();
    }

    public void editStripped(String s, Object... args) {
        if (!event.isAcknowledged()) {
            replyStripped(s, args);
            return;
        }

        event.getHook().editOriginal(i18n.get(s).formatted(args))
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .setEmbeds(Collections.emptyList())
                .setComponents()
                .queue();
    }

    public WebhookMessageEditAction<Message> editAction(String s) {
        if (!event.isAcknowledged()) {
            event.deferReply().setEphemeral(forceEphemeral).complete();
        }

        return event.getHook().editOriginal(s).setEmbeds(Collections.emptyList());
    }

    @Override
    public void send(MessageEmbed embed, ActionRow... actionRow) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        if (deferred) {
            event.getHook().sendMessageEmbeds(embed)
                    .setComponents(actionRow)
                    .queue(success -> {}, Throwable::printStackTrace);
        } else {
            event.replyEmbeds(embed)
                    .setComponents(actionRow)
                    .setEphemeral(forceEphemeral)
                    .queue(success -> {}, Throwable::printStackTrace);
        }
    }


    @Override
    public void sendFormat(String message, Object... format) {
        reply(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format));
    }

    @Override
    public void sendFormatStripped(String message, Object... format) {
        replyStripped(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format));
    }

    @Override
    public void sendFormat(String message, Collection<ActionRow> actionRow, Object... format) {
        if (deferred) {
            event.getHook().sendMessage(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format))
                    .setComponents(actionRow)
                    .queue();
        } else {
            event.reply(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format))
                    .setEphemeral(forceEphemeral)
                    .setComponents(actionRow)
                    .queue();
        }
    }

    @Override
    public void send(String s) {
        reply(s);
    }

    @Override
    public void send(MessageCreateData message) {
        reply(message);
    }

    @Override
    public void sendStripped(String s) {
        replyStripped(s);
    }

    @Override
    public Message sendResult(String s) {
        if (!event.isAcknowledged()) {
            event.deferReply().setEphemeral(forceEphemeral).complete();
        }

        return event.getHook().sendMessage(s).setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)).complete();
    }

    @Override
    public Message sendResult(MessageEmbed e) {
        if (!event.isAcknowledged()) {
            event.deferReply().setEphemeral(forceEphemeral).complete();
        }

        return event.getHook().sendMessageEmbeds(e).complete();
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
    public void sendLocalizedStripped(String s, Object... args) {
        replyStripped(s, args);
    }

    @Override
    public I18nContext getLanguageContext() {
        return getI18nContext();
    }

    public I18nContext getGuildLanguageContext() {
        return new I18nContext(getDBGuild(), null);
    }

    @Override
    public ManagedDatabase db() {
        return managedDatabase;
    }

    @Override
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

    @Override
    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), getLanguageContext(), event);
    }

    public User retrieveUserById(String id) {
        return event.getJDA().retrieveUserById(id).complete();
    }

    public MantaroAudioManager getAudioManager() {
        return getBot().getAudioManager();
    }

    @Override
    public ShardManager getShardManager() {
        return getBot().getShardManager();
    }

    @Override
    public MongoGuild getDBGuild() {
        return managedDatabase.getGuild(getGuild());
    }

    @Override
    public MongoUser getDBUser() {
        return managedDatabase.getUser(getAuthor());
    }

    @Override
    public MongoUser getDBUser(User user) {
        return managedDatabase.getUser(user);
    }

    public MongoUser getDBUser(Member member) {
        return managedDatabase.getUser(member);
    }

    public MongoUser getDBUser(String id) {
        return managedDatabase.getUser(id);
    }

    @Override
    public Player getPlayer() {
        return managedDatabase.getPlayer(getAuthor());
    }

    @Override
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

    @Override
    public MantaroObject getMantaroData() {
        return managedDatabase.getMantaroData();
    }

    public Marriage getMarriage(MongoUser userData) {
        return MantaroData.db().getMarriage(userData.getMarriageId());
    }

    public void setForceEphemeral(boolean force) {
        this.forceEphemeral = force;
    }
}
