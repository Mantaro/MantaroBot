package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.NewContext;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Responsible for parsing arguments from strings into more useful objects.
 *
 * <br>All implementations of this interface should be immutable and thread safe.
 *
 * @param <T> Type of object this parser creates.
 */
@FunctionalInterface
@ThreadSafe
public interface Parser<T> {
    /**
     * Attempts parsing an object from the given input. More than one argument may be used.
     *
     * <br>This method should <b>never</b> throw. Return {@link Optional#empty() an empty optional} instead.
     *
     * @param context Context for the current command call.
     * @param arguments Arguments to be used for parsing.
     *
     * @return The result of the parsing attempt.
     *
     * @implNote Implementations should avoid calling
     * <ul>
     *     <li>{@link Arguments#setOffset(int) setOffset}</li>
     *     <li>{@link Arguments#back() back}/{@link Arguments#previous() previous} for more arguments than they used</li>
     *     <li>{@link Arguments#absoluteRange(int, int) absoluteRange}</li>
     * </ul>
     * Alternatives such as {@link Arguments#marked() marked()} or {@link Arguments#range(int, int) range(int, int)} should be used instead.
     */
    @Nonnull
    @CheckReturnValue
    Optional<T> parse(@Nonnull NewContext context, @Nonnull Arguments arguments);

    /**
     * Helper method for {@link #parse(NewContext, Arguments)}
     *
     * @param context Context for the current command call.
     *
     * @return The result of the parsing attempt.
     *
     * @see #parse(NewContext, Arguments)
     */
    @Nonnull
    @CheckReturnValue
    default Optional<T> parse(@Nonnull NewContext context) {
        return parse(context, context.arguments());
    }

    /**
     * Filters the result of this parser, returning Optional.empty() when the given predicate returns false.
     *
     * @param predicate Filter to apply to the parsing result.
     *
     * @return A new parser, which applies the given filter.
     */
    @Nonnull
    @CheckReturnValue
    default Parser<T> filter(@Nonnull Predicate<? super T> predicate) {
        return (c, args) -> parse(c, args).filter(predicate);
    }

    /**
     * Maps the result of this parser.
     *
     * @param mapper Mapper to apply to the parsing result.
     *
     * @return A new parser, which applies the given mapper.
     *
     * @param <U> Type returned by the returned parser.
     */
    @Nonnull
    @CheckReturnValue
    default <U> Parser<U> map(@Nonnull Function<? super T, ? extends U> mapper) {
        return (c, args) -> parse(c, args).map(mapper);
    }

    /**
     * Maps the result of this parser.
     *
     * @param mapper Mapper to apply to the parsing result.
     *
     * @return A new parser, which applies the given mapper.
     *
     * @param <U> Type returned by the returned parser.
     */
    @Nonnull
    @CheckReturnValue
    default <U> Parser<U> flatMap(@Nonnull Function<? super T, Optional<U>> mapper) {
        return (c, args) -> parse(c, args).flatMap(mapper);
    }

    /**
     * Filters out the given values, returning Optional.empty() whenever they're yielded when parsing.
     *
     * @param values Values that get filtered out from parsing results.
     *
     * @return A new parser, which is guaranteed not to return any provided value.
     */
    @Nonnull
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    default Parser<T> noneOf(@Nonnull T... values) {
        return filter(v -> !Helper.contains(values, v));
    }

    /**
     * Ensures the returned value equals one of the provided ones, returning Optional.empty() for any other value.
     *
     * @param values Values that are allowed to be returned after parsing.
     *
     * @return A new parser, which is guaranteed to return an object equal to one of the provided values.
     */
    @Nonnull
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    default Parser<T> oneOf(@Nonnull T... values) {
        return filter(v -> Helper.contains(values, v));
    }

    /**
     * Returns a parser that attempts applying the given parsers, in order, to the input.
     * The first non {@link Optional#empty() empty} result is returned.
     * <br>If no parsers match, or none are provided, {@link Optional#empty() Optional.empty()}
     * is returned.
     *
     * @param parsers Parsers to use.
     * @param <T> Common supertype for all provided parsers.
     *
     * @return A parser that attempts to apply the provided parsers.
     */
    @Nonnull
    @CheckReturnValue
    @SafeVarargs
    static <T> Parser<T> firstOf(@Nonnull Parser<? extends T>... parsers) {
        return (c, args) -> {
            var block = args.marked();
            for (var parser : parsers) {
                var optional = parser.parse(c, args);
                if (optional.isEmpty()) {
                    block.reset();
                    continue;
                }
                return optional.map(Function.identity());
            }
            return Optional.empty();
        };
    }
}
