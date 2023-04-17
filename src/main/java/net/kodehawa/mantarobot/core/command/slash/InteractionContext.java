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

package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.GuildDatabase;
import net.kodehawa.mantarobot.db.entities.MantaroObject;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.UserDatabase;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

// The fact this is basically a copy of SlashContext makes me sad.
// But just in case I need anything special for this...
public class InteractionContext<T> implements IContext {
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private final GenericContextInteractionEvent<T> event;
    private final I18nContext i18n;
    private boolean deferred = false;
    
    public InteractionContext(GenericContextInteractionEvent<T> event, I18nContext i18n) {
        this.event = event;
        this.i18n = i18n;
    }

    public I18nContext getI18nContext() {
        return i18n;
    }

    public GenericContextInteractionEvent<T> getEvent() {
        return event;
    }

    public T getTarget() {
        return event.getTarget();
    }

    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    @Override
    public GuildMessageChannel getChannel() {
        return null; // Assume nothing?
    }

    public GuildChannel getGuildChannel() {
        return event.getGuildChannel();
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
    public RateLimitContext ratelimitContext() {
        return new RateLimitContext(getGuild(), null, getChannel(), null, event);
    }

    @Override
    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), getLanguageContext(), event);
    }

    public void defer() {
        event.deferReply(true).complete();
        deferred = true;
    }

    public void replyRaw(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(source.formatted(args)).queue();
        } else {
            event.reply(source.formatted(args)).setEphemeral(true).queue();
        }
    }

    public void reply(String source, Object... args) {
        if (deferred) {
            event.getHook().sendMessage(i18n.get(source).formatted(args)).queue();
        } else {
            event.reply(i18n.get(source).formatted(args)).setEphemeral(true).queue();
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
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void reply(String text) {
        if (deferred) {
            event.getHook().sendMessage(text).setEphemeral(true).queue();
        } else {
            event.reply(text).setEphemeral(true).queue();
        }
    }

    public void reply(MessageCreateData message) {
        if (deferred) {
            event.getHook().sendMessage(message).setEphemeral(true).queue();
        } else {
            event.reply(message.getContent()).setEphemeral(true).queue();
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
                    .setEphemeral(true)
                    .queue();
        }
    }

    public void reply(MessageEmbed embed) {
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
            event.deferReply().complete();
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

        // Assume its stripped already? No stripped version.
        event.getHook().editOriginal(s)
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
                .setEmbeds(Collections.emptyList())
                .setComponents()
                .queue();
    }

    public WebhookMessageEditAction<Message> editAction(String s) {
        if (!event.isAcknowledged()) {
            event.deferReply().complete();
        }

        return event.getHook().editOriginal(s).setEmbeds(Collections.emptyList());
    }

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
                    .setEphemeral(true)
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
            event.deferReply().complete();
        }

        return event.getHook().sendMessage(s).setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)).complete();
    }

    @Override
    public Message sendResult(MessageEmbed e) {
        if (!event.isAcknowledged()) {
            event.deferReply().complete();
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
    public ManagedDatabase db() {
        return managedDatabase;
    }

    @Override
    public I18nContext getLanguageContext() {
        return getI18nContext();
    }

    public GuildDatabase getDBGuild() {
        return managedDatabase.getGuild(getGuild());
    }

    public UserDatabase getDBUser() {
        return managedDatabase.getUser(getAuthor());
    }

    public UserDatabase getDBUser(User user) {
        return managedDatabase.getUser(user);
    }

    @Override
    public ShardManager getShardManager() {
        return event.getJDA().getShardManager();
    }

    public Player getPlayer() {
        return managedDatabase.getPlayer(getAuthor());
    }

    public Player getPlayer(User user) {
        return managedDatabase.getPlayer(user);
    }

    public MantaroObject getMantaroData() {
        return managedDatabase.getMantaroData();
    }

    @Override
    public Config getConfig() {
        return config;
    }
}
