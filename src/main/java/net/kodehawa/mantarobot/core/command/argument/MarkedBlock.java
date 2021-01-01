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

/**
 * Allows resetting to a previous position, with support for nesting
 */
public class MarkedBlock {
    private final Arguments arguments;
    private int offset;

    public MarkedBlock(Arguments arguments) {
        this.arguments = arguments;
        mark();
    }

    /**
     * Updates the reset offset. After calling this method, any resets will return to the
     * current offset.
     */
    public void mark() {
        this.offset = arguments.getOffset();
    }

    /**
     * Resets to the currently marked offset.
     */
    public void reset() {
        arguments.setOffset(offset);
    }
}
