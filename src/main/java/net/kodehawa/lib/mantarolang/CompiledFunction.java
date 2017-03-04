package net.kodehawa.lib.mantarolang;

public interface CompiledFunction<T> {
	T run();

	long timeTook();
}
