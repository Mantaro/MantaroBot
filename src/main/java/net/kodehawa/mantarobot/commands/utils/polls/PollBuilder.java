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

package net.kodehawa.mantarobot.commands.utils.polls;

import java.util.List;

public class PollBuilder {
    private String guildId;
    private String channelId;
    private String name;
    private String image;
    private List<String> options;
    private long time;

    public PollBuilder guildId(String guildId) {
        this.guildId = guildId;
        return this;
    }

    public PollBuilder channelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public PollBuilder name(String name) {
        this.name = name;
        return this;
    }

    public PollBuilder image(String image) {
        this.image = image;
        return this;
    }

    public PollBuilder options(List<String> options) {
        this.options = options;
        return this;
    }

    public PollBuilder time(long time) {
        this.time = time;
        return this;
    }

    public Poll build() {
        if (channelId == null)
            throw new IllegalArgumentException("Channel ID cannot be null.");
        if (guildId == null)
            throw new IllegalArgumentException("Guild ID cannot be null.");
        if (time <= 0)
            throw new IllegalArgumentException("Time to remind must be positive and > 0.");
        if (options.isEmpty())
            throw new IllegalArgumentException("Options can't be empty.");
        if (options.size() < 2)
            throw new IllegalArgumentException("Too few options.");
        if (options.size() > 9)
            throw new IllegalArgumentException("Too many options.");
        if (name == null || name.length() > 500)
            throw new IllegalArgumentException("Empty or invalid (over 500 characters) name.");

        return new Poll(guildId, channelId, null, name, image, options, time);
    }
}
