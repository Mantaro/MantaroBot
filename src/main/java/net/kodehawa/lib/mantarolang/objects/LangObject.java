package net.kodehawa.lib.mantarolang.objects;

public interface LangObject {
	default boolean asTruth() {
		return true;
	}

	default String asString() {
		return toString();
	}

}
