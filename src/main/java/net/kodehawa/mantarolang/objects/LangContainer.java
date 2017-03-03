package net.kodehawa.mantarolang.objects;

import net.kodehawa.mantarolang.LangRuntimeException;

public interface LangContainer extends LangObject {
	LangObject get(String name);

	default LangObject invalidProperty(String name) {
		throw new LangRuntimeException("'"+name+"' isn't a valid property.");
	}
}
