package net.kodehawa.lib.mantarolang.objects;

import net.kodehawa.lib.mantarolang.LangRuntimeException;

import java.util.Collections;
import java.util.List;

public interface LangCallable extends LangObject {
	static LangCallable of(LangCallable lambda) {
		return lambda;
	}
	@SuppressWarnings("unchecked")
	default <T extends LangObject> T cast(LangObject object, Class<T> c) {
		if (!c.isInstance(object)) throw new LangRuntimeException("Can't cast " + object + " to " + c);
		return ((T) object);
	}

	default LangObject get(List<LangObject> list, int index) {
		return index >= list.size() ? null : list.get(index);
	}

	default LangObject get(List<LangObject> list, int index, LangObject orDefault) {
		return index >= list.size() ? orDefault : list.get(index);
	}

	List<LangObject> call(List<LangObject> args);

	default List<LangObject> call() {
		return call(Collections.emptyList());
	}

	default List<LangObject> call(LangObject arg) {
		return call(Collections.singletonList(arg));
	}
}
