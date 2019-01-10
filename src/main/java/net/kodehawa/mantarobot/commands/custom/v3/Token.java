package net.kodehawa.mantarobot.commands.custom.v3;

public class Token {
    private final Position position;
    private final TokenType type;
    private final String value;

    public Token(Position position, TokenType type, String value) {
        this.position = position;
        this.type = type;
        this.value = value;
    }

    public Position position() {
        return position;
    }

    public TokenType type() {
        return type;
    }

    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode() ^ position.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Token)) {
            return false;
        }

        Token token = (Token)obj;
        return token.position.equals(position)
            && token.type == type && token.value.equals(value);
    }

    @Override
    public String toString() {
        return "Token(" + position + ", " + type + ", '" + value + "')";
    }
}
