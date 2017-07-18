package net.kodehawa.lib.imageboards.konachan.providers;

@FunctionalInterface
public interface DownloadProvider {
    void onSuccess(String route);
}
