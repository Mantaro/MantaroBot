package net.kodehawa.mantarolang.objects;

public class LangInteger implements LangWrapped<Long>, LangContainer {
	private final long number;

	public LangInteger(long number) {
		this.number = number;
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
	public String toString() {
		return "LInteger{" + number + '}';
	}
}
