package net.kodehawa.lib.giphy.provider;

import net.kodehawa.lib.giphy.main.entities.RandomGif;

@FunctionalInterface
public interface RandomProvider {
	void onSuccess(RandomGif results);
}
