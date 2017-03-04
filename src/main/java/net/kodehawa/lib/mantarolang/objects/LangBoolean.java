package net.kodehawa.lib.mantarolang.objects;

public class LangBoolean implements LangWrapped<Boolean>, LangContainer {
	public static final LangBoolean FALSE = new LangBoolean(false);
	public static final LangBoolean TRUE = new LangBoolean(true);
	private final boolean bool;

	private LangBoolean(boolean bool) {
		this.bool = bool;
	}

	@Override
	public boolean asTruth() {
		return bool;
	}

	@Override
	public Boolean get() {
		return bool;
	}

	@Override
	public LangObject get(String name) {
		if (name.equals("not")) {
			return bool ? FALSE : TRUE;
		}

		return invalidProperty(name);
	}

	@Override
	public String toString() {
		return "LangBoolean{" + bool + '}';
	}

}
