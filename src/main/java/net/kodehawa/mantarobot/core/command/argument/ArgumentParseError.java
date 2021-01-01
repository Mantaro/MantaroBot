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

/**
 * Thrown when an argument cannot be parsed on methods that must return a valid parsed argument.
 */
@SuppressWarnings("rawtypes")
public class ArgumentParseError extends RuntimeException {
    private final NewContext context;
    private final Parser<?> parser;
    private final Arguments readArguments;
    static final long serialVersionUID = 1L;

    public ArgumentParseError(String message, NewContext context, Parser<?> parser, Arguments readArguments) {
        super(messageString(message, parser, readArguments));
        this.context = context;
        this.parser = parser;
        this.readArguments = readArguments;
    }

    /**
     * Context for the current command call.
     *
     * @return Context for the command.
     */
    public NewContext context() {
        return context;
    }

    /**
     * Parser that failed to yield a valid value.
     *
     * @return The failing parser.
     */
    public Parser parser() {
        return parser;
    }

    /**
     * Arguments that were used by the parser.
     *
     * @return Arguments used by the parser.
     */
    public Arguments readArguments() {
        return readArguments;
    }

    private static String messageString(String message, Parser parser, Arguments readArguments) {
        if (message != null) return message;
        return "Unable to parse argument using parser " + parser + " and arguments " + readArguments;
    }
}
