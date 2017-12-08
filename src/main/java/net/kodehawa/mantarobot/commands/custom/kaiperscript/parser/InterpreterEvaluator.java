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
import xyz.avarel.kaiper.runtime.Undefined;
import xyz.avarel.kaiper.runtime.functions.NativeFunc;
import xyz.avarel.kaiper.runtime.functions.Parameter;
import xyz.avarel.kaiper.scope.DefaultScope;
import xyz.avarel.kaiper.scope.Scope;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class InterpreterEvaluator implements Evaluator {
    private final ExprInterpreter interpreter;
    private final Scope scope;

    public InterpreterEvaluator() {
        this(new ExprInterpreter(), DefaultScope.INSTANCE.copy());
    }

    public InterpreterEvaluator(ExprInterpreter interpreter, Scope scope) {
        this.scope = scope;
        this.interpreter = interpreter;
    }

    @Override
    public Object run(String string, PrintWriter out) throws ExecutionException {
        Obj result;

        scope.getMap().put("print", new NativeFunc("print", Parameter.of("args", true)) {
                @Override
                protected Obj eval(List<Obj> args) {
                    out.print(args.stream().map(Object::toString).collect(Collectors.joining()));
                    return Undefined.VALUE;
                }
            }
        );

        scope.getMap().put("println", new NativeFunc("println", Parameter.of("args", true)) {
                @Override
                protected Obj eval(List<Obj> args) {
                    out.println(args.stream().map(Object::toString).collect(Collectors.joining()));
                    return Undefined.VALUE;
                }
            }
        );


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

    public InterpreterEvaluator assign(String variable, Obj obj) {
        scope.assign(variable, obj);
        return this;
    }

    public InterpreterEvaluator declare(String variable, Obj obj) {
        scope.declare(variable, obj);
        return this;
    }
}
