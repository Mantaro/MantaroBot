package net.kodehawa.mantarobot.commands.custom.v3.ast;

public class LiteralNode implements Node {
    private final String value;

    public LiteralNode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public <T, C> T accept(NodeVisitor<T, C> visitor, C context) {
        return visitor.visitLiteral(this, context);
    }
}
