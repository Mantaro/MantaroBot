package net.kodehawa.lib.imageboards.rule34.providers;

import net.kodehawa.lib.imageboards.rule34.entities.Hentai;

import java.util.List;

@FunctionalInterface
public interface HentaiProvider {

    void onSuccess(List<Hentai> results);

}
