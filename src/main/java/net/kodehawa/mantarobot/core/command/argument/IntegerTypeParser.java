package net.kodehawa.mantarobot.core.command.argument;

import net.kodehawa.mantarobot.core.command.NewContext;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class IntegerTypeParser<T> implements Parser<T> {
    private final BiFunction<String, Integer, T> parseFunction;
    private final Function<Exception, Optional<T>> errorHandler;

    public IntegerTypeParser(BiFunction<String, Integer, T> parseFunction, Function<Exception, Optional<T>> errorHandler) {
        this.parseFunction = parseFunction;
        this.errorHandler = errorHandler;
    }

    public IntegerTypeParser(BiFunction<String, Integer, T> parseFunction) {
        this(parseFunction, __ -> Optional.empty());
    }

    @Nonnull
    @Override
    public Optional<T> parse(@Nonnull NewContext context, @Nonnull Arguments arguments) {
        try {
            String s = arguments.next().getValue();
            StringBuilder builder = new StringBuilder();
            int offset = 0;
            for(; offset < s.length(); offset++) {
                char c = s.charAt(offset);
                if (c == '.' || c == ',') continue;
                if (!Character.isDigit(c)) break;
                builder.append(c);
            }
            if (builder.length() == 0) return Optional.empty();
            int multiplier;
            if (offset < s.length()) {
                switch(s.substring(offset).toLowerCase()) {
                    case "k": multiplier = 1000; break;
                    case "kk": case "m": multiplier = 1000000; break;
                    default: return Optional.empty();
                }
            } else {
                multiplier = 1;
            }
            return Optional.of(parseFunction.apply(builder.toString(), multiplier));
        } catch(Exception e) {
            return errorHandler.apply(e);
        }
    }
}
