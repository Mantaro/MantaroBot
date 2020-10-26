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

package net.kodehawa.mantarobot.core.listeners.entities;

import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;

public class CachedMessage {
    private final long guildId;
    private final long author;
    private final String content;

    public CachedMessage(long guildId, long author, String content) {
        this.guildId = guildId;
        this.author = author;
        this.content = content;
    }

    public User getAuthor() {
        var guild = MantaroBot.getInstance().getShardManager().getGuildById(guildId);
        User user = null;

        if (guild != null)  {
            user = guild.retrieveMemberById(author).complete().getUser();
        }

        return user;
    }

    public String getContent() {
        return this.content;
    }
}
