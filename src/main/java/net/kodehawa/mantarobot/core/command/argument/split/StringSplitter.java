package net.kodehawa.mantarobot.core.command.argument.split;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Splits strings, returning an object that preserves the delimiters (defaults to whitespace).
 */
public class StringSplitter {
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("\\s+");

    private final Pattern pattern;

    /**
     * Creates a new string splitter with the given pattern.
     *
     * @param pattern Pattern used to split strings.
     */
    public StringSplitter(@Nonnull Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "Pattern may not be null");
    }

    /**
     * Creates a new string splitter with the given pattern and flags.
     *
     * @param regex Pattern used to split strings.
     * @param flags Regex flags to compile with.
     */
    public StringSplitter(@Nonnull String regex, int flags) {
        this(Pattern.compile(regex, flags));
    }

    /**
     * Creates a new string splitter with the given pattern.
     *
     * @param regex Pattern used to split strings.
     */
    public StringSplitter(@Nonnull String regex) {
        this(Pattern.compile(regex));
    }

    /**
     * Creates a new string splitter with the default pattern of {@code \\s+}.
     */
    public StringSplitter() {
        this(DEFAULT_PATTERN);
    }

    /**
     * Splits a string, returning an array of {@link SplitString split strings}, which allow
     * getting the previous delimiter on each string.
     *
     * @param string String to split.
     *
     * @return The result of the split operation.
     */
    @Nonnull
    @CheckReturnValue
    public SplitString[] split(@Nonnull String string) {
        String[] values = pattern.split(string);
        int remove = values.length > 0 && values[0].isEmpty() ? 1 : 0; //1 if first string is empty
        int effectiveLength = values.length - remove;
        SplitString[] strings = new SplitString[effectiveLength];
        String lookup = string;
        for(int i = remove; i < values.length; i++) {
            String value = values[i];
            int startIdx = lookup.indexOf(value);
            strings[i - remove] = new SplitString(value, lookup.substring(0, startIdx));
            lookup = lookup.substring(startIdx + value.length());
        }
        return strings;
    }

    /**
     * Splits a string, throwing away all delimiters.
     *
     * @param string String to split.
     *
     * @return The result of the split operation.
     *
     * @see Pattern#split(CharSequence)
     */
    @Nonnull
    @CheckReturnValue
    public String[] rawSplit(@Nonnull String string) {
        return pattern.split(string);
    }

    /**
     * Splits a string, throwing away all delimiters.
     *
     * @param string String to split.
     * @param limit Maximum amount of resulting strings.
     *
     * @return The result of the split operation.
     *
     * @see Pattern#split(CharSequence, int)
     */
    @Nonnull
    @CheckReturnValue
    public String[] rawSplit(@Nonnull String string, @Nonnegative int limit) {
        return pattern.split(string, limit);
    }
}
