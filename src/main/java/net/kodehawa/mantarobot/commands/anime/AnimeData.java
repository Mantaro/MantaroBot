/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.anime;

import com.fasterxml.jackson.annotation.JsonIgnore;

//Kitsu API anime data.
public class AnimeData {
    @SuppressWarnings("unused")
    private String id;
    @SuppressWarnings("unused")
    private String type;
    @SuppressWarnings("unused")
    private Attributes attributes;

    @SuppressWarnings("unused")
    public String getId() {
        return this.id;
    }

    @SuppressWarnings("unused")
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
        @SuppressWarnings("unused")
        private Names titles;
        //The normal title.
        @SuppressWarnings("unused")
        private String canonicalTitle;

        //Usually has some weird characters?
        @SuppressWarnings("unused")
        private String synopsis;
        @SuppressWarnings("unused")
        private Image image;

        //Popularity stuff. How many people have favorited it.
        @SuppressWarnings("unused")
        private int favoritesCount;

        //yyyy-mm-dd, seems ISO.
        @SuppressWarnings("unused")
        private String startDate;
        @SuppressWarnings("unused")
        private String endDate;

        //Popularity stuff
        @SuppressWarnings("unused")
        private int ratingRank;
        @SuppressWarnings("unused")
        private int popularityRank;

        //TV or Movie/OVA
        @SuppressWarnings("unused")
        private String showType;

        //Ongoing or Finished
        @SuppressWarnings("unused")
        private String status;

        //Image to show the user
        @SuppressWarnings("unused")
        private PosterImage posterImage;

        @SuppressWarnings("unused")
        private int episodeCount;
        //In minutes.
        @SuppressWarnings("unused")
        private int episodeLength;

        @SuppressWarnings("unused")
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

        @SuppressWarnings("unused")
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

        @SuppressWarnings("unused")
        public int getRatingRank() {
            return this.ratingRank;
        }

        @SuppressWarnings("unused")
        public int getPopularityRank() {
            return this.popularityRank;
        }

        public String getShowType() {
            return this.showType;
        }

        @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static class PosterImage {
        @SuppressWarnings("unused")
        private String medium;

        @SuppressWarnings("unused")
        public String getMedium() {
            return this.medium;
        }
    }

    @SuppressWarnings("unused")
    public static class Names {
        @SuppressWarnings("unused")
        private String en;
        @SuppressWarnings("unused")
        private String ja_jp;

        @SuppressWarnings("unused")
        public String getEn() {
            return this.en;
        }

        @SuppressWarnings("unused")
        public String getJa_jp() {
            return this.ja_jp;
        }
    }

    @SuppressWarnings("unused")
    public static class Image {
        @SuppressWarnings("unused")
        private String original;

        @SuppressWarnings("unused")
        public String getOriginal() {
            return this.original;
        }
    }
}
