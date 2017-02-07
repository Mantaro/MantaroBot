package net.kodehawa.lib.giphy.provider;

import net.kodehawa.lib.giphy.main.entities.Search;

@FunctionalInterface
public interface SearchProvider {
	void onSuccess(Search results);
}
