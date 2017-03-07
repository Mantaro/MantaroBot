package net.kodehawa.lib.mantarolang.objects;

public interface LangWrapped<T> extends LangObject {
	T get();

	@Override
	default String asString() {
		return get().toString();
	}
}
