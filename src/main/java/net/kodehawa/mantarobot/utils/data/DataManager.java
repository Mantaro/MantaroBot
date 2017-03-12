package net.kodehawa.mantarobot.utils.data;

import java.util.function.Supplier;

public interface DataManager<T> extends Supplier<T> {
	void save();
}
