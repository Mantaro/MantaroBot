package net.kodehawa.mantarobot.commands.custom.v3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TokenIterator implements Iterator<Token> {
    private final List<Token> tokens;
    private int index;

    public TokenIterator(List<Token> tokens) {
        this.tokens = tokens;
    }

    public TokenIterator(String input) {
        this(tokenize(input));
    }

    @Override
    public boolean hasNext() {
        return index < tokens.size();
    }

    @Override
    public Token next() {
        return tokens.get(index++);
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
        if(t == null || t.type() != tokenType) {
            throw new IllegalStateException("Expected token of type " + tokenType + ", got " + t);
        }
    }

    private static List<Token> tokenize(String input) {
        List<Token> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        for(; i < input.length(); i++) {
            switch(input.charAt(i)) {
                case '$': {
                    if(i < input.length() - 1 && input.charAt(i + 1) == '(') {
                        if(current.length() > 0) {
                            out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
                            current.setLength(0);
                        }
                        i++;
                        out.add(new Token(i - 1, i, TokenType.START_VAR, "$("));
                    } else {
                        current.append('$');
                    }
                    break;
                }
                case ')': {
                    if(current.length() > 0) {
                        out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
                        current.setLength(0);
                    }
                    out.add(new Token(i, i, TokenType.RIGHT_PAREN, ")"));
                    break;
                }
                case '@': {
                    if(i < input.length() - 1 && input.charAt(i + 1) == '{') {
                        if(current.length() > 0) {
                            out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
                            current.setLength(0);
                        }
                        i++;
                        out.add(new Token(i - 1, i, TokenType.START_OP, "@{"));
                    } else {
                        current.append('@');
                    }
                    break;
                }
                case '}': {
                    if(current.length() > 0) {
                        out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
                        current.setLength(0);
                    }
                    out.add(new Token(i, i, TokenType.RIGHT_BRACE, "}"));
                    break;
                }
                case ';': {
                    if(current.length() > 0) {
                        out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
                        current.setLength(0);
                    }
                    out.add(new Token(i, i, TokenType.SEMICOLON, ";"));
                    break;
                }
                default: {
                    current.append(input.charAt(i));
                }
            }
        }
        if(current.length() > 0) {
            out.add(new Token(i - current.length(), i - 1, TokenType.LITERAL, current.toString()));
        }
        return out;
    }
}
