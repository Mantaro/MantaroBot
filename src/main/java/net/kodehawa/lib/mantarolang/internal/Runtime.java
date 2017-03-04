package net.kodehawa.lib.mantarolang.internal;

import net.kodehawa.lib.mantarolang.objects.LangObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;

public class Runtime {
	private final LangObject thisObj;
	private LangObject current;
	private List<LangObject> done = new ArrayList<>();
	private LangObject queue;

	public Runtime(LangObject thisObj, LangObject current) {
		this.thisObj = thisObj;
		this.current = current;
	}

	public Runtime(LangObject thisObj) {
		this(thisObj, thisObj);
	}

	public void applyOperation(BinaryOperator<LangObject> operator) {
		current = operator.apply(queue, current);
		queue = null;
	}

	public Runtime copy() {
		return new Runtime(thisObj);
	}

	public LangObject current() {
		return current;
	}

	public LangObject discard() {
		LangObject before = current;
		current = thisObj;
		return before;
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

	public void queue() {
		queue = current;
		current = thisObj;
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
