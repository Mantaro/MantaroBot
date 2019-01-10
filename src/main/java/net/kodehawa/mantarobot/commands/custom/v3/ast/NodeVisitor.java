package net.kodehawa.mantarobot.commands.custom.v3.ast;

public interface NodeVisitor<T, C> {
    T visitLiteral(LiteralNode node, C context);

    T visitVariable(VariableNode node, C context);

    T visitOperation(OperationNode node, C context);

    T visitMulti(MultiNode node, C context);
}
