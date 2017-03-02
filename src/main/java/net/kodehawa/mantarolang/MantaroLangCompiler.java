package net.kodehawa.mantarolang;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class MantaroLangCompiler {
	private static <T> UnaryOperator<T> asFunction(Consumer<T> consumer) {
		return t -> {
			consumer.accept(t);
			return t;
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(LangObject object, Class<T> c) {
		if (!c.isInstance(object)) throw new LanguageRuntimeException("Can't cast " + object + " to " + c);
		return ((T) object);
	}

	private static LangCallable closure(UnaryOperator<Runtime> compiled, LangObject thisObj) {
		return args -> compiled.apply(new Runtime(thisObj, args.size() == 0 ? thisObj : args.get(0))).done();
	}

	public static Consumer<Runtime> compile(String code) {
		Objects.requireNonNull(code, "code");

		RuntimeOperator runtime = new RuntimeOperator();

		char[] array = code.toCharArray();

		int line = 0;
		for (int i = 0; i < array.length; i++) {
			char c = array[i];

			if (c == '\r' || c == '\n') {
				line++;
				continue;
			}
			if (c == ' ' || c == '\t') continue;

			if (c == ',' || c == '.' || c == '\\' || c == ')' || c == '}' || c == '[' || c == ']') {
				throw new IllegalStateException("Invalid character '" + c + "' at line " + i);
			}
			if (c == '(') {
				int pCount = 0;
				StringBuilder functionBlock = new StringBuilder();
				i++;

				for (; i < array.length; i++) {
					c = array[i];
					if (c == '(') pCount++;
					if (c == ')') pCount--;
					if (pCount == -1) break;
					functionBlock.append(c);
				}

				if (pCount != -1) {
					throw new IllegalStateException("Unbalanced parenthesis at line " + i);
				}

				String block = functionBlock.toString();
				if (block.isEmpty()) {
					runtime.replaceWithList(cur -> cast(cur, LangCallable.class).call());
				} else {
					UnaryOperator<Runtime> function = asFunction(compile(block));
					runtime.replaceWithList((r, cur) -> cast(cur, LangCallable.class).call(function.apply(r.copy()).done()));
				}
			} else if (c == '{') {
				int pCount = 0;
				StringBuilder functionBlock = new StringBuilder();
				i++;

				for (; i < array.length; i++) {
					c = array[i];
					if (c == '{') pCount++;
					if (c == '}') pCount--;
					if (pCount == -1) break;
					functionBlock.append(c);
				}

				if (pCount != -1) {
					throw new IllegalStateException("Unbalanced brackets at line " + i);
				}

				String block = functionBlock.toString();
				if (block.isEmpty()) {
					runtime.replace((r, cur) -> closure(asFunction(r1 -> r1.replace(null)), r.thisObj()));
				} else {
					UnaryOperator<Runtime> function = asFunction(compile(block));
					runtime.replace((r, cur) -> closure(function, r.thisObj()));
				}
			} else if (c == '"') {
				boolean invalid = true, escaping = false;
				StringBuilder s = new StringBuilder();

				i++;
				for (; i < array.length; i++) {
					c = array[i];

					if (escaping) {
						escaping = false;
						s.append(escape(c));
						continue;
					}

					if (c == '"') {
						invalid = false;
						break;
					}

					if (c == '\\') {
						escaping = true;
						continue;
					}

					s.append(c);
				}

				if (invalid) {
					throw new IllegalStateException("Unclosed brackets at line " + i);
				}

				LangString string = LangString.of(s.toString());
				runtime.modify(r -> r.replace(string));
			} else if (Character.isDigit(c)) {
				StringBuilder num = new StringBuilder();

				for (; i < array.length; i++) {
					c = array[i];

					if (c == '\r' || c == '\n') {
						line++;
						break;
					}
					if (c == ' ' || c == '\t') break;
					num.append(c);
				}
			} else {

				String value;
				StringBuilder currentBlock = new StringBuilder();

				for (; i < array.length; i++) {
					c = array[i];
					if (c == '\r' || c == '\n') {
						line++;
						break;
					}
					if (c == ' ' || c == '\t') break;

					if (c == ',' || c == '.' || c == '\\' || c == ')' || c == '}' || c == '[' || c == ']') {
						throw new IllegalStateException("Invalid character '" + c + "' at line " + i);
					}
					if (c != '.' || c != ',' || c != '(') currentBlock.append(c);
					else break;
				}

				value = currentBlock.toString();
			}
			//TODO LOOK AFTER. IF IS "," DO NEXT
		}

		return runtime.getOperation();
	}

	private static char escape(char c) {
		switch (c) {
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\t';
			default:
				return c;
		}
	}

	public static void main(String[] args) {
		System.out.println(asFunction(compile("\"hi\"")).apply(new Runtime((LangCallable) args1 -> args1)).done());
	}
}
