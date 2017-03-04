package net.kodehawa.lib.mantarolang.java;

import net.kodehawa.lib.mantarolang.objects.LangCallable;
import net.kodehawa.lib.mantarolang.objects.LangContainer;
import net.kodehawa.lib.mantarolang.objects.LangObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StaticSerializator<T> {
	private class LangContainerCallable implements LangCallable, LangContainer {
		private final T obj;

		public LangContainerCallable(T obj) {
			this.obj = obj;
		}

		@Override
		public List<LangObject> call(List<LangObject> args) {
			return call.apply(obj, args);
		}

		@Override
		public LangObject get(String name) {
			if (!map.containsKey(name)) invalidProperty(name);
			return map.get(name).apply(obj);
		}

		@Override
		public String toString() {
			return "LangContainer&LangCallable{of=" + obj + ";from=" + StaticSerializator.this.toString() + "}";
		}
	}

	private BiFunction<T, List<LangObject>, List<LangObject>> call;
	private boolean callable = false, container = false;
	private Map<String, Function<T, LangObject>> map = new HashMap<>();

	public StaticSerializator() {
	}

	public StaticSerializator<T> call(BiFunction<T, List<LangObject>, List<LangObject>> call) {
		callable = true;
		this.call = call;
		return this;
	}

	public StaticSerializator<T> map(String name, Function<T, LangObject> map) {
		container = true;
		this.map.put(name, map);
		return this;
	}

	public LangObject serialize(T obj) {
		if (callable && container) return new LangContainerCallable(obj);
		if (callable) return new LangCallable() {
			@Override
			public List<LangObject> call(List<LangObject> args) {
				return call.apply(obj, args);
			}

			@Override
			public String toString() {
				return "LangCallable{of=" + obj + ";from=" + StaticSerializator.this.toString() + "}";
			}
		};
		if (container) return new LangContainer() {
			@Override
			public LangObject get(String name) {
				if (!map.containsKey(name)) invalidProperty(name);
				return map.get(name).apply(obj);
			}

			@Override
			public String toString() {
				return "LangContainer{of=" + obj + ";from=" + StaticSerializator.this.toString() + "}";
			}
		};

		return new LangObject() {
		};
	}

	@Override
	public String toString() {
		return "StaticSerializator{" +
			"callable=" + callable + (callable ? " (" + call + ')' : "") +
			", container=" + container + (container ? " (" + map + ')' : "") +
			'}';
	}
}
