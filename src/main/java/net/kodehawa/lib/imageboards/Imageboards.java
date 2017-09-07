/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.lib.imageboards;

import net.kodehawa.lib.imageboards.entities.Furry;
import net.kodehawa.lib.imageboards.entities.Hentai;
import net.kodehawa.lib.imageboards.entities.Wallpaper;
import net.kodehawa.lib.imageboards.entities.YandereImage;

public class Imageboards {
    public static final ImageboardAPI<Furry> E621 = new ImageboardAPI<>(ImageboardAPI.Boards.E621, ImageboardAPI.Type.JSON, Furry[].class);
    public static final ImageboardAPI<Wallpaper> KONACHAN = new ImageboardAPI<>(ImageboardAPI.Boards.KONACHAN, ImageboardAPI.Type.JSON, Wallpaper[].class);
    public static final ImageboardAPI<Hentai> RULE34 = new ImageboardAPI<>(ImageboardAPI.Boards.R34, ImageboardAPI.Type.XML, Hentai[].class);
    public static final ImageboardAPI<YandereImage> YANDERE = new ImageboardAPI<>(ImageboardAPI.Boards.YANDERE, ImageboardAPI.Type.JSON, YandereImage[].class);
}
