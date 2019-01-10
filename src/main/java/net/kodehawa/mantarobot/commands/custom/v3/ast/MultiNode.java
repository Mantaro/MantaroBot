package net.kodehawa.mantarobot.commands.custom.v3.ast;

import java.util.List;

public class MultiNode implements Node {
    private final List<Node> children;

    public MultiNode(List<Node> children) {
        this.children = children;
    }

    public List<Node> children() {
        return children;
    }

    @Override
    public <T, C> T accept(NodeVisitor<T, C> visitor, C context) {
        return visitor.visitMulti(this, context);
    }

    @Override
    public Node simplify() {
        return children.size() == 1 ? children.get(0).simplify() : this;
    }
}
