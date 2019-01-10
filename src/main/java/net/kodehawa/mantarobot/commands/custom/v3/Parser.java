package net.kodehawa.mantarobot.commands.custom.v3;

import net.kodehawa.mantarobot.commands.custom.v3.ast.*;

import java.util.*;

public class Parser {
    private static final Map<TokenType, Parselet> PARSELETS = new HashMap<TokenType, Parselet>() {{
        put(TokenType.LITERAL, (__, c, t) -> c.add(new LiteralNode(t.value())));
        put(TokenType.START_VAR, (it, c, t) -> {
            Stack<Position> stack = new Stack<>();
            stack.push(t.position());
            List<Node> name = new ArrayList<>();
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
            c.add(new VariableNode(new MultiNode(name)));
        });
        put(TokenType.START_OP, (it, c, t) -> {
            Stack<Position> stack = new Stack<>();
            stack.push(t.position());
            boolean hasName = false;
            List<Node> name = new ArrayList<>();
            List<Node> args = new ArrayList<>();
            List<Node> current = new ArrayList<>();
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
                            args.add(new MultiNode(current));
                        }
                        current = new ArrayList<>();
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
                args.add(new MultiNode(current));
            }
            c.add(new OperationNode(new MultiNode(name), args));
        });
        put(TokenType.RIGHT_PAREN, (__1, c, __2) -> c.add(new LiteralNode(")")));
        put(TokenType.RIGHT_BRACE, (__1, c, __2) -> c.add(new LiteralNode("}")));
        put(TokenType.SEMICOLON, (__1, c, __2) -> c.add(new LiteralNode(";")));

    }};
    private final TokenIterator iterator;

    public Parser(TokenIterator iterator) {
        this.iterator = iterator;
    }

    public Parser(String input) {
        this(new TokenIterator(input));
    }

    public Node parse() {
        List<Node> code = new ArrayList<>();
        while(iterator.hasNext()) {
            Token token = iterator.next();
            PARSELETS.get(token.type()).apply(iterator, code, token);
        }
        return new MultiNode(code);
    }

    private interface Parselet {
        void apply(TokenIterator iterator, List<Node> code, Token token);
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
