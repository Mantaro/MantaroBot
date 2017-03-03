package net.kodehawa.lib.mantarolang;

public class LangRuntimeException extends RuntimeException {
	public LangRuntimeException() {
	}

	public LangRuntimeException(String message) {
		super(message);
	}

	public LangRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public LangRuntimeException(Throwable cause) {
		super(cause);
	}

	public LangRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
