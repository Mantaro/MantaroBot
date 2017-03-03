package net.kodehawa.mantarolang.objects;

public interface LangObject {
	default boolean isCallable() {
		return this instanceof LangCallable;
	}

	default boolean isNumber() {
		return this instanceof LangFloat;
	}

	default boolean isString() {
		return this instanceof LangString;
	}

	default boolean isContainer() {
		return this instanceof LangContainer;
	}
}
