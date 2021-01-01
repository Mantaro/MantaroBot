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

package net.kodehawa.mantarobot.core.command.argument.split;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Represents a part of the result of splitting a string.
 * <br>Stores both the value and the previous whitespace, useful to rebuild the original string.
 */
public final class SplitString {
    private final String value;
    private final String previousWhitespace;

    SplitString(String value, String previousWhitespace) {
        this.value = value;
        this.previousWhitespace = previousWhitespace;
    }

    /**
     * The value of this string, after splitting the original on whitespaces.
     * <br>Does not contain whitespace.
     *
     * @return The value of this string.
     */
    @Nonnull
    @CheckReturnValue
    public String getValue() {
        return value;
    }

    /**
     * The whitespace preceding this string.
     *
     * @return The whitespace preceding this string.
     */
    @Nonnull
    @CheckReturnValue
    public String getPreviousWhitespace() {
        return previousWhitespace;
    }

    /**
     * The raw value of this string, obtained by concatenating the previous whitespace with the value.
     *
     * @return The raw value of this string.
     */
    @Nonnull
    @CheckReturnValue
    public String getRawValue() {
        return previousWhitespace + value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SplitString && ((SplitString) obj).value.equals(value);
    }

    @Override
    public String toString() {
        return getValue();
    }
}
