package net.kodehawa.lib.konachan.providers;

import net.kodehawa.lib.konachan.main.entities.Tag;
import net.kodehawa.lib.konachan.main.entities.Wallpaper;

import java.util.List;

@FunctionalInterface
public interface WallpaperProvider {
	void onSuccess(List<Wallpaper> wallpapers, Tag[] tags);
}