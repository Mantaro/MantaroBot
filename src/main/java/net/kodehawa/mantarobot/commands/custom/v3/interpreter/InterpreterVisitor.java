package net.kodehawa.mantarobot.commands.custom.v3.interpreter;

import net.kodehawa.mantarobot.commands.custom.v3.ast.*;

import java.util.stream.Collectors;

public class InterpreterVisitor implements NodeVisitor<String, InterpreterContext> {
    @Override
    public String visitLiteral(LiteralNode node, InterpreterContext context) {
        return node.value();
    }

    @Override
    public String visitVariable(VariableNode node, InterpreterContext context) {
        return context.vars().getOrDefault(node.name().accept(this, context), "");
    }

    @Override
    public String visitOperation(OperationNode node, InterpreterContext context) {
        String type = node.name().accept(this, context);
        Operation op = context.operations().get(type);
        if(op == null) {
            return "";
        }
        return op.apply(context, node.args().stream()
                .map(n -> (Operation.Argument) () -> n.accept(this, context))
                .collect(Collectors.toList())
        );
    }

    @Override
    public String visitMulti(MultiNode node, InterpreterContext context) {
        StringBuilder sb = new StringBuilder();
        for(Node n : node.children()) {
            sb.append(n.accept(this, context));
        }
        return sb.toString();
    }
}
