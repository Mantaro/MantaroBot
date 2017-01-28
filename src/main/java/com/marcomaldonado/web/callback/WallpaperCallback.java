package com.marcomaldonado.web.callback;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;

public interface WallpaperCallback extends Callback {

	void onSuccess(Wallpaper[] wallpapers, Tag[] tags);

}
