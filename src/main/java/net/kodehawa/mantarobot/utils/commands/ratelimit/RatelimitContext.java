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

package net.kodehawa.mantarobot.utils.commands.ratelimit;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RatelimitContext {
    private final Guild guild;
    private final Message message;
    private final GuildMessageChannel channel;
    private final MessageReceivedEvent event;
    private final GenericCommandInteractionEvent slashEvent;

    public RatelimitContext(Guild guild, Message message, GuildMessageChannel channel, MessageReceivedEvent event, GenericCommandInteractionEvent slashEvent) {
        this.guild = guild;
        this.message = message;
        this.channel = channel;
        this.event = event;
        this.slashEvent = slashEvent;
    }

    public Guild getGuild() {
        return guild;
    }

    public Message getMessage() {
        return message;
    }

    public GuildMessageChannel getChannel() {
        return channel;
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    public GenericCommandInteractionEvent getSlashEvent() {
        return slashEvent;
    }

    public void send(String message) {
        if (slashEvent != null) {
            if (slashEvent.isAcknowledged()) {
                slashEvent.getHook().sendMessage(message).queue();
            } else {
                slashEvent.reply(message).queue();
            }
        } else {
            event.getChannel().sendMessage(message).queue();
        }
    }
}
