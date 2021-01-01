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
