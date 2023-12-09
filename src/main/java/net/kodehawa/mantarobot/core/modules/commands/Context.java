/*
 * Copyright (C) 2016 Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.modules.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.command.slash.IContext;
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
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;
import redis.clients.jedis.JedisPool;

import java.awt.Color;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused") // class will die eventually
public class Context implements IContext {
    private final MantaroBot bot = MantaroBot.getInstance();
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();

    private final MessageReceivedEvent event;
    private final String content;
    private final boolean isMentionPrefix;
    private I18nContext languageContext;
    private String commandName = "";
    private String customContent;

    public Context(MessageReceivedEvent event, I18nContext languageContext, String content, boolean isMentionPrefix) {
        this.event = event;
        this.languageContext = languageContext;
        this.content = content;
        this.isMentionPrefix = isMentionPrefix;
    }

    public Context(MessageReceivedEvent event, I18nContext languageContext, String cmdName, String content, boolean isMentionPrefix) {
        this.event = event;
        this.languageContext = languageContext;
        this.content = content;
        this.isMentionPrefix = isMentionPrefix;
        this.commandName = cmdName;
    }

    public MantaroBot getBot() {
        return bot;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public ManagedDatabase db() {
        return managedDatabase;
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    public JDA getJDA() {
        return getEvent().getJDA();
    }

    @Override
    public I18nContext getLanguageContext() {
        return languageContext;
    }

    public I18nContext getGuildLanguageContext() {
        return new I18nContext(getDBGuild(), null);
    }

    public List<User> getMentionedUsers() {
        final var mentionedUsers = getEvent().getMessage().getMentions().getUsers();
        if (isMentionPrefix) {
            final var mutable = new LinkedList<>(mentionedUsers);
            return mutable.subList(1, mutable.size());
        }

        return mentionedUsers;
    }

    public List<Member> getMentionedMembers() {
        final var mentionedMembers = getEvent().getMessage().getMentions().getMembers();
        if (isMentionPrefix) {
            final var mutable = new LinkedList<>(mentionedMembers);
            return mutable.subList(1, mutable.size());
        }

        return mentionedMembers;
    }

    @Override
    public RateLimitContext ratelimitContext() {
        return new RateLimitContext(getGuild(), getMessage(), getChannel(), getEvent(), null);
    }

    @Override
    public Member getMember() {
        return event.getMember();
    }

    public User getUser() {
        return event.getAuthor();
    }

    @Override
    public User getAuthor() {
        return getUser();
    }

    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    public Message getMessage() {
        return event.getMessage();
    }

    public SelfUser getSelfUser() {
        return event.getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return getGuild().getSelfMember();
    }

    @Override
    public GuildMessageChannel getChannel() {
        return event.getGuildChannel();
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
        return managedDatabase.getUser(getUser());
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
        return managedDatabase.getPlayer(getUser());
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

    public String getCommandName() {
        return commandName;
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

    public boolean hasReactionPerms() {
        return getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_ADD_REACTION) &&
                // Somehow also needs this?
                getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_HISTORY);
    }

    public String getContent() {
        return content;
    }

    public String[] getArguments() {
        return StringUtils.advancedSplitArgs(content, 0);
    }

    public Map<String, String> getOptionalArguments() {
        return StringUtils.parseArguments(getArguments());
    }

    public Marriage getMarriage(MongoUser userData) {
        return MantaroData.db().getMarriage(userData.getMarriageId());
    }

    @Override
    public void send(MessageCreateData message) {
        getChannel().sendMessage(message).queue();
    }

    @Override
    public void send(String message) {
        getChannel().sendMessage(message).queue();
    }

    @Override
    public Message sendResult(String s) {
        return getChannel().sendMessage(s).complete();
    }

    @Override
    public Message sendResult(MessageEmbed e) {
        return getChannel().sendMessageEmbeds(e).complete();
    }

    public Color getMemberColor(Member member) {
        return member.getColor() == null ? Color.PINK : member.getColor();
    }

    public Color getMemberColor() {
        return getMemberColor(getMember());
    }

    public void send(String message, ActionRow... actionRow) {
        getChannel().sendMessage(message).setComponents(actionRow).queue();
    }

    @Override
    public void sendFormat(String message, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).queue();
    }

    @Override
    public void sendFormatStripped(String message, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)).queue();
    }

    @Override
    public void sendFormat(String message, Collection<ActionRow> actionRow, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).setComponents(actionRow).queue();
    }

    @Override
    public void send(MessageEmbed embed, ActionRow... actionRow) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessageEmbeds(embed)
                .setComponents(actionRow).queue(success -> {}, Throwable::printStackTrace);
    }

    @Override
    public void send(MessageEmbed embed) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessageEmbeds(embed)
                .queue(success -> {}, Throwable::printStackTrace);
    }

    @Override
    public void sendLocalized(String localizedMessage, Object... args) {
        // Stop swallowing issues with String replacements (somehow really common)
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), languageContext.get(localizedMessage), args)
        ).queue(success -> {}, Throwable::printStackTrace);
    }

    @Override
    public void sendLocalizedStripped(String s, Object... args) {
        getChannel().sendMessage(languageContext.get(s).formatted(args))
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public void sendLocalized(String localizedMessage, Collection<ActionRow> actionRow, Object... args) {
        // Stop swallowing issues with String replacements (somehow really common)
        getChannel().sendMessage(String.format(Utils.getLocaleFromLanguage(getLanguageContext()), languageContext.get(localizedMessage), args))
                .setComponents(actionRow).queue(success -> {}, Throwable::printStackTrace);
    }


    public void sendLocalized(String localizedMessage) {
        getChannel().sendMessage(languageContext.get(localizedMessage)).queue();
    }

    public void sendLocalized(String localizedMessage, ActionRow... actionRow) {
        getChannel().sendMessage(languageContext.get(localizedMessage)).setComponents(actionRow).queue();
    }

    @Override
    public void sendStripped(String message) {
        getChannel().sendMessage(message)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessage(String.format(
                Utils.getLocaleFromLanguage(getLanguageContext()), languageContext.get(localizedMessage), args)
        ).setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)).queue();
    }

    public void findMember(String query, Consumer<List<Member>> success) {
        CustomFinderUtil.lookupMember(getGuild(), this, query).onSuccess(s -> {
            try {
                success.accept(s);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void sendFile(byte[] file, String name) {
        getChannel().sendFiles(FileUpload.fromData(file, name)).queue();
    }

    public boolean isUserBlacklisted(String id) {
        return getMantaroData().getBlackListedUsers().contains(id);
    }

    public User retrieveUserById(String id) {
        User user = null;
        try {
            user = MantaroBot.getInstance().getShardManager().retrieveUserById(id).complete();
        } catch (Exception ignored) { }

        return user;
    }

    public Member retrieveMemberById(Guild guild, String id, boolean update) {
        Member member = null;
        try {
            member = guild.retrieveMemberById(id).useCache(!update).complete();
        } catch (Exception ignored) { }

        return member;
    }

    public Member retrieveMemberById(String id, boolean update) {
        Member member = null;
        try {
            member = getGuild().retrieveMemberById(id).complete();
        } catch (Exception ignored) { }

        return member;
    }

    public boolean isMentionPrefix() {
        return isMentionPrefix;
    }

    public JedisPool getJedisPool() {
        return MantaroData.getDefaultJedisPool();
    }

    public void setLanguageContext(I18nContext languageContext) {
        this.languageContext = languageContext;
    }

    @Override
    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), languageContext, null);
    }

    public boolean isChannelNSFW() {
        if (getChannel() instanceof IAgeRestrictedChannel txtChannel) {
            return txtChannel.isNSFW();
        }

        return true;
    }

    // Both used for options.
    public void setCustomContent(String str) {
        this.customContent = str;
    }

    /**
     * Get the custom (usually filtered) content. This is only used in options, do not call it anywhere else.
     * @return The custom content that has been set using Context#setCustomContent
     */
    public String getCustomContent() {
        return this.customContent;
    }
}
