/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.StringUtils;

import java.util.Map;

public class Context {
    private final MantaroBot bot = MantaroBot.getInstance();
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();

    private final GuildMessageReceivedEvent event;
    private final I18nContext languageContext;
    private final Member member;
    private final User user;
    private final Guild guild;
    private final TextChannel channel;
    private final String content;

    public Context(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
        this.event = event;
        this.languageContext = languageContext;
        this.member = event.getMember();
        this.user = event.getAuthor();
        this.guild = event.getGuild();
        this.channel = event.getChannel();
        this.content = content;
    }

    public MantaroBot getBot() {
        return bot;
    }

    public Config getConfig() {
        return config;
    }

    public ManagedDatabase getManagedDatabase() {
        return managedDatabase;
    }

    public GuildMessageReceivedEvent getEvent() {
        return event;
    }

    public I18nContext getLanguageContext() {
        return languageContext;
    }

    public Member getMember() {
        return member;
    }

    public User getUser() {
        return user;
    }

    public Guild getGuild() {
        return guild;
    }

    public SelfUser getSelfUser() {
        return event.getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return guild.getSelfMember();
    }

    public TextChannel getChannel() {
        return channel;
    }

    public DBGuild getDBGuild() {
        return managedDatabase.getGuild(guild);
    }
    public DBUser getDBUser() {
        return managedDatabase.getUser(user);
    }

    public DBUser getDBUser(User user) {
        return managedDatabase.getUser(user);
    }

    public Player getPlayer() {
        return managedDatabase.getPlayer(user);
    }

    public Player getPlayer(User user) {
        return managedDatabase.getPlayer(user);
    }

    public String[] getArguments() {
        return StringUtils.advancedSplitArgs(content, 0);
    }

    public Map<String, String> getOptionalArguments() {
        return StringUtils.parse(getArguments());
    }

    public void send(Message message) {
        channel.sendMessage(message).queue();
    }

    public void send(String message) {
        channel.sendMessage(message).queue();
    }

    public void send(MessageEmbed embed) {
        channel.sendMessage(embed).queue();
    }

    public void sendLocalized(String localizedMessage, Object... args) {
        channel.sendMessageFormat(languageContext.get(localizedMessage), args).queue();
    }

    public void sendStripped(String message) {
        new MessageBuilder().setContent(message)
                .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                .sendTo(channel)
                .queue();
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        new MessageBuilder().setContent(String.format(localizedMessage, args))
                .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                .sendTo(channel)
                .queue();
    }
}

