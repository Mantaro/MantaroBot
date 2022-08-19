/*
 * Copyright (C) 2016-2022 David Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.utils.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

public class UtilsContext {
    private final Guild guild;
    private final Member member;
    private final GuildMessageChannel channel;
    private final GenericCommandInteractionEvent slashEvent;
    private final I18nContext languageContext;

    public UtilsContext(Guild guild, Member member, GuildMessageChannel channel, I18nContext languageContext, GenericCommandInteractionEvent event) {
        this.guild = guild;
        this.member = member;
        this.channel = channel;
        this.slashEvent = event;
        this.languageContext = languageContext;
    }

    public I18nContext getLanguageContext() {
        return languageContext;
    }

    public Guild getGuild() {
        return guild;
    }

    public Member getMember() {
        return member;
    }

    public GuildMessageChannel getChannel() {
        return channel;
    }

    public User getAuthor() {
        return member.getUser();
    }

    public Message send(String message) {
        if (slashEvent == null)
            return channel.sendMessage(message).complete();
        else {
            if (!slashEvent.isAcknowledged()) {
                slashEvent.deferReply().complete();
            }

            return slashEvent.getHook().editOriginal(message).complete();
        }
    }

    public Message send(MessageEmbed message) {
        if (slashEvent == null)
            return channel.sendMessageEmbeds(message).complete();
        else {
            if (!slashEvent.isAcknowledged()) {
                slashEvent.deferReply().complete();
            }

            return slashEvent.getHook().editOriginalEmbeds(message).setContent("").complete();
        }
    }
}
