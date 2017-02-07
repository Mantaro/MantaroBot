package net.kodehawa.lib.giphy.provider;

import net.kodehawa.lib.giphy.main.entities.Gif;

@FunctionalInterface
public interface Provider {
	void onSuccess(Gif results);
}
