package net.kodehawa.mantarolang;

public class LangCompileException extends RuntimeException {
	public LangCompileException() {
	}

	public LangCompileException(String message) {
		super(message);
	}

	public LangCompileException(String message, Throwable cause) {
		super(message, cause);
	}

	public LangCompileException(Throwable cause) {
		super(cause);
	}

	public LangCompileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
