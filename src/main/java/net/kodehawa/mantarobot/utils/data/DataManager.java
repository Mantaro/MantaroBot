package net.kodehawa.mantarobot.utils.data;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

public interface DataManager<T> extends Supplier<T>, Closeable {
    void save();

    @Override
    default void close() throws IOException {
        save();
    }
}
