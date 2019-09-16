/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
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
 *
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

@Getter
//Kitsu API anime data.
public class AnimeData {
    private String id;
    private String type;
    private Attributes attributes;

    @Getter
    public static class Attributes {
        private Names titles;
        //The normal title.
        private String canonicalTitle;

        //Usually has some weird characters?
        private String synopsis;
        private Image image;

        //Popularity stuff. How many people have favorited it.
        private int favoritesCount;

        //yyyy-mm-dd, seems ISO.
        private String startDate;
        private String endDate;

        //Popularity stuff
        private int ratingRank;
        private int popularityRank;

        //TV or Movie/OVA
        private String showType;

        //Ongoing or Finished
        private String status;

        //Image to show the user
        private PosterImage posterImage;

        private int episodeCount;
        //In minutes.
        private int episodeLength;

        private boolean nsfw;
    }

    @JsonIgnore
    public String getURL() {
        return "https://kitsu.io/anime/" + id;
    }

    @Getter
    public static class PosterImage {
        private String medium;
    }

    @Getter
    public static class Names {
        private String en;
        private String ja_jp;
    }

    @Getter
    public static class Image {
        private String original;
    }
}
