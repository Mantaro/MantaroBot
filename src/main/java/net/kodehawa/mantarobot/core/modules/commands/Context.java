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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MantaroObject;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

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

    public Context(MessageReceivedEvent event, I18nContext languageContext, String cmdName, String content, boolean isMentionPrefix) {
        this.event = event;
        this.languageContext = languageContext;
        this.content = content;
        this.isMentionPrefix = isMentionPrefix;
        this.commandName = cmdName;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public ManagedDatabase db() {
        return managedDatabase;
    }

    @Override
    public I18nContext getLanguageContext() {
        return languageContext;
    }

    @Override
    public RateLimitContext ratelimitContext() {
        return new RateLimitContext(getGuild(), event.getMessage(), getChannel(), event, null);
    }

    @Override
    public Member getMember() {
        return event.getMember();
    }

    @Override
    public User getAuthor() {
        return event.getAuthor();
    }

    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    @Override
    public GuildMessageChannel getChannel() {
        return event.getGuildChannel();
    }

    @Override
    public ShardManager getShardManager() {
        return MantaroBot.getInstance().getShardManager();
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

    @Override
    public Player getPlayer() {
        return managedDatabase.getPlayer(getAuthor());
    }

    @Override
    public Player getPlayer(User user) {
        return managedDatabase.getPlayer(user);
    }

    @Override
    public MantaroObject getMantaroData() {
        return managedDatabase.getMantaroData();
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
        // Sending embeds while suppressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessageEmbeds(embed)
                .setComponents(actionRow).queue(success -> {}, Throwable::printStackTrace);
    }

    @Override
    public void send(MessageEmbed embed) {
        // Sending embeds while suppressing the failure callbacks leads to very hard
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

    @Override
    public void sendStripped(String message) {
        getChannel().sendMessage(message)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    @Override
    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), languageContext, null);
    }
}
