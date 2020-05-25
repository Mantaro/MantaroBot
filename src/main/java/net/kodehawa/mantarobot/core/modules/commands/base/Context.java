/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.StringUtils;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;

public class Context {
    private final MantaroBot bot = MantaroBot.getInstance();
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();

    private final GuildMessageReceivedEvent event;
    private final I18nContext languageContext;
    private final String content;

    public Context(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
        this.event = event;
        this.languageContext = languageContext;
        this.content = content;
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
        return getEvent().getMessage().getMentionedUsers();
    }

    public List<Member> getMentionedMembers() {
        return getEvent().getMessage().getMentionedMembers();
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

    public boolean isSeasonal() {
        Map<String, String> optionalArguments = getOptionalArguments();
        return optionalArguments.containsKey("season") || optionalArguments.containsKey("s");
    }

    public boolean hasReactionPerms() {
        return getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_ADD_REACTION);
    }

    public String[] getArguments() {
        return StringUtils.advancedSplitArgs(content, 0);
    }

    public Map<String, String> getOptionalArguments() {
        return StringUtils.parse(getArguments());
    }

    public void send(Message message) {
        getChannel().sendMessage(message).queue();
    }

    public void send(String message) {
        getChannel().sendMessage(message).queue();
    }

    public void sendFormat(String message, Object... format) {
        getChannel().sendMessageFormat(message, format).queue();
    }

    public void send(MessageEmbed embed) {
        getChannel().sendMessage(embed).queue();
    }

    public void sendLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessageFormat(languageContext.get(localizedMessage), args).queue();
    }

    public void sendLocalized(String localizedMessage) {
        getChannel().sendMessage(languageContext.get(localizedMessage)).queue();
    }

    public void sendStripped(String message) {
        new MessageBuilder().setContent(message)
                .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                .sendTo(getChannel())
                .queue();
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        new MessageBuilder().setContent(String.format(languageContext.get(localizedMessage), args))
                .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                .sendTo(getChannel())
                .queue();
    }

    public JedisPool getJedisPool() {
        return MantaroData.getDefaultJedisPool();
    }
}
