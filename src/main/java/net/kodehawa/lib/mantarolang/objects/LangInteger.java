package net.kodehawa.lib.mantarolang.objects;

import net.kodehawa.lib.mantarolang.objects.operations.*;

public class LangInteger implements LangWrapped<Long>, LangContainer, LangOpAdd, LangOpSubtract, LangOpDivide, LangOpMultiply, LangOpAnd, LangOpOr, LangOpXor, LangOpLeftShift, LangOpRightShift {
	private final long number;

	public LangInteger(long number) {
		this.number = number;
	}

	@Override
	public LangObject add(LangObject object) {
		return new LangInteger(number + _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject and(LangObject object) {
		return new LangInteger(number & _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject divide(LangObject object) {
		return new LangInteger(number / _cast(object, LangInteger.class).get());
	}

	@Override
	public Long get() {
		return number;
	}

	@Override
	public LangObject get(String name) {
		if (name.equals("float")) {
			return new LangFloat(get());
		}
		return invalidProperty(name);
	}

	@Override
	public LangObject leftShift(LangObject object) {
		return new LangInteger(number << _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject multiply(LangObject object) {
		return new LangInteger(number * _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject or(LangObject object) {
		return new LangInteger(number | _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject rightShift(LangObject object) {
		return new LangInteger(number >> _cast(object, LangInteger.class).get());
	}

	@Override
	public LangObject subtract(LangObject object) {
		return new LangInteger(number - _cast(object, LangInteger.class).get());
	}

	@Override
	public String toString() {
		return "LInteger{" + number + '}';
	}

	@Override
	public LangObject xor(LangObject object) {
		return new LangInteger(number ^ _cast(object, LangInteger.class).get());
	}
}
