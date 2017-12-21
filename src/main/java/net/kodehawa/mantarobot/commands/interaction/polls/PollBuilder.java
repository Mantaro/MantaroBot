/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.interaction.polls;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.junit.Assert;

import java.util.UUID;

public class PollBuilder {
    private GuildMessageReceivedEvent event;
    private String name = "";
    private String[] options;
    private long timeout;

    public PollBuilder setEvent(GuildMessageReceivedEvent event) {
        this.event = event;
        return this;
    }

    public PollBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PollBuilder setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public PollBuilder setOptions(String... options) {
        this.options = options;
        return this;
    }

    public Poll build() {
        if(options == null)
            throw new IllegalArgumentException("Cannot create a poll with null options");
        if(event == null)
            throw new IllegalArgumentException("Cannot create a poll with null event");
        if(timeout == 0)
            throw new IllegalArgumentException("Cannot create a poll without a timeout");

        return new Poll(UUID.randomUUID().toString(), event.getGuild().getId(), event.getChannel().getId(), event.getAuthor().getId(), name, timeout, options);
    }
}
