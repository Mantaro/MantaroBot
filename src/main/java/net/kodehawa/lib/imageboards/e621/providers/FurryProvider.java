package net.kodehawa.lib.imageboards.e621.providers;

import net.kodehawa.lib.imageboards.e621.main.entities.Furry;

import java.util.List;

@FunctionalInterface
public interface FurryProvider {
    void onSuccess(List<Furry> results);
}
