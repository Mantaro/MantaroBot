package net.kodehawa.mantarolang;

public interface LangString extends LangWrapped<String> {
	static LangString of(String string) {
		return () -> string;
	}
}
