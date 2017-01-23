package com.marcomaldonado.web.callback;

import com.marcomaldonado.konachan.entities.Tag;
import com.marcomaldonado.konachan.entities.Wallpaper;

/**
 * Created by Mxrck on 05/12/15.
 */
public interface WallpaperCallback extends Callback {

	void onSuccess(Wallpaper[] wallpapers, Tag[] tags);

}
