package net.kodehawa.mantarobot.commands.custom.v3;

public class SyntaxException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public SyntaxException(String message) {
        super(message);
    }
}
