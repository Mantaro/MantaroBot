package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.NewContext;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Similar to {@link BasicParser}, but catches any thrown exceptions and allows having custom handling
 * of them them, or defaulting to an empty result.
 *
 * @param <T> Resulting type of the transformation.
 */
public class CatchingParser<T> implements Parser<T> {
    private final ThrowingParser<T> parseFunction;
    private final Function<Exception, Optional<T>> errorHandler;

    public CatchingParser(@Nonnull ThrowingParser<T> parseFunction, @Nonnull Function<Exception, Optional<T>> errorHandler) {
        this.parseFunction = parseFunction;
        this.errorHandler = errorHandler;
    }

    public CatchingParser(@Nonnull ThrowingParser<T> parseFunction) {
        this(parseFunction, __->Optional.empty());
    }

    @Nonnull
    @Override
    public Optional<T> parse(@Nonnull NewContext context, @Nonnull Arguments arguments) {
        try {
            return Optional.of(parseFunction.parse(arguments.next().getValue()));
        } catch(Exception e) {
            return errorHandler.apply(e);
        }
    }

    @FunctionalInterface
    public interface ThrowingParser<T> {
        T parse(String value) throws Exception;
    }
}
