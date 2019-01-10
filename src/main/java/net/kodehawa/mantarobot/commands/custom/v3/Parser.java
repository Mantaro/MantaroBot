package net.kodehawa.mantarobot.commands.custom.v3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Parser {
    private static final Map<TokenType, Parselet> PARSELETS = new HashMap<TokenType, Parselet>() {{
        put(TokenType.LITERAL, (__, c, t) -> c.put(new JSONObject().put("type", "literal").put("value", t.value())));
        put(TokenType.START_VAR, (it, c, t) -> {
            Stack<Position> stack = new Stack<>();
            stack.push(t.position());
            JSONArray name = new JSONArray();
            while(stack.size() > 0 && it.hasNext()) {
                t = it.next();
                switch(t.type()) {
                    case LITERAL: {
                        count(t, stack, '(');
                        get(TokenType.LITERAL).apply(it, name, t);
                        break;
                    }
                    case RIGHT_PAREN: {
                        stack.pop();
                        if(stack.size() > 0) {
                            get(TokenType.LITERAL).apply(it, name, t);
                        }
                        break;
                    }
                    default: get(t.type()).apply(it, name, t);
                }
            }
            if(stack.size() > 0) {
                throw syntaxError(it, stack.pop(), '(');
            }
            c.put(new JSONObject().put("type", "variable").put("name", name));
        });
        put(TokenType.START_OP, (it, c, t) -> {
            Stack<Position> stack = new Stack<>();
            stack.push(t.position());
            boolean hasName = false;
            JSONArray name = new JSONArray();
            JSONArray data = new JSONArray();
            JSONArray current = new JSONArray();
            while(stack.size() > 0 && it.hasNext()) {
                t = it.next();
                switch(t.type()) {
                    case LITERAL: {
                        count(t, stack, '{');
                        get(TokenType.LITERAL).apply(it, current, t);
                        break;
                    }
                    case RIGHT_BRACE: {
                        stack.pop();
                        if(stack.size() > 0) {
                            get(TokenType.LITERAL).apply(it, current, t);
                        }
                        break;
                    }
                    case SEMICOLON: {
                        if(!hasName) {
                            name = current;
                        } else {
                            data.put(current);
                        }
                        current = new JSONArray();
                        hasName = true;
                        break;
                    }
                    default: get(t.type()).apply(it, current, t);
                }
            }
            if(stack.size() > 0) {
                throw syntaxError(it, stack.pop(), '{');
            }
            if(!hasName) {
                name = current;
            } else {
                data.put(current);
            }
            c.put(new JSONObject().put("type", "op").put("name", name).put("data", data));
        });
        put(TokenType.RIGHT_PAREN, (__1, c, __2) -> c.put(new JSONObject().put("type", "literal").put("value", ")")));
        put(TokenType.RIGHT_BRACE, (__1, c, __2) -> c.put(new JSONObject().put("type", "literal").put("value", "}")));
        put(TokenType.SEMICOLON, (__1, c, __2) -> c.put(new JSONObject().put("type", "literal").put("value", ";")));

    }};
    private final TokenIterator iterator;

    public Parser(TokenIterator iterator) {
        this.iterator = iterator;
    }

    public Parser(String input) {
        this(new TokenIterator(input));
    }

    public JSONArray parse() {
        JSONArray code = new JSONArray();
        while(iterator.hasNext()) {
            Token token = iterator.next();
            PARSELETS.get(token.type()).apply(iterator, code, token);
        }
        return code;
    }

    private interface Parselet {
        void apply(TokenIterator iterator, JSONArray code, Token token);
    }

    private static void count(Token token, Stack<Position> stack, char c) {
        String v = token.value();
        int line = token.position().line();
        int column = token.position().column();
        for(int i = 0; i < v.length(); i++) {
            if(v.charAt(i) == '\n') {
                line++;
                column = 0;
            }
            if(v.charAt(i) == c) {
                stack.push(new Position(line, column, -1, -1));
            }
            column++;
        }
    }

    private static RuntimeException syntaxError(TokenIterator iterator, Position p, char unclosed) {
        int column = (p.end() < 0 ? p.column() : p.end() + 1);
        String line = iterator.source().split("\n")[p.line() - 1];
        String str = line.substring(Math.max(column - 11, 0), Math.min(column + 10, line.length()));
        StringBuilder sb = new StringBuilder();
        sb.append("Unclosed ").append(unclosed).append(" at line ")
                .append(p.line()).append(", column ").append(column).append('\n');
        sb.append(str).append('\n');
        for(int i = 0; i < Math.min(10, column - 1); i++) {
            sb.append(' ');
        }
        sb.append('^');
        throw new SyntaxException(sb.toString());
    }
}
