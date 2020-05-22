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

package net.kodehawa.mantarobot.commands.custom.v3;

public class Token {
    private final Position position;
    private final TokenType type;
    private final String value;

    public Token(Position position, TokenType type, String value) {
        this.position = position;
        this.type = type;
        this.value = value;
    }

    public Position position() {
        return position;
    }

    public TokenType type() {
        return type;
    }

    public String value() {
        return value;
    }

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
        if (!(obj instanceof Token)) {
            return false;
        }

        Token token = (Token) obj;
        return token.position.equals(position)
                && token.type == type && token.value.equals(value);
    }
}
