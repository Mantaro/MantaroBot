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

public class Position {
    private final int line;
    private final int column;
    private final int start;
    private final int end;

    public Position(int line, int column, int start, int end) {
        this.line = line;
        this.column = column;
        this.start = start;
        this.end = end;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }
}
