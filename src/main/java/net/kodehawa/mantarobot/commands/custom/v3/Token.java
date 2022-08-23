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

package net.kodehawa.mantarobot.commands.custom.v3;

public record Token(Position position, TokenType type, String value) {
    @Override
    public int hashCode() {
        return value.hashCode() ^ position.hashCode();
    }

    @Override
    public String toString() {
        return "Token(" + position + ", " + type + ", '" + value + "')";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Token token)) {
            return false;
        }

        return token.position.equals(position)
                && token.type == type && token.value.equals(value);
    }
}
