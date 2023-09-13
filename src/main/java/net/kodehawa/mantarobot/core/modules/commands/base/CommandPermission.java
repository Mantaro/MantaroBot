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

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.function.Predicate;

public enum CommandPermission {
    USER(member -> true),
    ADMIN(
            member -> member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) ||
                    member.hasPermission(Permission.MANAGE_SERVER) || MantaroData.config().get().isOwner(member) ||
                    member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("Bot Commander"))
    ),
    OWNER(member -> MantaroData.config().get().isOwner(member)),
    INHERIT(member -> {
        throw new UnsupportedOperationException("Used by NewCommand to inherit from parent");
    });

    private final Predicate<Member> predicate;

    CommandPermission(Predicate<Member> predicate) {
        this.predicate = predicate;
    }

    public boolean test(Member member) {
        return predicate.test(member);
    }

    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
