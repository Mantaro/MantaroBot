package net.kodehawa.lib.mantarolang;

import net.kodehawa.lib.mantarolang.internal.Runtime;
import net.kodehawa.lib.mantarolang.objects.LangCallable;
import net.kodehawa.lib.mantarolang.objects.LangContainer;
import net.kodehawa.lib.mantarolang.objects.LangObject;
import net.kodehawa.lib.mantarolang.objects.LangString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MantaroLang implements LangContainer {
	public final Map<String, LangObject> ENV = new HashMap<>();

	public MantaroLang() {
		ENV.put("set", LangCallable.of(args -> {
			ENV.put(_cast(args.get(0), LangString.class).get(), args.get(1));
			return Collections.singletonList(MantaroLang.this);
		}));
	}

	@Override
	public LangObject get(String object) {
		return ENV.get(object);
	}

	public CompiledFunction<Pair<Long, List<LangObject>>> compile(String code) {
		if (code == null) return null;

		long millis = -System.currentTimeMillis();
		Consumer<Runtime> compiled = MantaroLangCompiler.compile(code);
		millis += System.currentTimeMillis();

		long finalMillis = millis;

		return new CompiledFunction<Pair<Long, List<LangObject>>>() {
			@Override
			public Pair<Long, List<LangObject>> run() {
				Runtime runtime = new Runtime(MantaroLang.this);
				long millis = -System.currentTimeMillis();
				compiled.accept(runtime);
				millis += System.currentTimeMillis();
				return Pair.of(millis, runtime.done());
			}

			@Override
			public long timeTook() {
				return finalMillis;
			}
		};
	}

	public List<LangObject> eval(String code) {
		if (code == null) return null;

		Runtime runtime = new Runtime(this);

		MantaroLangCompiler.compile(code).accept(runtime);

		return runtime.done();
	}
}
