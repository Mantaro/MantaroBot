package net.kodehawa.lib.konachan.providers;

@FunctionalInterface
public interface DownloadProvider {
	void onSuccess(String route);
}
