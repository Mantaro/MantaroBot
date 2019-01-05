package net.kodehawa.mantarobot.commands.custom.v3;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Parser {
    private static final Map<TokenType, Parselet> PARSELETS = new HashMap<TokenType, Parselet>() {{
        put(TokenType.LITERAL, (__, c, t) -> c.put(new JSONObject().put("type", "literal").put("value", t.value())));
        put(TokenType.START_VAR, (it, c, t) -> {
            int d = 1;
            JSONArray name = new JSONArray();
            while(d > 0 && it.hasNext()) {
                t = it.next();
                switch(t.type()) {
                    case LITERAL: {
                        String v = t.value();
                        for(int i = 0; i < v.length(); i++) {
                            if(v.charAt(i) == '(') {
                                d++;
                            }
                        }
                        get(TokenType.LITERAL).apply(it, name, t);
                        break;
                    }
                    case RIGHT_PAREN: {
                        d--;
                        if(d > 0) {
                            get(TokenType.LITERAL).apply(it, name, t);
                        }
                        break;
                    }
                    default: get(t.type()).apply(it, name, t);
                }
            }
            c.put(new JSONObject().put("type", "variable").put("name", name));
        });
        put(TokenType.START_OP, (it, c, t) -> {
            int d = 1;
            boolean hasName = false;
            JSONArray name = new JSONArray();
            JSONArray data = new JSONArray();
            JSONArray current = new JSONArray();
            while(d > 0 && it.hasNext()) {
                t = it.next();
                switch(t.type()) {
                    case LITERAL: {
                        String v = t.value();
                        for(int i = 0; i < v.length(); i++) {
                            if(v.charAt(i) == '{') {
                                d++;
                            }
                        }
                        get(TokenType.LITERAL).apply(it, current, t);
                        break;
                    }
                    case RIGHT_BRACE: {
                        d--;
                        if(d > 0) {
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
}
