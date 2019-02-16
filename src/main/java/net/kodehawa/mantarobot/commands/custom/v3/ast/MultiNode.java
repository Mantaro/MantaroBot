package net.kodehawa.mantarobot.commands.custom.v3.ast;

import java.util.List;
import java.util.stream.Collectors;

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
        switch(children.size()) {
            case 0: return new LiteralNode("");
            case 1: return children.get(0).simplify();
            default: return new MultiNode(
                    children.stream()
                            .map(Node::simplify)
                            .collect(Collectors.toList())
            );
        }
    }
}
