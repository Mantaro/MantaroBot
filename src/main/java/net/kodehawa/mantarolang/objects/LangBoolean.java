package net.kodehawa.mantarolang.objects;

public class LangBoolean implements LangWrapped<Boolean>, LangContainer {
	public static final LangBoolean TRUE = new LangBoolean(true);
	public static final LangBoolean FALSE = new LangBoolean(false);
	private final boolean bool;

	private LangBoolean(boolean bool) {
		this.bool = bool;
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
}
