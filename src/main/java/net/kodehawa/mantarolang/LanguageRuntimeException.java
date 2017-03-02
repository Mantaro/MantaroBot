package net.kodehawa.mantarolang;

public class LanguageRuntimeException extends RuntimeException {
	public LanguageRuntimeException() {
	}

	public LanguageRuntimeException(String message) {
		super(message);
	}

	public LanguageRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public LanguageRuntimeException(Throwable cause) {
		super(cause);
	}

	public LanguageRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
