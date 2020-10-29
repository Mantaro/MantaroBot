/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.custom.v3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TokenIterator implements Iterator<Token> {
    private final String source;
    private final List<Token> tokens;
    private int index;

    public TokenIterator(String input) {
        this.source = input;
        this.tokens = new Lexer(input).tokenize();
    }

    @Override
    public boolean hasNext() {
        return index < tokens.size();
    }

    @Override
    public Token next() {
        return tokens.get(index++);
    }

    public String source() {
        return source;
    }

    public Token peek() {
        return tokens.get(index);
    }

    public void back() {
        index--;
    }

    public boolean match(TokenType tokenType) {
        return hasNext() && peek().type() == tokenType;
    }

    public void expect(TokenType tokenType) {
        Token t = hasNext() ? next() : null;
        if (t == null || t.type() != tokenType) {
            throw new IllegalStateException("Expected token of type " + tokenType + ", got " + t);
        }
    }

    private static class Lexer {
        private final StringBuilder current = new StringBuilder();
        private final List<Token> out = new ArrayList<>();
        private final String source;
        private int i = 0;

        private Lexer(String source) {
            this.source = source;
        }

        private void pushCurrentLiteral() {
            if (current.length() > 0) {
                out.add(new Token(position(i - current.length(), i - 1), TokenType.LITERAL, current.toString()));
                current.setLength(0);
            }
        }

        private void push(TokenType type) {
            pushCurrentLiteral();
            out.add(new Token(position(i, i), type, type.literalValue()));
        }

        private Position position(int from, int to) {
            int line = 1;
            int columnStart = 0;
            return new Position(line, from - columnStart + 1, from, to);
        }

        public List<Token> tokenize() {
            for (; i < source.length(); i++) {
                switch (source.charAt(i)) {
                    case '$' -> {
                        if (i < source.length() - 1 && source.charAt(i + 1) == '(') {
                            pushCurrentLiteral();
                            i++;
                            out.add(new Token(position(i - 1, i), TokenType.START_VAR, "$("));
                        } else {
                            current.append('$');
                        }
                    }
                    case '@' -> {
                        if (i < source.length() - 1 && source.charAt(i + 1) == '{') {
                            pushCurrentLiteral();
                            i++;
                            out.add(new Token(position(i - 1, i), TokenType.START_OP, "@{"));
                        } else {
                            current.append('@');
                        }
                    }
                    case ')' -> push(TokenType.RIGHT_PAREN);
                    case '}' -> push(TokenType.RIGHT_BRACE);
                    case ';' -> push(TokenType.SEMICOLON);
                    default -> current.append(source.charAt(i));
                }
            }
            pushCurrentLiteral();
            return out;
        }
    }

}
