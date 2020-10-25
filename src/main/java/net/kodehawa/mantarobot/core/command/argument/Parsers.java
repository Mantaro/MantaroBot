package net.kodehawa.mantarobot.core.command.argument;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Provides default parser implementations.
 */
public class Parsers {
    private Parsers() {}

    /**
     * Returns a parser that matches everything.
     *
     * @return A parser that matches everything.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> string() {
        return new BasicParser<>(Function.identity());
    }

    /**
     * Returns a parser that strictly matches integers.
     * <br>This parser behaves exactly like {@link Integer#valueOf(String)}.
     *
     * @return A parser that strictly matches integers.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Integer> strictInt() {
        return new CatchingParser<>(Integer::valueOf);
    }

    /**
     * Returns a parser that strictly matches integers.
     * <br>This parser behaves exactly like {@link Integer#valueOf(String)}.
     *
     * @return A parser that strictly matches integers.
     *
     * @deprecated Use {@link #strictInt()} or {@link #lenientInt()} instead.
     */
    @Nonnull
    @CheckReturnValue
    @Deprecated
    public static Parser<Integer> parseInt() {
        return strictInt();
    }

    /**
     * Returns a parser that leniently matches integers.
     * <br>This parser will ignore any {@code .} and {@code ,}
     * characters, and will multiply numbers depending on their suffix:
     * <ul>
     *     <li>{@code k} will multiply the results by 1000</li>
     *     <li>{@code kk} or {@code m} will multiply the results by 1000000</li>
     * </ul>
     * <br>Example: {@code 1.000k} {@literal ->} {@code 1000000}
     *
     * @return A parser that leniently matches integers.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Integer> lenientInt() {
        return new IntegerTypeParser<>((s, m) -> Integer.parseInt(s) * m);
    }

    /**
     * Returns a parser that matches floats.
     *
     * @return A parser that matches floats.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Float> parseFloat() {
        return new CatchingParser<>(Float::valueOf);
    }

    /**
     * Returns a parser that strictly matches longs.
     * <br>This parser behaves exactly like {@link Long#valueOf(String)}.
     *
     * @return A parser that strictly matches longs.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Long> strictLong() {
        return new CatchingParser<>(Long::valueOf);
    }

    /**
     * Returns a parser that matches longs.
     * <br>This parser behaves exactly like {@link Long#valueOf(String)}.
     *
     * @return A parser that matches longs.
     *
     * @deprecated Use {@link #strictLong()} or {@link #lenientLong()} instead.
     */
    @Nonnull
    @CheckReturnValue
    @Deprecated
    public static Parser<Long> parseLong() {
        return strictLong();
    }

    /**
     * Returns a parser that leniently matches longs.
     * <br>This parser will ignore any {@code .} and {@code ,}
     * characters, and will multiply numbers depending on their suffix:
     * <ul>
     *     <li>{@code k} will multiply the results by 1000</li>
     *     <li>{@code kk} or {@code m} will multiply the results by 1000000</li>
     * </ul>
     * <br>Example: {@code 1.000k} {@literal ->} {@code 1000000}
     *
     * @return A parser that leniently matches longs.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Long> lenientLong() {
        return new IntegerTypeParser<>((s, m) -> Long.parseLong(s) * m);
    }

    /**
     * Returns a parser that matches doubles.
     *
     * @return A parser that matches doubles.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Double> parseDouble() {
        return new CatchingParser<>(Double::valueOf);
    }

    /**
     * Returns a parser that matches integer ranges, inclusive on both ends.
     *
     * @param from First end of the range. May be either the lower or upper bound.
     * @param to Second end of the range. May be either the lower or upper bound.
     *
     * @return A parser that matches integer ranges, inclusive on both ends.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Integer> range(int from, int to) {
        int smaller = Math.min(from, to);
        int larger = Math.max(from, to);
        return lenientInt().filter(n->n >= smaller && n <= larger);
    }

    /**
     * Returns a parser that matches integer ranges, inclusive on both ends.
     *
     * @param from First end of the range. May be either the lower or upper bound.
     * @param to Second end of the range. May be either the lower or upper bound.
     *
     * @return A parser that matches integer ranges, inclusive on both ends.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Integer> rangeStrict(int from, int to) {
        int smaller = Math.min(from, to);
        int larger = Math.max(from, to);
        return strictInt().filter(n->n >= smaller && n <= larger);
    }

    /**
     * Returns a parser that matches long ranges, inclusive on both ends.
     *
     * @param from First end of the range. May be either the lower or upper bound.
     * @param to Second end of the range. May be either the lower or upper bound.
     *
     * @return A parser that matches long ranges, inclusive on both ends.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Long> range(long from, long to) {
        long smaller = Math.min(from, to);
        long larger = Math.max(from, to);
        return lenientLong().filter(n->n >= smaller && n <= larger);
    }

    /**
     * Returns a parser that matches long ranges, inclusive on both ends.
     *
     * @param from First end of the range. May be either the lower or upper bound.
     * @param to Second end of the range. May be either the lower or upper bound.
     *
     * @return A parser that matches long ranges, inclusive on both ends.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<Long> rangeStrict(long from, long to) {
        long smaller = Math.min(from, to);
        long larger = Math.max(from, to);
        return strictLong().filter(n->n >= smaller && n <= larger);
    }

    /**
     * Returns a parser that matches based on regular expressions.
     *
     * @param regex Regular expression to use. May not be null.
     *
     * @return A parser that matches based on regular expressions.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> matching(@Nonnull String regex) {
        return matching(Pattern.compile(regex));
    }

    /**
     * Returns a parser that matches based on regular expressions.
     *
     * @param regex Regular expression to use. May not be null.
     * @param flags Flags for the expression.
     *
     * @return A parser that matches based on regular expressions.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> matching(@Nonnull String regex, int flags) {
        return matching(Pattern.compile(regex, flags));
    }

    /**
     * Returns a parser that matches based on regular expressions.
     *
     * @param pattern Pattern to use. May not be null.
     *
     * @return A parser that matches based on regular expressions.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> matching(@Nonnull Pattern pattern) {
        return string().filter(s->pattern.matcher(s).matches());
    }

    /**
     * Returns a parser that matches enum values.
     *
     * @param enumClass Class of the enum.  May not be null.
     * @param <T> Enum type.
     *
     * @return A parser that matches enum values.
     */
    @Nonnull
    @CheckReturnValue
    public static <T extends Enum<T>> Parser<T> toEnum(@Nonnull Class<T> enumClass) {
        return toEnum(enumClass, false);
    }

