package net.kodehawa.lib.mantarolang.internal;

import java.util.function.Consumer;

public interface RuntimeOperation extends Consumer<Runtime> {
	void execute(Runtime r);

	@Override
	default void accept(Runtime runtime) {
		execute(runtime);
	}

	int opCount();
}
