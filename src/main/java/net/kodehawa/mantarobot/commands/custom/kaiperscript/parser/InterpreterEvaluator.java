package net.kodehawa.mantarobot.commands.custom.kaiperscript.parser;

import net.kodehawa.mantarobot.commands.custom.kaiperscript.parser.internal.LimitReachedException;
import xyz.avarel.kaiper.ast.Expr;
import xyz.avarel.kaiper.exceptions.ComputeException;
import xyz.avarel.kaiper.exceptions.InterpreterException;
import xyz.avarel.kaiper.exceptions.ReturnException;
import xyz.avarel.kaiper.exceptions.SyntaxException;
import xyz.avarel.kaiper.interpreter.ExprInterpreter;
import xyz.avarel.kaiper.lexer.KaiperLexer;
import xyz.avarel.kaiper.parser.KaiperParser;
import xyz.avarel.kaiper.runtime.Obj;
import xyz.avarel.kaiper.scope.Scope;

import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

public class InterpreterEvaluator implements Evaluator {
    private final ExprInterpreter interpreter;
    private final Scope scope;

    public InterpreterEvaluator() {
        this(new ExprInterpreter(), new Scope());
    }

    public InterpreterEvaluator(ExprInterpreter interpreter, Scope scope) {
        this.scope = scope;
        this.interpreter = interpreter;
    }

    public InterpreterEvaluator declare(String variable, Obj obj) {
        scope.declare(variable, obj);
        return this;
    }

    public InterpreterEvaluator assign(String variable, Obj obj) {
        scope.assign(variable, obj);
        return this;
    }

    @Override
    public Object run(String string, PrintWriter out) throws ExecutionException {
        Obj result;

        try {
            KaiperLexer lexer = new KaiperLexer(string);
            KaiperParser parser = new KaiperParser(lexer);
            Expr expr = parser.parse();
            result = expr.accept(interpreter, scope);
        } catch (SyntaxException | InterpreterException e) {
            throw new ExecutionException(e);
        } catch (ComputeException e) {
            if (e.getMessage().endsWith("limit")) {
                throw new LimitReachedException(e.getMessage()); //assume it's a to-be-released VisitorException
            }

            throw new ExecutionException(e);
        } catch (ReturnException re) {
            result = re.getValue();
        }

        return result.toJava();
    }
}
