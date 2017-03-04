package net.kodehawa.lib.mantarolang;

import net.kodehawa.lib.mantarolang.internal.Runtime;
import net.kodehawa.lib.mantarolang.internal.RuntimeOperator;
import net.kodehawa.lib.mantarolang.objects.*;
import net.kodehawa.lib.mantarolang.objects.operations.*;

import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class MantaroLangCompiler {
	private static class LangClosure implements LangCallable/*, LangOpMultiply*/ {
		private final UnaryOperator<Runtime> compiled;
		private final LangObject thisObj;

		public LangClosure(UnaryOperator<Runtime> compiled, LangObject thisObj) {
			this.compiled = compiled;
			this.thisObj = thisObj;
		}

		@Override
		public List<LangObject> call(List<LangObject> args) {
			return compiled.apply(new Runtime(_get(args, 0, thisObj))).done();
		}

//		@Override
//		public LangObject multiply(LangObject object) {
//			return new LangClosure(
//				IntStream.range(0, Math.min(200, Math.max(0, _cast(object, LangInteger.class).get().intValue())))
//					.mapToObj(i -> compiled).reduce(UnaryOperator.identity(), (compiled1, compiled2) -> new UnaryOperator<Runtime>() {
//					@Override
//					public Runtime apply(Runtime r) {
//						return compiled2.apply(compiled1.apply(r));
//					}
//
//					@Override
//					public String toString() {
//						return "UnaryOperator{" + compiled1 + '&' + compiled2 + '}';
//					}
//				})
//				, thisObj
//			);
//		}

		@Override
		public String toString() {
			return "LClosure{" + compiled + '@' + thisObj + '}';
		}
	}

	private static <T> UnaryOperator<T> asFunction(Consumer<T> consumer) {
		return new UnaryOperator<T>() {
			@Override
			public T apply(T t) {
				consumer.accept(t);
				return t;
			}

			@Override
			public String toString() {
				return "UnaryOperator{" + consumer + '}';
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <T extends LangObject> T cast(LangObject object, Class<T> c) {
		if (!c.isInstance(object)) throw new LangRuntimeException("Can't cast " + object + " to " + c);
		return ((T) object);
	}

	private static LangCallable closure(UnaryOperator<Runtime> compiled, LangObject thisObj) {
		return new LangClosure(compiled, thisObj);
	}

	public static Consumer<Runtime> compile(String code) {
		Objects.requireNonNull(code, "code");

		RuntimeOperator runtime = new RuntimeOperator();

		char[] array = code.toCharArray();

		int line = 0;

		BinaryOperator<LangObject> queuedOperation = null;
		char queuedOpChar = 0;
		boolean onThis = true;
		for (int i = 0; i < array.length; i++) {
			char c = array[i];

			//region HANDLER SPACES , ;
			switch (c) {
				case '\r':
				case '\n':
					line++;
				case ' ':
				case '\t':
					continue;
				case ',': {
					if (queuedOperation != null) {
						if (onThis)
							throw new LangCompileException("Queued Operation '" + queuedOpChar + "' unsatified at line " + line);
						BinaryOperator<LangObject> op = queuedOperation;
						runtime.modify(r -> r.applyOperation(op));
						queuedOperation = null;
						i--;
						continue;
					}

					if (onThis) throw new LangCompileException("Invalid character '" + c + "' at line " + line);

					runtime.modify(Runtime::next);
					onThis = true;
					continue;
				}
				case ';':
					if (queuedOperation != null) {
						if (onThis)
							throw new LangCompileException("Queued Operation '" + queuedOpChar + "' unsatified at line " + line);
						BinaryOperator<LangObject> op = queuedOperation;
						runtime.modify(r -> r.applyOperation(op));
						queuedOperation = null;
						i--;
						continue;
					}

					runtime.modify(Runtime::discard);
					onThis = true;
					continue;
				default:
					break;
			}
			//endregion

			/*
			The operations {...} "..." 0.0 can only happen on "this".
			The operations . can only happen out of "this".
			The operation (...) have a special property on "this"
			 */
			if (onThis) {
				//region OPERATIONS (...) {...} [...] "..." 0.0
				if (c == '(') {
					//region OPERATION (...)
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

					if (pCount != -1) throw new LangCompileException("Unbalanced parenthesis at line " + line);

					String block = functionBlock.toString().trim();
					if (block.isEmpty()) {
						runtime.replace(cur -> null);
					} else {
						UnaryOperator<Runtime> function = asFunction(compile(block));
						runtime.replaceWithList((r, cur) -> function.apply(r.copy()).done());
					}

					onThis = false;
					continue;
					//endregion
				} else if (c == '{') {
					//region OPERATION {...}
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

					if (pCount != -1) throw new LangCompileException("Unbalanced brackets at line " + line);

					String block = functionBlock.toString().trim();

					if (block.isEmpty()) {
						runtime.replace((r, cur) -> closure(asFunction(r1 -> r1.replace(null)), r.thisObj()));
					} else {
						UnaryOperator<Runtime> function = asFunction(compile(block));
						runtime.replace((r, cur) -> closure(function, r.thisObj()));
					}

					onThis = false;
					continue;
					//endregion
				} else if (c == '[') {
					//region OPERATION [...]
					int pCount = 0;
					StringBuilder functionBlock = new StringBuilder();
					i++;

					for (; i < array.length; i++) {
						c = array[i];
						if (c == '[') pCount++;
						if (c == ']') pCount--;
						if (pCount == -1) break;
						functionBlock.append(c);
					}

					if (pCount != -1) throw new LangCompileException("Unbalanced squared brackets at line " + line);

					String block = functionBlock.toString().trim();

					if (block.isEmpty()) {
						runtime.replace(cur -> new LangList());
					} else {
						UnaryOperator<Runtime> function = asFunction(compile(block));
						runtime.replace((r, cur) -> new LangList(function.apply(r.copy()).doneWithoutThis()));
					}

					onThis = false;
					continue;
					//endregion
				} else if (c == '"' || c == '\'') {
					//region OPERATION "..."
					boolean invalid = true, escaping = false;
					StringBuilder s = new StringBuilder();

					char closeChar = c;

					i++;
					for (; i < array.length; i++) {
						c = array[i];

						if (escaping) {
							escaping = false;
							s.append(escape(c));
							continue;
						}

						if (c == '\r' || c == '\n') {
							line++;
						}

						if (c == closeChar) {
							invalid = false;
							break;
						}

						if (c == '\\') {
							escaping = true;
							continue;
						}

						s.append(c);
					}

					if (invalid) throw new LangCompileException("Unclosed brackets at line " + line);

					LangString string = new LangString(s.toString());
					runtime.modify(r -> r.replace(string));

					onThis = false;
					continue;
					//endregion
				} else if (Character.isDigit(c) || c == '-') {
					//region OPERATION NUMBERS
					StringBuilder num = new StringBuilder();
					/*
					0 = int
					1 = right after dot
					2 = float
					 */
					byte state = 0;

					loop:
					for (; i < array.length; i++) {
						c = array[i];

						switch (c) {
							case '\r':
							case '\n':
								line++;
							case ' ':
							case '\t':
							case ';':
								onThis = true;
								break loop;
							case ',':
								onThis = true;
								i--;
								break loop;
						}

						switch (state) {
							case 0: {
								if (c == '+' || c == '-' || c == '*' || c == '/' || c == '|' || c == '&' || c == '<' || c == '>' || c == '^') {
									i--;
									break loop;
								}
								if (c == '.') {
									state = 1;
									num.append(c);
									break;
								}

								if (!Character.isDigit(c))
									throw new LangCompileException("Invalid character at line " + line + ": expected digit or dot, got '" + c + "'");

								num.append(c);

								break;
							}
							case 1: {
								if (Character.isAlphabetic(c) || c == '_') {
									num.deleteCharAt(num.length() - 1);
									state = 0;
									i--;
									i--;
									break loop;
								}

								if (c == '.')
									throw new LangCompileException("Invalid character at line " + line + ": expected digit or identifier, got '" + c + "'");
								state = 2;
							}
							case 2: {
								if (c == '+' || c == '-' || c == '*' || c == '/' || c == '|' || c == '&' || c == '<' || c == '>' || c == '^') {
									i--;
									break loop;
								}
								if (c == '.') {
									break loop;
								}

								if (!Character.isDigit(c))
									throw new LangCompileException("Invalid character at line " + line + ": expected digit or dot , got '" + c + "'");
								num.append(c);

								break;
							}
							default:
								throw new LangCompileException("Invalid compilation state at line " + line + ": " + state);
						}
					}

					switch (state) {
						case 0: {
							LangInteger number = new LangInteger(Long.parseLong(num.toString()));
							runtime.modify(r -> r.replace(number));
							break;
						}
						case 2: {
							LangFloat number = new LangFloat(Double.parseDouble(num.toString()));
							runtime.modify(r -> r.replace(number));
							break;
						}
						default:
							throw new LangCompileException("Invalid compilation state at line " + line + ": " + state);
					}

					onThis = false;
					continue;
					//endregion
				}
				//endregion
				//region INVALID OPERATIONS .
				else if (c == '.') {
					throw new LangCompileException("Invalid character '" + c + "' at line " + line);
				}
				//endregion
			} else {
				//region OPERATIONS (...) . + - * / | & < > ^
				if (c == '(') {
					//region OPERATION (...)
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

					if (pCount != -1) throw new IllegalStateException("Unbalanced parenthesis at line " + line);

					String block = functionBlock.toString().trim();
					if (block.isEmpty()) {
						runtime.replaceWithList(cur -> cast(cur, LangCallable.class).call());
					} else {
						UnaryOperator<Runtime> function = asFunction(compile(block));
						runtime.replaceWithList((r, cur) -> cast(cur, LangCallable.class).call(function.apply(r.copy()).done()));
					}
					continue;
					//endregion
				} else if (c == '.') {
					//region OPERATION .
					i++;
					loop:
					for (; i < array.length; i++) {
						switch (c = array[i]) {
							case '\r':
							case '\n':
								line++;
							case ' ':
							case '\t':
								continue;
							default:
								break loop;
						}
					}
					//endregion
				} else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '|' || c == '&' || c == '<' || c == '>' || c == '^') {
					//region OPERATORS + - * / | & < > ^
					if (queuedOperation != null) {
						BinaryOperator<LangObject> op = queuedOperation;
						runtime.modify(r -> r.applyOperation(op));
						queuedOperation = null;
					}

					queuedOpChar = c;
					switch (c) {
						case '+':
							queuedOperation = (q, cur) -> cast(q, LangOpAdd.class).add(cur);
							break;
						case '-':
							queuedOperation = (q, cur) -> cast(q, LangOpSubtract.class).subtract(cur);
							break;
						case '*':
							queuedOperation = (q, cur) -> cast(q, LangOpMultiply.class).multiply(cur);
							break;
						case '/':
							queuedOperation = (q, cur) -> cast(q, LangOpDivide.class).divide(cur);
							break;
						case '|':
							queuedOperation = (q, cur) -> cast(q, LangOpOr.class).or(cur);
							break;
						case '&':
							queuedOperation = (q, cur) -> cast(q, LangOpAnd.class).and(cur);
							break;
						case '<':
							queuedOperation = (q, cur) -> cast(q, LangOpLeftShift.class).leftShift(cur);
							break;
						case '>':
							queuedOperation = (q, cur) -> cast(q, LangOpRightShift.class).rightShift(cur);
							break;
						case '^':
							queuedOperation = (q, cur) -> cast(q, LangOpXor.class).xor(cur);
							break;
					}

					runtime.modify(Runtime::queue);
					onThis = true;
					continue;
					//endregion
				}

				//endregion
				//region INVALID OPERATIONS
				else throw new LangCompileException("Invalid character '" + c + "' at line " + line);
				//endregion
			}

			if (Character.isJavaIdentifierStart(c)) {
				//region OPERATION GET
				StringBuilder name = new StringBuilder();

				loop:
				for (; i < array.length; i++) {
					switch (c = array[i]) {
						case '\r':
						case '\n':
							line++;
						case ' ':
						case '\t':
							break loop;
						case '+':
						case '-':
						case '*':
						case '/':
						case '|':
						case '&':
						case '<':
						case '>':
						case '^':
						case ';':
						case '.':
						case ',':
						case '(':
							i--;
							break loop;
						default: {
							if (Character.isJavaIdentifierPart(c)) {
								name.append(c);
							} else throw new LangCompileException("Invalid character '" + c + "' at line " + line);
							break;
						}
					}
				}

				String block = name.toString();

				if (block.isEmpty()) throw new LangCompileException("Invalid empty block at line " + line);

				if (onThis) {
					if (block.equals("this")) {
						onThis = false;
						continue;
					} else if (block.equals("true")) {
						runtime.replace(r -> LangBoolean.TRUE);
						onThis = false;
						continue;
					} else if (block.equals("false")) {
						runtime.replace(r -> LangBoolean.FALSE);
						onThis = false;
						continue;
					}
				}

				runtime.replace((r, cur) -> cast(cur, LangContainer.class).get(block));
				//endregion
			} else throw new LangCompileException("Invalid character '" + c + "' at line " + line);
		}

		if (queuedOperation != null) {
			BinaryOperator<LangObject> op = queuedOperation;
			runtime.modify(r -> r.applyOperation(op));
			queuedOperation = null;
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
		try {
			System.out.print("Compiling...");
			long millis = -System.currentTimeMillis();
			Consumer<Runtime> compiled = compile("{this+this}*2");
			millis += System.currentTimeMillis();
			System.out.println(" took " + millis + " ms");
			System.out.print("Running...");
			millis = -System.currentTimeMillis();
			List<LangObject> result = asFunction(compiled).apply(new Runtime(null)).done();
			millis += System.currentTimeMillis();
			System.out.println(" took " + millis + " ms");

			System.out.println("\n\nResult:" + result);
		} catch (Exception e) {
			System.out.print("Exception!");
			System.out.flush();
			e.printStackTrace();
		}
	}
}
