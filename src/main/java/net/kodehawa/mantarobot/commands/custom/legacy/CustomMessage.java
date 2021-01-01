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

package net.kodehawa.mantarobot.commands.custom.legacy;

import net.dv8tion.jda.api.entities.Member;

import java.util.LinkedList;
import java.util.List;

public class CustomMessage {
    private final String message;
    private final List<Member> mentionedUsers;
    private final boolean mentionPrefix;

    public CustomMessage(String message, List<Member> mentionedUsers, boolean isMentionPrefix) {
        this.message = message;
        this.mentionedUsers = mentionedUsers;
        this.mentionPrefix = isMentionPrefix;
    }

    public String getContentRaw() {
        return message;
    }

    public List<Member> getMentionedUsers() {
        if (mentionPrefix) {
            var mutable = new LinkedList<>(mentionedUsers);
            return mutable.subList(1, mutable.size());
        }
        return mentionedUsers;
    }
}