    /**
     * Returns a parser that matches enum values.
     *
     * @param enumClass Class of the enum.  May not be null.
     * @param ignoreCase Ignore case when matching the enum values.
     * @param <T> Enum type.
     *
     * @return A parser that matches enum values.
     */
    @Nonnull
    @CheckReturnValue
    public static <T extends Enum<T>> Parser<T> toEnum(@Nonnull Class<T> enumClass, boolean ignoreCase) {
        T[] constants = enumClass.getEnumConstants();
        return (__, arguments) -> {
            String name = arguments.next().getValue();
            for(T t : constants) {
                if (ignoreCase) {
                    if (t.name().equalsIgnoreCase(name)) return Optional.of(t);
                } else {
                    if (t.name().equals(name)) return Optional.of(t);
                }
            }
            return Optional.empty();
        };
    }

    /**
     * Returns a parser that yields all the remaining content as-is.
     * <br>This method differs from {@link #remainingArguments() remainingArguments()} and
     * {@link #remainingArguments(String) remainingArguments(String)} because it preserves
     * all whitespace in the actual user input.
     *
     * @return A parser that yields all the remaining content as-is.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> remainingContent() {
        return (__, arguments) -> {
            if (!arguments.hasNext()) return Optional.empty();
            StringJoiner sj = new StringJoiner("");
            while(arguments.hasNext()) {
                sj.add(arguments.next().getRawValue());
            }
            return Optional.of(sj.toString());
        };
    }

    /**
     * Returns a parser that yields all remaining arguments as a string.
     *
     * @return A parser that yields all remaining arguments as a string.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> remainingArguments() {
        return remainingArguments(" ");
    }

    /**
     * Returns a parser that yields all remaining arguments as a string.
     *
     * @param delimiter Delimiter used when joining the strings. May not be null.
     *
     * @return A parser that yields all remaining arguments as a string.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> remainingArguments(@Nonnull String delimiter) {
        return (c, arguments) -> {
            if (!arguments.hasNext()) return Optional.empty();
            StringJoiner sj = new StringJoiner(delimiter);
            while(arguments.hasNext()) {
                sj.add(arguments.next().getValue());
            }
            return Optional.of(sj.toString());
        };
    }

    /**
     * Returns a parser that matches an URL.
     *
     * @return A parser that matches an URL.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<URL> url() {
        return new CatchingParser<>(URL::new);
    }

    /**
     * Returns a parser that matches an URL with one of the given protocols.
     *
     * @param allowedProtocols Protocols that are allowed.
     *
     * @return A parser that matches an URL with one of the given protocols.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<URL> url(Collection<String> allowedProtocols) {
        return url().filter(u->allowedProtocols.contains(u.getProtocol()));
    }

    /**
     * Returns a parser that matches an URL with one of the given protocols.
     *
     * @param allowedProtocols Protocols that are allowed.
     *
     * @return A parser that matches an URL with one of the given protocols.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<URL> url(String... allowedProtocols) {
        return url(Arrays.asList(allowedProtocols));
    }

    /**
     * Returns a parser that matches an HTTP URL.
     *
     * @return A parser that matches an HTTP URL.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<URL> httpUrl() {
        return url(Arrays.asList("http", "https"));
    }

    /**
     * Returns a parser that matches a string delimited by a given character.
     *
     * <ul>
     *     <li>If the given character is found, arguments will be read until a matching delimiter is found.</li>
     *     <li>If escaping is enabled, adding a {@literal \} character will escape a delimiter, or all whitespace until the next argument.</li>
     *     <li>If no matching delimiter is found, all the remaining arguments will be read.</li>
     * </ul>
     *
     * @param delimiter Delimiter for the match.
     * @param allowEscaping Allow escaping delimiters and whitespace with a backslash {@literal \}
     *
     * @return A parser that matches a string delimited by a given character.
     */
    @Nonnull
    @CheckReturnValue
    public static Parser<String> delimitedBy(char delimiter, boolean allowEscaping) {
        return (__, arguments) -> {
            if (!arguments.hasNext()) return Optional.empty();
            DelimiterContext context = new DelimiterContext(delimiter, allowEscaping);
            while(arguments.hasNext()) {
                if (!context.handle(arguments.next())) break;
            }
            return Optional.of(context.result());
        };
    }

    /**
     * Returns a parser that always returns a successful result, containing
     * the optional returned by the provided parser.
     * <br>This method is intended for use when you don't want to unread arguments,
     * even if parsing fails.
     *
     * @param parser Parser used for values.
     * @param <T> Type of the object returned by the given parser.
     *
     * @return Parser that never fails, always returning a valid option.
     */
    @Nonnull
    @CheckReturnValue
    public static <T> Parser<Optional<T>> option(@Nonnull Parser<T> parser) {
        return (c, args) -> Optional.of(parser.parse(c, args));
    }
}
