/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;

//Kitsu API anime data.
public class AnimeData {
    private String id;
    private String type;
    private Attributes attributes;

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public Attributes getAttributes() {
        return this.attributes;
    }

    @JsonIgnore
    public String getURL() {
        return "https://kitsu.io/anime/" + id;
    }

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

        public Names getTitles() {
            return this.titles;
        }

        public String getCanonicalTitle() {
            return this.canonicalTitle;
        }

        public String getSynopsis() {
            return this.synopsis;
        }

        public Image getImage() {
            return this.image;
        }

        public int getFavoritesCount() {
            return this.favoritesCount;
        }

        public String getStartDate() {
            return this.startDate;
        }

        public String getEndDate() {
            return this.endDate;
        }

        public int getRatingRank() {
            return this.ratingRank;
        }

        public int getPopularityRank() {
            return this.popularityRank;
        }

        public String getShowType() {
            return this.showType;
        }

        public String getStatus() {
            return this.status;
        }

        public PosterImage getPosterImage() {
            return this.posterImage;
        }

        public int getEpisodeCount() {
            return this.episodeCount;
        }

        public int getEpisodeLength() {
            return this.episodeLength;
        }

        public boolean isNsfw() {
            return this.nsfw;
        }
    }

    public static class PosterImage {
        private String medium;

        public String getMedium() {
            return this.medium;
        }
    }

    public static class Names {
        private String en;
        private String ja_jp;

        public String getEn() {
            return this.en;
        }

        public String getJa_jp() {
            return this.ja_jp;
        }
    }

    public static class Image {
        private String original;

        public String getOriginal() {
            return this.original;
        }
    }
}
