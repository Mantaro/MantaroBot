package net.kodehawa.lib.mantarolang;

public interface CompiledFunction<T> {
	long timeTook();
	T run();
}
