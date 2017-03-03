package net.kodehawa.lib.mantarolang;

import net.kodehawa.lib.mantarolang.internal.Runtime;
import net.kodehawa.lib.mantarolang.objects.LangCallable;
import net.kodehawa.lib.mantarolang.objects.LangContainer;
import net.kodehawa.lib.mantarolang.objects.LangObject;
import net.kodehawa.lib.mantarolang.objects.LangString;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kodehawa.lib.mantarolang.MantaroLangCompiler.cast;

public class MantaroLang implements LangContainer {
	public final Map<String, LangObject> ENV = new HashMap<>();

	public MantaroLang() {
		ENV.put("set", LangCallable.of(args -> {
			ENV.put(cast(args.get(0), LangString.class).get(),args.get(1));
			return Collections.singletonList(MantaroLang.this);
		}));
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
