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

package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.argument.split.SplitString;

class DelimiterContext {
    private final StringBuilder builder = new StringBuilder();
    private final char delimiter;
    private final boolean allowEscaping;
    private boolean escaped;
    private boolean insideBlock;

    DelimiterContext(char delimiter, boolean allowEscaping) {
        this.delimiter = delimiter;
        this.allowEscaping = allowEscaping;
    }

    String result() {
        return builder.toString();
    }

    boolean handle(SplitString string) {
        builder.append(string.getPreviousWhitespace());
        String value = string.getValue();
        for(int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                escaped = false;
                builder.append(c);
                continue;
            }
            if (c == delimiter) {
                insideBlock = !insideBlock;
                continue;
            }
            if (allowEscaping && c == '\\') {
                escaped = true;
                continue;
            }

            builder.append(c);
        }
        return insideBlock || escaped;
    }
}
