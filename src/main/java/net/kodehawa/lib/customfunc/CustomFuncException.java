package net.kodehawa.lib.customfunc;

public class CustomFuncException extends RuntimeException {
    public CustomFuncException() {
    }

    public CustomFuncException(String message) {
        super(message);
    }

    public CustomFuncException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomFuncException(Throwable cause) {
        super(cause);
    }

    public CustomFuncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
