package net.kodehawa.mantarolang;

import net.kodehawa.mantarolang.objects.LangContainer;
import net.kodehawa.mantarolang.objects.LangObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantaroLang implements LangContainer {
	public final Map<String, LangObject> ENV = new HashMap<>();

	public MantaroLang() {
	}

	@Override
	public LangObject get(String object) {
		return ENV.get(object);
	}

	public List<LangObject> eval(String code) {
		if (code == null) return null;

		Runtime runtime = new Runtime(this);

		MantaroLangCompiler.compile(code).accept(runtime);

		return runtime.done();
	}
}
