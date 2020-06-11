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
