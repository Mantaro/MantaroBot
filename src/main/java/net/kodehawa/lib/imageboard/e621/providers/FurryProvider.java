package net.kodehawa.lib.imageboard.e621.providers;

import net.kodehawa.lib.imageboard.e621.main.entities.Furry;

import java.util.List;

@FunctionalInterface
public interface FurryProvider {

	void onSuccess(List<Furry> results);

}
