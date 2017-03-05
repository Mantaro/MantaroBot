package net.kodehawa.lib.imageboard.konachan.providers;

import net.kodehawa.lib.imageboard.konachan.main.entities.Tag;
import net.kodehawa.lib.imageboard.konachan.main.entities.Wallpaper;

import java.util.List;

@FunctionalInterface
public interface WallpaperProvider {
	void onSuccess(List<Wallpaper> wallpapers, Tag[] tags);
}