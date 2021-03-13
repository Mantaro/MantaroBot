/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.*;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import redis.clients.jedis.JedisPool;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Context {
    private final MantaroBot bot = MantaroBot.getInstance();
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();

    private final GuildMessageReceivedEvent event;
    private final String content;
    private final boolean isMentionPrefix;
    private I18nContext languageContext;

    public Context(GuildMessageReceivedEvent event, I18nContext languageContext, String content, boolean isMentionPrefix) {
        this.event = event;
        this.languageContext = languageContext;
        this.content = content;
        this.isMentionPrefix = isMentionPrefix;
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

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public JDA getJDA() {
        return getEvent().getJDA();
    }

    public I18nContext getLanguageContext() {
        return languageContext;
    }

    public I18nContext getGuildLanguageContext() {
        return new I18nContext(getDBGuild().getData(), null);
    }

    public List<User> getMentionedUsers() {
        final var mentionedUsers = getEvent().getMessage().getMentionedUsers();
        if (isMentionPrefix) {
            final var mutable = new LinkedList<>(mentionedUsers);
            return mutable.subList(1, mutable.size());
        }

        return mentionedUsers;
    }

    public List<Member> getMentionedMembers() {
        final var mentionedMembers = getEvent().getMessage().getMentionedMembers();
        if (isMentionPrefix) {
            final var mutable = new LinkedList<>(mentionedMembers);
            return mutable.subList(1, mutable.size());
        }

        return mentionedMembers;
    }

    public Member getMember() {
        return event.getMember();
    }

    public User getUser() {
        return event.getAuthor();
    }

    public User getAuthor() {
        return getUser();
    }

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

    public TextChannel getChannel() {
        return event.getChannel();
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

    public SeasonPlayer getSeasonPlayer() {
        return managedDatabase.getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
    }

    public SeasonPlayer getSeasonPlayer(User user) {
        return managedDatabase.getPlayerForSeason(user, getConfig().getCurrentSeason());
    }

    public SeasonPlayer getSeasonPlayer(Member member) {
        return managedDatabase.getPlayerForSeason(member, getConfig().getCurrentSeason());
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

    public boolean isSeasonal() {
        Map<String, String> optionalArguments = getOptionalArguments();
        return optionalArguments.containsKey("season") || optionalArguments.containsKey("s");
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

    public Marriage getMarriage(UserData userData) {
        return MantaroData.db().getMarriage(userData.getMarriageId());
    }

    public void send(Message message) {
        getChannel().sendMessage(message).queue();
    }

    public void send(String message) {
        getChannel().sendMessage(message).queue();
    }

    public void sendFormat(String message, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).queue();
    }

    public void send(MessageEmbed embed) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessage(embed).queue(success -> {}, Throwable::printStackTrace);
    }

    public void sendLocalized(String localizedMessage, Object... args) {
        // Stop swallowing issues with String replacements (somehow really common)
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), languageContext.get(localizedMessage), args)
        ).queue(success -> {}, Throwable::printStackTrace);
    }

    public void sendLocalized(String localizedMessage) {
        getChannel().sendMessage(languageContext.get(localizedMessage)).queue();
    }

    public void sendStripped(String message) {
        getChannel().sendMessage(message)
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessage(String.format(
                Utils.getLocaleFromLanguage(getLanguageContext()), languageContext.get(localizedMessage), args)
        ).allowedMentions(EnumSet.noneOf(Message.MentionType.class)).queue();
    }

    public Task<List<Member>> findMember(String query, Consumer<List<Member>> success) {
        return CustomFinderUtil.lookupMember(getGuild(), this, query).onSuccess(s -> {
            try {
                success.accept(s);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
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
            member = guild.retrieveMemberById(id, update).complete();
        } catch (Exception ignored) { }

        return member;
    }

    public Member retrieveMemberById(String id, boolean update) {
        Member member = null;
        try {
            member = getGuild().retrieveMemberById(id, update).complete();
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
}
