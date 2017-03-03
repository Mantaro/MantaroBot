package net.kodehawa.mantarolang;

import net.kodehawa.mantarolang.objects.LangObject;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class RuntimeOperator {
	private Consumer<Runtime> operation = r -> {
	};

	public Consumer<Runtime> getOperation() {
		return operation;
	}

	public void modify(Consumer<Runtime> operator) {
		Objects.requireNonNull(operator);
		Consumer<Runtime> wrapped = operation;
		operation = r -> {
			wrapped.accept(r);
			operator.accept(r);
		};
	}

	public void replace(UnaryOperator<LangObject> object) {
		modify(runtime -> runtime.replace(object.apply(runtime.current())));
	}

	public void replace(BiFunction<Runtime, LangObject, LangObject> object) {
		modify(runtime -> runtime.replace(object.apply(runtime, runtime.current())));
	}

	public void replaceWithList(Function<LangObject, List<LangObject>> object) {
		modify(runtime -> {
			List<LangObject> returns = object.apply(runtime.current());
			runtime.replace(returns.size() == 0 ? null : returns.remove(0));
			returns.forEach(runtime::next);
		});
	}

	public void replaceWithList(BiFunction<Runtime, LangObject, List<LangObject>> object) {
		modify(runtime -> {
			List<LangObject> returns = object.apply(runtime, runtime.current());
			runtime.replace(returns.size() == 0 ? null : returns.remove(0));
			returns.forEach(runtime::next);
		});
	}
}
