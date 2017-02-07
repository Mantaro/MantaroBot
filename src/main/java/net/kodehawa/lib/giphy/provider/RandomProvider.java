package net.kodehawa.lib.giphy.provider;

import net.kodehawa.lib.giphy.main.entities.Random;

@FunctionalInterface
public interface RandomProvider {
	void onSuccess(Random results);
}
