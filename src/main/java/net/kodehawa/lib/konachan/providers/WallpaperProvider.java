package net.kodehawa.lib.konachan.providers;

import net.kodehawa.lib.konachan.konachan.entities.Tag;
import net.kodehawa.lib.konachan.konachan.entities.Wallpaper;

import java.util.List;

public interface WallpaperProvider {

	void onSuccess(List<Wallpaper> wallpapers, Tag[] tags);

}
