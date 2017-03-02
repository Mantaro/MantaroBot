package net.kodehawa.mantarolang;

import java.util.HashMap;
import java.util.Map;

public class MantaroLang implements LangContainer {


//	public static void main(String[] args) {
//		System.out.println(new MantaroLang().eval("("));
//	}

	public final Map<String, LangObject> ENV = new HashMap<>();

	public MantaroLang() {
		ENV.put("this", this);
	}

	@Override
	public LangObject get(String object) {
		return ENV.get(object);
	}

//	public Function<LangObject> compile(String code) {
//		if (code == null) return null;
//
//
//
//		//StringBuilder currentBlock = new StringBuilder();
//
//
//		return env;
//	}
}
