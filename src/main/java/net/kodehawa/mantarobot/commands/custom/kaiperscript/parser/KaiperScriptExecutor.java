package net.kodehawa.mantarobot.commands.custom.kaiperscript.parser;

import net.kodehawa.mantarobot.commands.custom.kaiperscript.lexer.KaiperScriptLexer;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.lexer.Token;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

public class KaiperScriptExecutor {
    private final List<Token> tokens;

    public KaiperScriptExecutor(String code) {
        this(new KaiperScriptLexer(code));
    }

    public KaiperScriptExecutor(KaiperScriptLexer lexer) {
        this(lexer.getTokens());
    }

    public KaiperScriptExecutor(List<Token> tokens) {
        this.tokens = tokens;
    }

    public KaiperScriptExecutor mapTextTokens(UnaryOperator<String> function) {
        tokens.replaceAll(token -> {
            switch (token.getType()) {
                case TEXT: {
                    return new Token(
                        token.getPosition(),
                        token.getType(),
                        function.apply(token.getString()));
                }
                default: {
                    return token;
                }
            }
        });

        return this;
    }

    public String execute(Evaluator evaluator) {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);

        for (Token token : tokens) {
            try {
                switch (token.getType()) {
                    case TEXT: {
                        buffer.append(token.getString());
                        break;
                    }

                    case CODE: {
                        evaluator.run(token.getString(), out);
                        break;
                    }

                    case CODE_EQUALS: {
                        out.print(evaluator.run(token.getString(), out));
                        break;
                    }
                    case CODE_OPT_EQUALS: {
                        Object result = evaluator.run(token.getString(), out);
                        if (result != null) out.print(result);
                        break;
                    }
                }
            } catch (ExecutionException e) {
                out.print("**``Err: " + e.getCause().getMessage() + "``**");
            }
        }

        return buffer.toString();
    }
}
