package net.kodehawa.lib.imageboard.konachan.providers;

@FunctionalInterface
public interface DownloadProvider {
	void onSuccess(String route);
}
