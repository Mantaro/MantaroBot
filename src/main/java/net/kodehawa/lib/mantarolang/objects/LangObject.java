package net.kodehawa.lib.mantarolang.objects;

import net.kodehawa.lib.mantarolang.LangRuntimeException;

import java.util.List;

public interface LangObject {
	static <T extends LangObject> T cast(LangObject object, Class<T> c) {
		if (!c.isInstance(object)) throw new LangRuntimeException("Can't cast " + object + " to " + c);
		return c.cast(object);
	}

	static LangObject get(List<LangObject> list, int index) {
		return index >= list.size() ? null : list.get(index);
	}

	static LangObject get(List<LangObject> list, int index, LangObject orDefault) {
		return index >= list.size() ? orDefault : list.get(index);
	}

	default <T extends LangObject> T _cast(LangObject object, Class<T> c) {
		return cast(object, c);
	}

	default LangObject _get(List<LangObject> list, int index) {
		return get(list, index);
	}

	default LangObject _get(List<LangObject> list, int index, LangObject orDefault) {
		return get(list, index, orDefault);
	}

	default String asString() {
		return toString();
	}

	default boolean asTruth() {
		return true;
	}
}
