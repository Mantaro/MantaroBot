package net.kodehawa.lib.mantarolang.objects;

import net.kodehawa.lib.mantarolang.objects.operations.LangOpAdd;
import net.kodehawa.lib.mantarolang.objects.operations.LangOpLeftShift;
import net.kodehawa.lib.mantarolang.objects.operations.LangOpMultiply;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LangString implements LangWrapped<String>, LangOpAdd, LangOpMultiply, LangOpLeftShift {
	private final String s;

	public LangString(String s) {
		this.s = s;
	}

	@Override
	public LangObject add(LangObject object) {
		return new LangString(this.asString() + object.asString());
	}

	@Override
	public String get() {
		return s;
	}

	@Override
	public LangObject leftShift(LangObject object) {
		return add(object);
	}

	@Override
	public LangObject multiply(LangObject object) {
		return new LangString(IntStream.range(0, Math.min(200, Math.max(0, _cast(object, LangInteger.class).get().intValue()))).mapToObj(i -> s).collect(Collectors.joining()));
	}

	@Override
	public String toString() {
		return "LString{" + '\'' + s + '\'' + '}';
	}
}
