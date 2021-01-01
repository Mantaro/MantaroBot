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
