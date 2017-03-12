package net.kodehawa.mantarobot.utils.data;

import java.io.Closeable;
import java.util.function.Supplier;

public interface DataManager<T> extends Supplier<T>, Closeable {
	void save();
	default void close() {
	    save();
    }
}
