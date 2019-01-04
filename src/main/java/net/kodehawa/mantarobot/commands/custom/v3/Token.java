package net.kodehawa.mantarobot.commands.custom.v3;

public class Token {
    private final int start;
    private final int end;
    private final TokenType type;
    private final String value;

    public Token(int start, int end, TokenType type, String value) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.value = value;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public TokenType type() {
        return type;
    }

    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode() ^ start ^ end;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Token)) {
            return false;
        }

        Token token = (Token)obj;
        return token.start == start && token.end == end
            && token.type == type && token.value.equals(value);
    }

    @Override
    public String toString() {
        return "Token(" + start + "-" + end + ", " + type + ", '" + value + "')";
    }
}
