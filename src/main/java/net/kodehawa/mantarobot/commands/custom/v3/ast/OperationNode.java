package net.kodehawa.mantarobot.commands.custom.v3.ast;

import java.util.List;

public class OperationNode implements Node {
    private final Node name;
    private final List<Node> args;

    public OperationNode(Node name, List<Node> args) {
        this.name = name;
        this.args = args;
    }

    public Node name() {
        return name;
    }

    public List<Node> args() {
        return args;
    }

    @Override
    public <T, C> T accept(NodeVisitor<T, C> visitor, C context) {
        return visitor.visitOperation(this, context);
    }
}
