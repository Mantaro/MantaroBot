package net.kodehawa.mantarolang;

import java.util.Collections;
import java.util.List;

public interface LangCallable extends LangObject {
	List<LangObject> call(List<LangObject> args);

	default List<LangObject> call() {
		return call(Collections.emptyList());
	}

	default List<LangObject> call(LangObject arg) {
		return call(Collections.singletonList(arg));
	}
}
