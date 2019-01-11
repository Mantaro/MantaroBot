package net.kodehawa.mantarobot.commands.custom.v3.ast;

public class VariableNode implements Node {
    private final Node name;

    public VariableNode(Node name) {
        this.name = name;
    }

    public Node name() {
        return name;
    }

    @Override
    public <T, C> T accept(NodeVisitor<T, C> visitor, C context) {
        return visitor.visitVariable(this, context);
    }

    @Override
    public Node simplify() {
        return this;
    }
}
