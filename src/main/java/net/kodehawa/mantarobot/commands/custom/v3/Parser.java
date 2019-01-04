package net.kodehawa.mantarobot.commands.custom.v3;

import org.json.JSONArray;
import org.json.JSONObject;

public class Parser {
    private final TokenIterator iterator;

    public Parser(TokenIterator iterator) {
        this.iterator = iterator;
    }

    public Parser(String input) {
        this(new TokenIterator(input));
    }

    public JSONArray parse() {
        return parse(ParseUntil.END_OF_INPUT);
    }

    private JSONArray parse(ParseUntil until) {
        JSONArray code = new JSONArray();
        while(iterator.hasNext()) {
            Token token = iterator.next();
            switch(token.type()) {
                case LITERAL: {
                    code.put(new JSONObject().put("type", "literal").put("value", token.value()));
                    break;
                }
                case START_VAR: {
                    code.put(new JSONObject().put("type", "variable").put("name", parse(ParseUntil.END_OF_VAR)));
                    iterator.expect(TokenType.RIGHT_PAREN);
                    break;
                }
                case RIGHT_PAREN: {
                    if(until == ParseUntil.END_OF_VAR) {
                        iterator.back();
                        return code;
                    } else {
                        code.put(new JSONObject().put("type", "literal").put("value", ")"));
                    }
                    break;
                }
                case START_OP: {
                    JSONArray name = parse(ParseUntil.END_OF_OP);
                    if(iterator.match(TokenType.RIGHT_BRACE)) {
                        code.put(new JSONObject().put("type", "op").put("name", name).put("data", new JSONArray()));
                        break;
                    }
                    JSONArray data = new JSONArray();
                    while(iterator.match(TokenType.SEMICOLON)) {
                        iterator.next();
                        data.put(parse(ParseUntil.END_OF_OP));
                    }
                    iterator.expect(TokenType.RIGHT_BRACE);
                    code.put(new JSONObject().put("type", "op").put("name", name).put("data", data));
                    break;
                }
                case RIGHT_BRACE: {
                    if(until == ParseUntil.END_OF_OP) {
                        iterator.back();
                        return code;
                    } else {
                        code.put(new JSONObject().put("type", "literal").put("value", "}"));
                    }
                    break;
                }
                case SEMICOLON: {
                    if(until == ParseUntil.END_OF_OP) {
                        iterator.back();
                        return code;
                    } else {
                        code.put(new JSONObject().put("type", "literal").put("value", ";"));
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("Unknown token " + token);
                }
            }
        }
        return code;
    }

    private enum ParseUntil {
        END_OF_VAR, END_OF_OP, END_OF_INPUT
    }
}
