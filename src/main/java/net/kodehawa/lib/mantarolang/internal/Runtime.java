package net.kodehawa.lib.mantarolang.internal;

import net.kodehawa.lib.mantarolang.objects.LangObject;

import java.util.ArrayList;
import java.util.List;

public class Runtime {
	private final LangObject thisObj;
	private LangObject current;
	private List<LangObject> done = new ArrayList<>();

	public Runtime(LangObject thisObj, LangObject current) {
		this.thisObj = thisObj;
		this.current = current;
	}

	public Runtime(LangObject thisObj) {
		this(thisObj, thisObj);
	}

	public Runtime copy() {
		return new Runtime(thisObj);
	}

	public LangObject current() {
		return current;
	}

	public List<LangObject> done() {
		next();
		return done;
	}

	public List<LangObject> doneWithoutThis() {
		if (current != thisObj) next();
		return done;
	}

	public void next() {
		next(thisObj);
	}

	public void next(LangObject next) {
		done.add(current);
		current = next;
	}

	public LangObject replace(LangObject current) {
		LangObject before = this.current;
		this.current = current;
		return before;
	}

	public LangObject thisObj() {
		return thisObj;
	}
}
