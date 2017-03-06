package net.kodehawa.lib.mantarolang.objects;

import net.kodehawa.lib.mantarolang.objects.operations.LangOpAdd;
import net.kodehawa.lib.mantarolang.objects.operations.LangOpDivide;
import net.kodehawa.lib.mantarolang.objects.operations.LangOpMultiply;
import net.kodehawa.lib.mantarolang.objects.operations.LangOpSubtract;

public class LangFloat implements LangContainer, LangOpAdd, LangOpSubtract, LangOpDivide, LangOpMultiply {
	private final double number;

	public LangFloat(double number) {
		this.number = number;
	}

	@Override
	public LangObject add(LangObject object) {
		return new LangFloat(number + _cast(object, LangFloat.class).get());
	}

	@Override
	public LangObject divide(LangObject object) {
		return new LangFloat(number / _cast(object, LangFloat.class).get());
	}

	public double get() {
		return number;
	}

	@Override
	public LangObject get(String name) {
		if (name.equals("int")) {
			return new LangInteger((long) get());
		}
		return invalidProperty(name);
	}

	@Override
	public LangObject multiply(LangObject object) {
		return new LangFloat(number * _cast(object, LangFloat.class).get());
	}

	@Override
	public LangObject subtract(LangObject object) {
		return new LangFloat(number - _cast(object, LangFloat.class).get());
	}

	@Override
	public String toString() {
		return "LFloat{" + number + '}';
	}
}
